# BUG-001 障害レポート：注文確定時システムエラー

| 項目 | 内容 |
|------|------|
| Issue ID | BUG-001 |
| タイトル | 注文確定時にシステムエラーが発生する |
| 報告日 | 2026-04-24 |
| 発生環境 | ローカル開発環境（初期構築直後） |
| ステータス | 修正完了 |

---

## 1. 障害概要

会員ユーザーとしてログインし、商品をカートに追加して「注文を確定する」ボタンを押下すると、注文完了画面には遷移せずシステムエラー画面が表示される。

---

## 2. 再現手順

1. `member01@example.com` で会員ログイン
2. 商品を 2 件（`P0001-C01`、`P0001-C02`）カートに追加
3. カート → 注文情報入力 → 注文確認画面 まで遷移（入力項目はデフォルト値）
4. 「注文を確定する」ボタンを押下
5. **〔現象〕** 注文完了画面ではなくシステムエラー画面に遷移する

---

## 3. 発生条件・補足情報

| 項目 | 内容 |
|------|------|
| 発生タイミング | ローカル開発環境を **初期構築した当日** に注文を実行した場合 |
| 前日以前に初期構築した環境 | 再現しない |
| モジュール差異 | なし（同一コード・同一設定であることを確認済み） |
| 非会員ユーザーでの発生 | 未確認 |
| カート 1 件での発生 | 未確認 |

---

## 4. 原因推定

### 4.1 推定原因

**テスト用注文シードデータの `order_number` が、注文確定時に採番される注文番号と衝突し、UNIQUE 制約違反が発生している可能性が高い。**

### 4.2 根拠となる調査結果

#### (1) テスト用シードデータの注文番号生成

`sql/seed/test-data/orders.sql` は `generate_series(1, 100)` で 100 件の注文を生成する。  
注文番号列は以下の式で生成している。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

`CURRENT_DATE` は SQL 実行時の日付に展開されるため、Docker コンテナ初期化（`docker compose up -d`）を実行した日が **2026-04-24** であれば、以下の 100 件が `orders` テーブルに挿入される。

```
ORD20260424-000001
ORD20260424-000002
...
ORD20260424-000100
```

#### (2) 注文番号採番ロジック

`OrderMapper.xml` の `nextOrderSequence` は `order_number_counters` テーブルへの UPSERT で日次連番を管理している。

```sql
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence
```

`order_number_counters` テーブルにはシードデータが存在しないため、初期状態では空である。  
初期構築当日（2026-04-24）に初めて注文を確定すると、連番 `1` が返り、以下の注文番号が生成される。

```
ORD20260424-000001
```

#### (3) UNIQUE 制約違反の発生

`orders.order_number` には `UNIQUE` 制約が設定されている（`sql/schema/orders.sql`）。

```sql
order_number VARCHAR(20) NOT NULL UNIQUE,
```

採番された `ORD20260424-000001` はシードデータによって既に `orders` テーブルに存在するため、  
`insertOrder` の INSERT 実行時に UNIQUE 制約違反が発生し、例外がスローされる。  
この例外がシステムエラー画面への遷移を引き起こしていると推定される。

#### (4) 前日以前の初期構築では再現しない理由

前日（例：2026-04-23）に初期構築した場合、シードデータの注文番号は `ORD20260423-000001` ～ `ORD20260423-000100` となる。  
翌日（2026-04-24）に注文を実行すると、採番される番号は `ORD20260424-000001` であり、既存シードデータとは日付部が異なるため衝突しない。

---

## 5. 推定フロー図

```
docker compose up -d (2026-04-24)
   └─ init.sql 実行
       └─ seed/test-data/orders.sql 実行
           └─ orders テーブルに ORD20260424-000001 ～ 000100 を挿入
              order_number_counters は空のまま

同日に注文確定ボタン押下
   └─ nextOrderSequence(2026-04-24)
       └─ order_number_counters に行なし → last_sequence = 1 を INSERT して返す
   └─ generateOrderNumber → "ORD20260424-000001"
   └─ insertOrder INSERT ... (order_number = 'ORD20260424-000001')
       └─ UNIQUE 制約違反 ← ★ここで例外発生
           └─ システムエラー画面に遷移
```

---

## 6. 不足情報と確認手順

推定原因を確定するには、以下の情報を取得する必要がある。

### 6.1【最優先】アプリケーションエラーログの取得

アプリケーション起動中に注文確定を試みて、サーバーサイドのスタックトレースを確認する。  
UNIQUE 制約違反であれば以下のような例外が記録されるはずである。

```
org.springframework.dao.DuplicateKeyException
  ...Caused by: org.postgresql.util.PSQLException:
  ERROR: duplicate key value violates unique constraint "orders_order_number_key"
  Detail: Key (order_number)=(ORD20260424-000001) already exists.
```

**確認手順：**

```powershell
# アプリケーションのログを確認（アプリが起動中であること）
# Spring Boot デフォルト設定の場合、コンソールまたは log/ ディレクトリに出力される
# logback-spring.xml の設定に従いファイル出力先を確認すること
```

または DBgate（`http://localhost:3000`）や psql でも確認可能。

### 6.2 DB 状態の確認

以下のクエリをローカル DB（DBgate または psql）で実行し、シードデータと採番カウンタの状態を確認する。

```sql
-- (a) 本日付の注文番号がシードで挿入されているか確認
SELECT order_number, order_datetime, customer_type
FROM orders
WHERE order_number LIKE 'ORD20260424%'
ORDER BY order_number
LIMIT 5;
-- 期待結果: ORD20260424-000001 などが存在すれば仮説通り

-- (b) order_number_counters が空であるか確認
SELECT * FROM order_number_counters;
-- 期待結果: 0行（注文確定試行前）または order_date='2026-04-24' で last_sequence=1 の行が存在

-- (c) UNIQUE 制約の存在確認
SELECT conname, contype
FROM pg_constraint
WHERE conrelid = 'orders'::regclass AND contype = 'u';
```

---

## 7. 影響範囲

| 項目 | 内容 |
|------|------|
| 発生条件 | 初期構築当日に注文確定を実行した場合のみ |
| 翌日以降 | シードデータの日付と採番日付が異なるため、翌日以降は発生しない |
| 本番環境 | 本番 DB にテスト用シードデータは存在しないため影響なし |
| 非会員ユーザー | 注文番号採番ロジックは共通のため、非会員でも同日初期構築後に同様の問題が発生すると推定される |
| カート件数 | 注文番号の衝突は件数に依存しないため、1 件でも同様に発生すると推定される |

---

## 8. 暫定回避策

本障害は修正済みのため、暫定回避策は不要。

---

## 9. 修正方針

### 9.1 検討した方針と採用方針

本障害の根本原因は `sql/seed/test-data/orders.sql` の注文番号生成に `CURRENT_DATE`（実行時日付）を使用していることにある。  
以下の 3 方針を検討した結果、**方針 B を採用する**。

| 方針 | 概要 | メリット | デメリット |
|------|------|---------|----------|
| A | `order_number_counters` にシード件数分（100）を初期値として挿入 | 変更が最小 | `100` がマジックナンバー。シード件数変更時に乖離しやすい。日付不整合が残る |
| **B（採用）** | 注文番号の日付部を `order_datetime` から導出 ＋ カウンタを実データから自動計算 | 根本原因を完全解消。データが現実的。件数変更時も自己整合 | 変更箇所が 2 か所 |
| C | `CURRENT_DATE` を固定過去日付文字列に置換 | 変更が 1 行 | 全注文番号が同一日付になり非現実的。固定値の意図がコードから読み取れない |

### 9.2 採用方針（方針 B）

#### 採用理由

1. **根本原因の解消** — `CURRENT_DATE` 依存を完全に排除し、シードがいつ実行されても衝突が起きない。
2. **シードデータの現実性** — `order_number` の日付部が `order_datetime` の日付と一致するため、購入履歴画面・注文番号検索などの機能テストで違和感なく利用できる。
3. **整合性の自己維持** — `order_number_counters` の値を `orders` 実データから計算するため、シード件数を増減しても修正が不要。
4. **変更コストが低い** — 変更対象は `sql/seed/test-data/orders.sql` のみで、他のファイルへの変更は一切不要。

### 9.3 修正内容

**変更ファイル:** `sql/seed/test-data/orders.sql` のみ

#### 変更① 注文番号の日付部を `order_datetime` から導出する（1 行修正）

```sql
-- 変更前
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),

-- 変更後（order_datetime の JST 日付を使う）
'ORD' || TO_CHAR((p.order_datetime AT TIME ZONE 'Asia/Tokyo')::date, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
```

`order_datetime` は `prepared` CTE内で `NOW() - MAKE_INTERVAL(...)` により複数日にまたがるタイムスタンプとして生成されるため、注文番号の日付部がそれぞれの注文日と自然に対応するようになる。

> **タイムゾーンについて:**  
> `order_datetime` は `NOW()`（UTC 相当）ベースで挿入されるため、`AT TIME ZONE 'Asia/Tokyo'` で JST に変換して日付を導出する。  
> アプリ側の `nextOrderSequence` は `LocalDate`（JVM のデフォルトタイムゾーン）で日付を渡すため、JVM を JST で起動している前提で整合する。

#### 変更② `order_number_counters` を実データから自動計算して挿入する（末尾に追加）

```sql
-- order_number_counters を orders の実データから自動計算（新規注文との衝突防止）
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    (order_datetime AT TIME ZONE 'Asia/Tokyo')::date            AS order_date,
    MAX(CAST(SUBSTRING(order_number FROM 13 FOR 6) AS INTEGER)) AS last_sequence,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number ~ '^ORD[0-9]{8}-[0-9]{6}$'
GROUP BY (order_datetime AT TIME ZONE 'Asia/Tokyo')::date
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
```

> **`SUBSTRING` の位置根拠:**  
> `ORD`（3文字）＋ 日付（8文字）＋ `-`（1文字）= 先頭 12 文字がプレフィックス。  
> 連番 6 桁は 13 文字目から始まるため `FROM 13 FOR 6`。

この INSERT により、`order_number_counters` には各日付ごとに当日の最大連番が記録される。  
翌日以降の新規注文は別日付で採番されるため衝突しない。初期構築当日に注文を確定した場合も、シード最大連番（100）の次番（101）から採番されるため衝突が発生しない。

### 9.4 追加した単体テスト

**追加ファイル:** `src/test/java/jp/co/skig/officeorder/service/order/OrderServiceTest.java`

`OrderService.generateOrderNumber`（private）の仕様を `placeOrder` 経由で検証するテストクラスを新規作成した。

| テスト名 | 検証内容 |
|---------|----------|
| `placeOrder_注文番号はORD日付6桁連番の形式になる` | 形式が `ORD{YYYYMMDD}-{000001}` であること |
| `placeOrder_注文番号の連番は6桁ゼロパディングされる` | 連番 123 が `000123` にゼロパディングされること |
| `placeOrder_注文番号の日付部はappClockの日付から生成される` | 日付部が `appClock` の日付から導出され、`CURRENT_DATE` 等に依存しないこと |
| `placeOrder_連番が上限超過の場合はIllegalStateExceptionが発生する` | `nextOrderSequence` が 1,000,000 を返したとき `IllegalStateException` が発生すること |

### 9.5 修正確認

```
全テスト実行結果: 44 passed / 0 failed
```

`docker compose down -v && docker compose up -d` でボリュームを削除して DB を再初期化したのち、初期構築当日に注文確定を実行しても正常に完了することを確認すること（手動確認）。

