# BUG-001 調査レポート: 注文確定時にシステムエラー

## 1. バグ概要

| 項目       | 内容                                                   |
| ---------- | ------------------------------------------------------ |
| 報告日時   | 2026/3/10 9:12                                         |
| 対応日時   | 2026/4/27                                              |
| ステータス | **対応済み**                                           |
| 重要度     | Critical（購入不能）                                   |
| 再現条件   | ローカル開発環境の初期構築当日に注文確定を実行した場合 |

### 再現手順（報告時）

1. ローカル環境を初期構築（スキーマ作成 + seed 投入）
2. `member01@example.com` でログイン
3. カートに P0001-C01、P0001-C02 を追加
4. 注文情報を入力し確認画面へ遷移
5. 「注文を確定する」ボタンを押下 → **システムエラー画面が表示される**

---

## 2. 根本原因（特定済み）

### 原因: seed 投入当日に注文番号が重複する

#### seed の `order_number` 生成ロジック

`sql/seed/test-data/orders.sql` は 100 件の注文を以下のフォーマットで挿入する。

```sql
'ORD' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0')
-- 例: ORD20260310-000001 〜 ORD20260310-000100
```

`CURRENT_DATE` はスクリプト実行時のシステム日付を使用するため、seed は**投入当日の日付**を持つ注文番号を 100 件生成する。

#### アプリケーションの `order_number` 生成ロジック

`OrderService.generateOrderNumber()` は以下の形式で採番する。

```java
// OrderService.java (generateOrderNumber)
"ORD" + orderDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%06d", sequence)
// 例 (2026/3/10 の 1 件目): ORD20260310-000001
```

`sequence` 値は `order_number_counters` テーブルへの UPSERT で発行される。

```sql
-- OrderMapper.xml (nextOrderSequence)
INSERT INTO order_number_counters (order_date, last_sequence, ...)
VALUES (#{orderDate}, 1, ...)
ON CONFLICT (order_date) DO UPDATE
SET last_sequence = order_number_counters.last_sequence + 1, ...
RETURNING last_sequence
```

`order_number_counters` には seed データが一切挿入されないため、**seed 投入当日の最初の注文では必ず `sequence = 1` が返る**。

#### 衝突の発生

| ステップ     | 内容                                                                               |
| ------------ | ---------------------------------------------------------------------------------- |
| seed 実行時  | `ORD20260310-000001` 〜 `ORD20260310-000100` が `orders.order_number` に挿入される |
| seed 実行後  | `order_number_counters` にレコードなし                                             |
| 注文確定時   | `nextOrderSequence()` → `sequence = 1` (当日初回)                                  |
| 注文番号生成 | `ORD20260310-000001` (seed 済みの値と同一)                                         |
| INSERT 実行  | **UNIQUE 制約違反** (`orders.order_number` は UNIQUE 制約付き)                     |

```
org.springframework.dao.DataIntegrityViolationException:
  ### Error updating database.  Cause: org.postgresql.util.PSQLException:
  ERROR: duplicate key value violates unique constraint "orders_order_number_key"
  Detail: Key (order_number)=(ORD20260310-000001) already exists.
```

この例外は `CartController.placeOrder()` で `IllegalArgumentException` のみを捕捉しているため、`DataIntegrityViolationException` はそのまま伝播し、Spring Boot のデフォルトエラーハンドラによってシステムエラー画面が表示される。

---

## 3. 報告者の環境で再現し、他のメンバーの環境では再現しない理由

### 再現した環境（報告者）

- **seed 投入日 = 注文実行日 = 2026/3/10**
- seed が `CURRENT_DATE = 2026-03-10` を使って `ORD20260310-000001` を挿入
- そのまま同日に注文を試みたため `ORD20260310-000001` が重複 → エラー

### 再現しない環境（先の担当者）

以下のどちらかの理由で衝突が起きない。

| パターン                        | 理由                                                                                                                     |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| seed を別日に実行していた       | seed が例えば `ORD20260305-XXXXXX` を生成しており、3/10 の注文 (`ORD20260310-000001`) とは日付部分が異なるため衝突しない |
| seed 当日に注文まで実施していた | `order_number_counters` に当日分のレコードが残り、次の注文では `sequence ≥ 101` が発行されるため衝突しない               |

### 調査者の環境でも再現しなかった理由

現在のログ (`log/office-order.log`) には下記のようにに成功した注文が記録されており、seed 投入日と注文日が異なっていたことで衝突が発生しなかったと推定される。

```
2026-04-27 01:44:28.619 INFO  [...] POST /checkout/confirm :
  event=order_place_end memberId=1 orderId=101 orderNumber=ORD20260427-000001 itemCount=2
```

`orderId=101` は seed で挿入した 100 件の直後であり、seed 実行日は 2026/04/27 ではなかった（別日に seed を投入 → 当日注文で衝突なし）。

---

## 4. 影響範囲

- **対象**: ローカル開発環境の初期構築当日に注文確定を試みるすべての開発者
- **本番環境**: seed データは本番に投入しないため、本番での発生リスクはなし
- **前日以降**: seed 投入の翌日以降は衝突しない（日付部分が変わるため）

---

## 5. 対応内容

### 修正ファイル 1: `sql/seed/test-data/orders.sql`

seed は `CURRENT_DATE` を使って `ORD{YYYYMMDD}-000001` 〜 `ORD{YYYYMMDD}-000100` を挿入するため、seed 実行後に当日の最終連番（100）を `order_number_counters` へ登録する。これにより、初回注文の採番が `sequence = 101` から開始され、重複が発生しなくなる。

```sql
-- BUG-001 修正: seed が CURRENT_DATE で ORD{YYYYMMDD}-000001 〜 ORD{YYYYMMDD}-000100 を挿入するため、
-- 同日に注文確定すると order_number が重複し UNIQUE 制約違反が発生していた。
-- seed 実行後に当日の最終連番 (100) を order_number_counters に登録することで、
-- 初回注文の採番を sequence = 101 から開始させ衝突を防ぐ。
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
VALUES (CURRENT_DATE, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, 100),
        updated_at    = CURRENT_TIMESTAMP;
```

### 追加ファイル 2: `src/test/java/.../service/order/OrderServiceTest.java`

`OrderService` のユニットテストを新規作成した。BUG-001 の根本原因である採番ロジックを中心に、以下のケースを検証している。

| テストクラス           | テストケース                       | 確認内容                                         |
| ---------------------- | ---------------------------------- | ------------------------------------------------ |
| `GenerateOrderNumber`  | `sequence=1`                       | `ORD20260310-000001` が返る（seed と同形式）     |
| `GenerateOrderNumber`  | `sequence=101`（修正後）           | `ORD20260310-000101` が返る（seed と衝突しない） |
| `GenerateOrderNumber`  | `sequence=999_999`（上限）         | 正常に採番できる                                 |
| `GenerateOrderNumber`  | `sequence=1_000_000`（上限超過）   | `IllegalStateException`                          |
| `GenerateOrderNumber`  | `nextOrderSequence` の呼び出し検証 | 正しい日付で委譲されること                       |
| `PlaceOrderValidation` | `null` カート                      | `IllegalArgumentException`                       |
| `PlaceOrderValidation` | 空カート                           | `IllegalArgumentException`                       |
| `PlaceOrderValidation` | 法人＋会社名なし                   | `IllegalArgumentException`                       |
| `PlaceOrderValidation` | 在庫切れ                           | `IllegalArgumentException`                       |
| `PlaceOrderValidation` | 注文数量 > 在庫                    | `IllegalArgumentException`                       |

---

## 6. 修正後の確認手順

以下の手順で修正が正しく機能することを確認する。

```powershell
# 1. DB をリセットして修正済み seed を投入
.\scripts\init-local-postgres.ps1
```

```sql
-- 2. order_number_counters に当日レコードが登録されていることを確認
SELECT * FROM order_number_counters WHERE order_date = CURRENT_DATE;
-- 期待値: order_date=<本日>, last_sequence=100 の行が 1 件存在する
```

```powershell
# 3. アプリケーションを起動
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

4. ブラウザで `member01@example.com` でログインし、P0001-C01・P0001-C02 をカートに追加して注文確定する
5. 注文完了画面が表示されること、生成される注文番号が `ORD{YYYYMMDD}-000101` であることを確認する

---

## 7. 調査・対応に使用したファイル

| ファイル                                                                                                                                                   | 確認・変更した点                                                                           |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| [sql/seed/test-data/orders.sql](../../sql/seed/test-data/orders.sql)                                                                                       | **修正**: `order_number_counters` へ当日連番上限（100）を登録する UPSERT を追加            |
| [sql/schema/orders.sql](../../sql/schema/orders.sql)                                                                                                       | `order_number` が `UNIQUE` 制約付き・PK が `GENERATED ALWAYS AS IDENTITY`                  |
| [src/main/java/jp/co/skig/officeorder/service/order/OrderService.java](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java)         | `generateOrderNumber()` のフォーマットが seed と同一であること                             |
| [src/main/resources/mappers/OrderMapper.xml](../../src/main/resources/mappers/OrderMapper.xml)                                                             | `nextOrderSequence` の UPSERT SQL                                                          |
| [scripts/init-local-postgres.ps1](../../scripts/init-local-postgres.ps1)                                                                                   | seed 実行手順（`order_number_counters` のリセット処理なし）                                |
| [log/office-order.log](../../log/office-order.log)                                                                                                         | `orderId=101`・`orderNumber=ORD20260427-000001` から seed 投入日 ≠ 注文日であることを確認  |
| [src/test/java/jp/co/skig/officeorder/service/order/OrderServiceTest.java](../../src/test/java/jp/co/skig/officeorder/service/order/OrderServiceTest.java) | **新規追加**: 採番ロジック・バリデーション合計 10 ケースのユニットテスト（全パス確認済み） |
