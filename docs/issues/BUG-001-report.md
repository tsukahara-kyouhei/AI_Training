# BUG-001 障害レポート — 注文確定時のシステムエラー

| 項目 | 内容 |
|------|------|
| 発生日時 | 2026/3/10 9:12 |
| 報告日 | 2026/4/14 |
| 対象機能 | 注文確定（POST /checkout/confirm） |
| 重大度 | High（ローカル環境で初期構築当日に注文操作が完全に不可能） |
| 対応状態 | 修正方針確定（未対応） |
| 採用修正方針 | 案C — `order_datetime` の日付を注文番号プレフィックスに利用 |

---

## 1. 事象

`POST /checkout/confirm` で「注文を確定する」ボタンを押下するとシステムエラー画面に遷移する。

### 再現手順
1. 会員ユーザー（member01@example.com）でログイン
2. カートに商品を 2 件（P0001-C01、P0001-C02）追加
3. カートから注文確認画面まで遷移（各入力項目はデフォルトのまま）
4. 「注文を確定する」ボタン押下
5. → システムエラー画面に遷移

### 再現条件
- **ローカル開発環境の初期構築当日（`init-local-postgres.ps1` 実行日と同日）に発生**
- 前日以前に初期構築した環境では再現しない（コードは同一）

---

## 2. 推定原因

### 根本原因（High Confidence）

**シードデータが `CURRENT_DATE`（初期化実行日）を注文番号のプレフィックスとして使用しており、同日に新規注文を置くと `orders.order_number` の UNIQUE 制約違反が発生する。**

#### 詳細

##### ① シードデータの注文番号生成（`sql/seed/test-data/orders.sql` 86行目付近）

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
```

`generate_series(1, 100)` のループで、以下 100 件の注文番号を生成・挿入する：

```
ORD{初期化実行日YYYYMMDD}-000001
ORD{初期化実行日YYYYMMDD}-000002
...
ORD{初期化実行日YYYYMMDD}-000100
```

例：2026/3/10 に初期構築を実施した場合、`orders` テーブルには `ORD20260310-000001` ～ `ORD20260310-000100` が存在する。

##### ② 日次連番テーブルが未投入（`order_number_counters` 空）

シードデータは `order_number_counters` テーブルに一切 INSERT しない。
初期構築直後、このテーブルは空の状態になる。

##### ③ 新規注文の番号採番ロジック（`OrderService.generateOrderNumber()`）

```java
private String generateOrderNumber(OffsetDateTime orderDatetime) {
    LocalDate orderDate = orderDatetime.toLocalDate();
    int sequence = orderRepository.nextOrderSequence(orderDate);  // → 1
    ...
    return "ORD" + datePart + "-" + String.format("%06d", sequence); // → "ORD20260310-000001"
}
```

`nextOrderSequence` は `order_number_counters` テーブルへの `INSERT ... ON CONFLICT DO UPDATE ... RETURNING last_sequence` を実行する：

```sql
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
  SET last_sequence = order_number_counters.last_sequence + 1
RETURNING last_sequence
```

`order_number_counters` が空なので初回は必ず `last_sequence = 1` が返る。

##### ④ UNIQUE 制約違反の発生フロー

| ステップ | 内容 |
|----------|------|
| 1 | `generateOrderNumber(2026-03-10T09:12:...)` が呼ばれる |
| 2 | `nextOrderSequence('2026-03-10')` → `1` を返す（`order_number_counters` は空） |
| 3 | 注文番号 `ORD20260310-000001` を生成 |
| 4 | `insertOrder(...)` で `orders` テーブルへ INSERT を試みる |
| 5 | `order_number` 列の UNIQUE 制約違反（`ORD20260310-000001` は既にシードデータで挿入済み） |
| 6 | `DataIntegrityViolationException` が `@Transactional` メソッド外に伝播 |
| 7 | 未ハンドリングの例外 → システムエラー画面に遷移 |

#### 人によって再現しない理由

| ケース | 初期構築日 | 注文試行日 | シードの注文番号プレフィックス | 生成される注文番号 | 結果 |
|--------|-----------|------------|-------------------------------|-------------------|------|
| 先に構築した人 | 3/8 以前 | 3/10 | `ORD20260308-*` | `ORD20260310-000001` | **衝突なし → 正常** |
| 今回の報告者 | 3/10 | 3/10 | `ORD20260310-*` | `ORD20260310-000001` | **衝突 → エラー** |

---

## 3. 原因確定に必要な追加情報

以下の情報が取得できれば原因を確定できる。**現時点では状況証拠のみであり、仮説の段階であることに留意すること。**

### 3-1. アプリケーションログ（最優先）

発生日時（2026/3/10 9:12）付近の Spring Boot ログ（`log/` または標準出力）から例外スタックトレースを確認する。

```bash
# 例: logback-spring.xml のログ出力先を確認しファイルを参照
# または console 出力をスクロールする
```

**確認ポイント**: 例外クラスが `org.springframework.dao.DataIntegrityViolationException` または `org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "orders_order_number_key"` であれば本仮説が確定する。

### 3-2. データベース状態の確認

現在のローカル環境が 2026/3/10 に構築されたものであれば、以下のクエリで確認できる。

```sql
-- ① シードで挿入された当日分の注文番号が存在するか
SELECT count(*), min(order_number), max(order_number)
FROM orders
WHERE order_number LIKE 'ORD20260310-%';
-- 期待値: count = 100 (if 3/10 setup)

-- ② order_number_counters に 2026-03-10 のレコードが存在するか
SELECT * FROM order_number_counters WHERE order_date = '2026-03-10';
-- 仮説通りなら: 0 行（もしくは last_sequence = 1 で失敗した痕跡）
```

### 3-3. 初期構築の実施日

`git log` や `orders` テーブルの `created_at` 最小値から、初期構築が 2026/3/10 であったことを確認する。

```sql
SELECT MIN(created_at) FROM orders;
```

---

## 4. 影響範囲

| 項目 | 内容 |
|------|------|
| 対象環境 | ローカル開発環境のみ（本番・ステージング環境のシードデータ構成による） |
| 再現日 | 初期構築実行日のみ（翌日以降は `CURRENT_DATE` が変わるため衝突しない） |
| 会員 / 非会員 | 未確認。注文番号採番ロジックはどちらも共通のため、同様に再現すると考えられる |
| カート内の商品数 | 注文番号採番時点（`insertOrder` 直前）に衝突するため、1 件でも同様に再現すると考えられる |
| 本番環境 | 本番環境ではシードデータは投入しないため、通常は影響なし |

---

## 5. 暫定回避策

初期構築当日の注文動作確認をスキップするか、以下のいずれかを手動実施する。

```sql
-- ① 当日分のカウンターを100に設定する（シードの最大連番に合わせる）
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
VALUES (CURRENT_DATE, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (order_date)
  DO UPDATE SET last_sequence = 100, updated_at = CURRENT_TIMESTAMP;
```

これにより次回の `nextOrderSequence` は `101` を返し、`ORD{TODAY}-000101` から採番されて衝突しなくなる。

---

## 6. 修正方針

### 6.1 方針比較

| 案 | 変更対象 | 変更量 | メリット | デメリット |
|----|----------|--------|----------|------------|
| A | `orders.sql` 1行変更 | 小 | 変更が最小限 | `order_number` 日付と `order_datetime` が乖離。全件同一日付プレフィックスで現実感がない |
| B | `orders.sql` 末尾にINSERT追加 | 小 | `order_number_counters` との整合性が保たれる | `CURRENT_DATE` 依存が残り `order_number` と `order_datetime` の不整合も未解消 |
| **C ★採用** | `orders.sql` CTE追加＋3箇所修正 | 中 | `order_number` 日付と `order_datetime` が一致。`CURRENT_DATE` 依存を完全排除。複数日付データで日付範囲検索の動作確認が容易 | SQL変更量が最も多い |

---

### 6.2 採用方針（案C）の選定理由

本障害の本質は「シードデータが `CURRENT_DATE` に依存しており、実行日によって動作が変わる」ことにある。案Aは最小変更だが `order_number` と `order_datetime` の不整合という別の問題を生む。案Bは `order_number_counters` の整合性を確保するが、全注文が実行当日付けになるという不自然さは残る。

案Cは `CURRENT_DATE` 依存を根本から排除し、注文番号と注文日時の一貫性を保った現実的なテストデータを生成する。変更はシードSQL1ファイル(`sql/seed/test-data/orders.sql`)内のみにとどまり、アプリケーション側のコード変更は一切不要である。

---

### 6.3 修正内容（`sql/seed/test-data/orders.sql`）

以下の4箇所を変更する。

#### 変更1: `prepared` CTE — `order_datetime` の最小値を1日前に保証

`days => ((s.n - 1) % 7)` は n=1,8,15... のとき `days => 0`（当日）になるため、`+1` を加えて最小でも1日前になるよう修正する。

```diff
- WHEN s.n <= 55 THEN NOW() - MAKE_INTERVAL(days => ((s.n - 1) % 7), hours => (s.n % 6), mins => (s.n % 50))
+ WHEN s.n <= 55 THEN NOW() - MAKE_INTERVAL(days => ((s.n - 1) % 7) + 1, hours => (s.n % 6), mins => (s.n % 50))
```

#### 変更2: `with_order_number` CTE を追加

`prepared` CTE の閉じカッコ `)` の直後（`INSERT INTO orders` の直前）に追加する。

```diff
  )
+ with_order_number AS (
+     SELECT
+         p.*,
+         'ORD' || TO_CHAR(p.order_datetime, 'YYYYMMDD') || '-' ||
+             LPAD(
+                 ROW_NUMBER() OVER (
+                     PARTITION BY p.order_datetime::date
+                     ORDER BY p.n
+                 )::TEXT,
+             6, '0'
+         ) AS order_number
+     FROM prepared p
+ )
  INSERT INTO orders (
```

`PARTITION BY order_datetime::date ORDER BY p.n` により、同一日の注文は `n` の昇順で `000001` から連番採番される。

#### 変更3: INSERT SELECT の `order_number` 列

```diff
  SELECT
-     'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
+     p.order_number,
      p.order_datetime,
```

#### 変更4: INSERT SELECT の `FROM` 句

```diff
- FROM prepared p;
+ FROM with_order_number p;
```

#### 変更後の動作イメージ

| n | order_datetime（例） | order_number（変更後） |
|---|----------------------|------------------------|
| 1 | 2026-04-13（1日前） | ORD20260413-000001 |
| 2 | 2026-04-13（1日前） | ORD20260413-000002 |
| 8 | 2026-04-07（7日前） | ORD20260407-000001 |
| 56 | 2026-03-22（23日前相当） | ORD20260322-000001 |

`order_number_counters` テーブルは空のまま正しい状態となり、アプリが当日初めて注文を採番すると `ORD{today}-000001` から始まるため、過去日付のシードデータとは絶対に衝突しない。

---

## 7. 参照ファイル

| ファイル | 役割 |
|---------|------|
| [sql/seed/test-data/orders.sql](../../sql/seed/test-data/orders.sql) | 問題のシードデータ（`CURRENT_DATE` を使用） |
| [sql/schema/orders.sql](../../sql/schema/orders.sql) | `orders.order_number UNIQUE` 制約の定義 |
| [scripts/init-local-postgres.ps1](../../scripts/init-local-postgres.ps1) | 初期構築スクリプト |
| [src/main/java/jp/co/skig/officeorder/service/order/OrderService.java](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java) | `generateOrderNumber()` / `placeOrder()` |
| [src/main/resources/mappers/OrderMapper.xml](../../src/main/resources/mappers/OrderMapper.xml) | `nextOrderSequence` SQL |
