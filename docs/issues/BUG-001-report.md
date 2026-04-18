# BUG-001 障害レポート：注文確定時のシステムエラー

## 基本情報

| 項目 | 内容 |
|---|---|
| 障害ID | BUG-001 |
| 発生日時 | 2026/3/10 9:12 |
| 報告日 | 2026/4/18 |
| 対象機能 | 注文確定（`POST /checkout/confirm`） |
| 影響範囲 | ローカル開発環境（初期構築当日） |
| 深刻度 | High（注文確定が完全に不可） |

---

## 事象

会員ユーザーがカートに商品を追加して注文確定を試みると、注文完了画面ではなくシステムエラー画面（HTTP 500）に遷移する。

---

## 原因（ログにより確定）

```
org.springframework.dao.DuplicateKeyException
  Cause: PSQLException: ERROR: duplicate key value violates unique constraint "orders_order_number_key"
  Key (order_number)=(ORD20260418-000001) already exists.
  at OrderRepository.insertOrder (OrderRepository.java:84)
  at OrderService.placeOrder (OrderService.java:201)
```

### 結論

**シードデータの注文番号と `order_number_counters` テーブルの不整合により、`orders.order_number` の UNIQUE 制約違反が発生した。**

### 詳細

#### 注文番号採番の仕組み

アプリは注文確定時に以下の処理で注文番号を採番する。

1. `OrderService.generateOrderNumber()` が `OrderRepository.nextOrderSequence(当日日付)` を呼ぶ
2. `nextOrderSequence` は `order_number_counters` テーブルに対して下記 SQL を実行する

   ```sql
   INSERT INTO order_number_counters (order_date, last_sequence, ...)
   VALUES (#{orderDate}, 1, ...)
   ON CONFLICT (order_date) DO UPDATE
     SET last_sequence = order_number_counters.last_sequence + 1, ...
   RETURNING last_sequence
   ```

3. 当日の `order_number_counters` レコードが存在しない場合は `1` が返る
4. 生成される注文番号は `ORD{YYYYMMDD}-000001`（例: `ORD20260310-000001`）

#### シードデータの問題

`sql/seed/test-data/orders.sql` は、**シード実行日の `CURRENT_DATE`** を使って 100 件の注文を生成する。

```sql
-- orders.sql の注文番号生成箇所
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
```

これにより、シード実行日が `2026/3/10` であれば `ORD20260310-000001` 〜 `ORD20260310-000100` が `orders` テーブルに挿入される。

一方、**`order_number_counters` テーブルはシードデータに含まれておらず、初期状態では空のまま**となる。

#### 障害発生の流れ

```
[シード実行日 = 2026/3/10]
  orders テーブル : ORD20260310-000001 〜 ORD20260310-000100 が存在
  order_number_counters : 空（2026-03-10 のレコードなし）

[当日 9:12 に注文確定操作]
  1. nextOrderSequence(2026-03-10)
       → order_number_counters に (2026-03-10, 1) を INSERT（競合なし）
       → 返り値 = 1
  2. 生成注文番号 = "ORD20260310-000001"
  3. INSERT INTO orders (..., order_number = 'ORD20260310-000001', ...)
       → UNIQUE 制約違反 ※orders.order_number は UNIQUE 制約あり
  4. DataIntegrityViolationException がスロー
  5. CartController の catch 節は IllegalArgumentException のみ補足
       → 例外が伝播し HTTP 500 → システムエラー画面
```

#### 他の人で再現しなかった理由

先に初期構築した人がシードを実行したのが `2026/3/10` より前（例: `2026/3/9`）であれば、シードが生成した注文番号は `ORD20260309-000001` 〜 `ORD20260309-000100` となる。`2026/3/10` に注文を試みると、`order_number_counters` に `2026-03-10` のレコードが存在しないため `1` を採番し、`ORD20260310-000001` を生成するが、この番号は `orders` テーブルに存在しないため UNIQUE 制約違反は発生しない。

### 関連コード・ファイル

| ファイル | 関連箇所 |
|---|---|
| [sql/seed/test-data/orders.sql](../../sql/seed/test-data/orders.sql) | 注文番号を `TO_CHAR(CURRENT_DATE, 'YYYYMMDD')` で生成（L8付近） |
| [sql/schema/orders.sql](../../sql/schema/orders.sql) | `order_number VARCHAR(20) NOT NULL UNIQUE`（L2） |
| [sql/schema/orders.sql](../../sql/schema/orders.sql) | `order_number_counters` テーブル定義（シードなし） |
| [src/main/resources/mappers/OrderMapper.xml](../../src/main/resources/mappers/OrderMapper.xml) | `nextOrderSequence` の SQL（`ON CONFLICT DO UPDATE`） |
| [src/main/java/.../service/order/OrderService.java](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java) | `generateOrderNumber()` メソッド |
| [src/main/java/.../web/CartController.java](../../src/main/java/jp/co/skig/officeorder/web/CartController.java) | `POST /checkout/confirm` — `IllegalArgumentException` のみ補足 |

---

## 修正方針

### 根本原因のまとめ

`sql/seed/test-data/orders.sql` は注文番号を `TO_CHAR(CURRENT_DATE, 'YYYYMMDD')` で生成するため、**シード実行日と注文確定日が UTC 基準で一致すると `ORD{当日}-000001` が衝突する**。`order_number_counters` がシードデータに含まれておらず、アプリが採番を `1` から始めてしまうことが直接の原因。

### 修正案

#### 案 A（推奨）: `order_number_counters` をシードで初期化する

`order_number_counters` にシードで挿入した件数分のカウンターを追加する。アプリはこの値から採番を続けるため、衝突が発生しなくなる。

**修正対象**: [sql/seed/test-data/orders.sql](../../sql/seed/test-data/orders.sql) の末尾に追加

```sql
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    CURRENT_DATE,
    COUNT(*),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number LIKE 'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '%'
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = EXCLUDED.last_sequence,
        updated_at = CURRENT_TIMESTAMP;
```

**利点**: シード件数が変わっても自動的に追従する。`orders` の注文番号形式を変更しなくて済む。

---

#### 案 B（参考）: シードの注文番号を固定の過去日付にする

注文番号生成を `CURRENT_DATE` から固定の過去日付（例: `'20260101'`）に変更し、今日の日付と衝突しないようにする。

**修正対象**: `orders.sql` の注文番号生成部分

```sql
-- 変更前
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),

-- 変更後
'ORD20260101-' || LPAD(p.n::TEXT, 6, '0'),
```

**欠点**: 注文番号の日付部分と `order_datetime` の日付が乖離するため、データとして不自然になる。

---

### 採用案

**案 A を採用する。** 案 B は見た目のデータ整合性が崩れるため不採用。案 A はシード終了時に `order_number_counters` を正しい状態にするという、本来あるべき初期化として自然な修正である。

---

## 補足

- 非会員ユーザーや商品 1 件の場合も同様に発生する（注文番号の衝突はカート内容と無関係）
- 翌 UTC 日以降にシードした場合は採番日がズレるため発生しない
