# BUG-001 障害レポート

## 基本情報

| 項目 | 内容 |
|---|---|
| 障害ID | BUG-001 |
| 発生日時 | 2026/3/10 9:12 |
| 報告日 | 2026/3/31 |
| 影響範囲 | 注文確定機能（会員ユーザー） |
| 重大度 | 高（注文不可） |

---

## 事象概要

会員ユーザーがカートに商品を追加し、注文確定ボタンを押下するとシステムエラー画面に遷移する。
ローカル開発環境の初期構築直後にのみ発生し、別の開発者が構築した環境では再現しない。

---

## 推定原因

### 結論

**シードデータが生成する注文番号と、アプリが実行時に採番する注文番号が衝突し、`orders.order_number` のUNIQUE制約違反が発生している可能性が高い。**

### 根拠となるコード・データの分析

#### (1) シードデータが注文番号に「実行当日の日付」を使用している

`sql/seed/test-data/orders.sql` では、100件のテスト注文を以下の形式で生成する。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

`CURRENT_DATE` はSQL実行時（＝初期構築時）の日付であるため、**初期構築を 2026/3/10 に実施した場合、`ORD20260310-000001` ～ `ORD20260310-000100` の 100 件が `orders` テーブルに登録される。**

#### (2) `order_number_counters` テーブルはシードで更新されない

`order_number_counters` テーブルはアプリが注文番号の連番管理に使うテーブルだが、**シードデータはこのテーブルを一切更新しない。**

`sql/seed/masters.sql`、`sql/seed/test-data/orders.sql` のいずれにも `order_number_counters` への `INSERT` は存在しない。

#### (3) 注文確定時の採番ロジック

`OrderService.generateOrderNumber()` → `OrderRepository.nextOrderSequence()` は以下の SQL を実行する。

```sql
-- OrderMapper.xml: nextOrderSequence
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence;
```

`order_number_counters` に当日分のレコードがない場合、`last_sequence = 1` で INSERT し、`1` を返す。
その結果、生成される注文番号は `ORD20260310-000001` となる。

#### (4) UNIQUE制約違反でシステムエラー発生

`orders.order_number` には `UNIQUE` 制約が設定されている。

```sql
-- sql/schema/orders.sql
order_number VARCHAR(20) NOT NULL UNIQUE,
```

既にシードデータで `ORD20260310-000001` が登録済みのため、同じ注文番号での `INSERT INTO orders` が **UNIQUE制約違反** となり、トランザクションがロールバックされて500エラーが発生する。

### 再現条件の整合性確認

| 障害票の条件 | 推定原因との整合 |
|---|---|
| 初期構築直後に発生 | シードの注文番号が当日日付で生成されるため、構築当日のみ衝突する |
| 先に構築した人では再現しない | 先に構築した人の `orders` テーブルには別の日付の注文番号（例: `ORD20260308-*`）が入っており、当日に新規注文しても番号が衝突しない |
| 会員ユーザーで発生 | 注文フローが完全に実行される会員ユーザーの方が確認しやすいが、採番ロジックは非会員でも同一であるため、非会員でも初期構築当日に初めて注文すれば同様に発生すると考えられる |
| 商品2件でも1件でも条件は同じ | 注文番号の採番は商品件数に依存しないため、件数によらず発生する |
| モジュール内容は同じ | コードには問題なく、データ状態の差異が原因 |

---

## 原因特定に必要な追加情報

現時点では状況証拠から推定原因を导いているが、以下の情報が得られれば確定診断が可能になる。

### 必要情報 1: アプリケーションのエラーログ（スタックトレース）

**目的**: システムエラーの具体的な例外クラス・メッセージを確認する。
UNIQUE制約違反であれば `org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "orders_order_number_key"` が記録されているはず。

**取得手順**:
1. 障害を発生させた環境のアプリケーションログを確認する
2. Spring Boot のデフォルトではコンソール出力、またはログファイル（`logs/spring.log` など）に出力される
3. 発生日時 2026/3/10 9:12 前後の `ERROR` レベルのログを検索する

```bash
# 例: ログファイルから検索
grep -A 30 "2026-03-10 09:12" logs/spring.log | grep -A 20 "ERROR"
```

### 必要情報 2: 障害発生環境の `order_number_counters` テーブルの内容

**目的**: シードデータ投入後に `order_number_counters` が空であったことを確認し、採番が `1` から始まったことを裏付ける。

**取得手順**:
```sql
-- 障害発生環境の PostgreSQL に接続して実行
SELECT * FROM order_number_counters ORDER BY order_date;
```

現在のカウンタ値が `1`（最初の注文だけ試みて失敗した場合は `0` のまま、または行なし）であれば推定原因を支持する。

### 必要情報 3: 障害発生環境の `orders` テーブルの当日日付分の件数

**目的**: シードデータに当日日付の注文が存在することを確認する。

**取得手順**:
```sql
-- 障害が発生した日付（初期構築日）を指定して実行
SELECT COUNT(*)
FROM orders
WHERE order_number LIKE 'ORD20260310-%';
```

100件ヒットすれば、シードが当日に実行されたことが確認できる。

### 必要情報 4: 障害発生環境の初期構築実施日

**目的**: シードの注文番号が障害発生日（2026/3/10）と一致するかを確認する。

**取得手順**:
- 初期構築を実施した日付をメンバーに直接確認する
- または以下のSQLで最古の注文の日付を確認する
```sql
SELECT MIN(order_datetime)::DATE AS min_order_date
FROM orders;
```

---

## まとめ

```
シードデータ投入（CURRENT_DATE = 初期構築日）
    ↓
orders テーブルに ORD{構築日}-000001 ～ ORD{構築日}-000100 が登録される
    ↓
order_number_counters は空のまま（シードで更新されない）
    ↓
構築当日に注文確定を実行
    ↓
nextOrderSequence() が order_number_counters に INSERT → last_sequence = 1 を返す
    ↓
生成された注文番号 ORD{構築日}-000001 が orders テーブルに既存
    ↓
INSERT INTO orders → UNIQUE制約違反 → トランザクションロールバック → システムエラー
```

別の日に初期構築した開発者では `orders` テーブルの注文番号の日付が異なるため、同日に注文しても番号が衝突せず正常に完了する。これにより「個人差」が生じる。

---

## 対応方針（修正は対象外、方針のみ記載）

シードデータに `order_number_counters` への初期データ投入を追加する、または注文番号の採番ロジックで既存の `orders` テーブルを参照してカウンタを補正する、のいずれかが有力な修正方針と考えられる。詳細は別途検討する。
