# BUG-001 注文確定時のシステムエラー障害レポート

## 1. 障害概要

| 項目 | 内容 |
| --- | --- |
| 障害ID | BUG-001 |
| 発生日時 | 2026/3/10 9:12 |
| 事象 | 会員 `member01@example.com` が `P0001-C01` と `P0001-C02` をカートに入れて注文確定すると、システムエラー画面へ遷移する |
| 期待結果 | 注文完了画面へ遷移する |
| 調査範囲 | リポジトリ内の注文確定処理、DB スキーマ、seed、初期化スクリプト、ログ処理 |
| 実装変更 | なし |

## 2. 原因推定

### 主原因候補: 注文番号の一意制約違反

**推定確度: 高。DB ログで例外を確認するまでは確定ではない。**

初期構築時のデータ投入と注文確定時の採番が、同じ注文番号を生成する可能性がある。

1. `sql/seed/orders.sql` は注文番号を次の形式で生成する。

   `ORD` + `CURRENT_DATE` の `YYYYMMDD` + `-000001` ～ `-000100`

2. 注文確定時の `OrderService.placeOrder` は `order_number_counters` から当日の次連番を取得する。

3. `scripts/init-local-postgres.ps1` は `sql/schema/orders.sql` でカウンタテーブルを作成した後、`sql/seed/orders.sql` を投入するが、seed 注文に対応する `order_number_counters` の行を作成・同期していない。

4. 2026/3/10 の初期構築直後であれば、seed 注文に `ORD20260310-000001` が存在し、カウンタは未登録である。注文確定時の採番は初回値 `1` となるため、同じ `ORD20260310-000001` を `orders.order_number` に INSERT する。

5. `orders.order_number` には `UNIQUE` 制約があるため PostgreSQL が INSERT を拒否する。`/checkout/confirm` が捕捉するのは `IllegalArgumentException` のみであり、DB の一意制約違反は共通の未処理例外ログを経てシステムエラー画面へ遷移すると考えられる。

この推定は、次の補足とも整合する。

- 初期構築直後に発生した。
- モジュール内容が同じでも、DB の初期化状態や初期化日時が異なると発生条件が変わる。
- 商品が2件であることは、注文ヘッダ INSERT の一意制約違反より後の処理なので、本候補では本質的な条件ではない。
- 会員ユーザーであることも、注文番号衝突自体には影響しない。非会員でも同様の衝突が起きる可能性がある。

### コード上の処理経路

- [CartController.java](../../src/main/java/jp/co/skig/officeorder/web/CartController.java) の `POST /checkout/confirm` が注文確定を開始する。
- [OrderService.java](../../src/main/java/jp/co/skig/officeorder/service/order/OrderService.java) の `placeOrder` が検証後に注文番号を採番し、注文ヘッダを保存する。
- [OrderMapper.xml](../../src/main/resources/mappers/OrderMapper.xml) の `nextOrderSequence` はカウンタ未登録時に `1` を返し、`insertOrder` は生成された番号を `orders` に INSERT する。
- [orders.sql](../../sql/schema/orders.sql) で `orders.order_number` は `NOT NULL UNIQUE` である。
- [GlobalExceptionLoggingAdvice.java](../../src/main/java/jp/co/skig/officeorder/web/GlobalExceptionLoggingAdvice.java) は未処理例外を `unhandled_exception` として記録して再送出する。

## 3. 現時点で確定できない事項

障害票だけでは、実際に 2026/3/10 9:12 のリクエストで発生した例外クラス、衝突した注文番号、DB の状態までは確認できない。次の情報が不足している。

1. アプリケーションログの該当時刻の `requestId`、`memberId`、`order_place_start`、`unhandled_exception` と例外スタックトレース
2. PostgreSQL の該当 DB における `orders` と `order_number_counters` の 2026/3/10 のレコード
3. 実際にアプリケーションが接続していた DB のホスト、DB 名、スキーマが初期化対象と同一か
4. 初期化スクリプトを実行した日時、`-SkipSeed` の有無、実行結果、および既存 DB を使用したか
5. 注文確定時の画面入力値。特に支払方法、会員情報の住所・電話番号、カート明細の在庫数
6. 同じ環境で再現した場合の、商品1件・ゲスト・会員の各組み合わせの結果

## 4. 原因を特定するための取得手順

### 4.1 アプリケーションログを確認する

障害発生時刻の前後 5 分を対象に、`log/office-order.log` と `log/archive/` のローテーション済みログを確認する。次のイベントを同じ `requestId` で追跡する。

- `event=order_place_start`
- `event=checkout_order_accepted` または `event=checkout_order_rejected`
- `event=unhandled_exception`
- `duplicate key value violates unique constraint`
- `orders_order_number_key`

`unhandled_exception` のスタックトレースに `DataIntegrityViolationException`、`DuplicateKeyException`、PostgreSQL の `23505` があれば、注文番号衝突を確定できる。`order_place_start` がなく `unhandled_exception` だけの場合は、注文確定処理より前の障害を別途切り分ける。

### 4.2 DB の注文番号衝突を確認する

アプリケーションと同じ接続先に対して、読み取り専用で次を実行する。パスワードはコマンドへ直接記載せず、`PGPASSWORD` など既存の認証方法を使用する。

```sql
SELECT order_number, order_datetime
FROM orders
WHERE order_number LIKE 'ORD20260310-%'
ORDER BY order_number
LIMIT 10;

SELECT order_date, last_sequence
FROM order_number_counters
WHERE order_date = DATE '2026-03-10';

SELECT to_regclass('public.orders') AS orders_table,
       to_regclass('public.order_number_counters') AS counter_table;
```

次の状態なら主原因候補を確定する。

- `orders` に `ORD20260310-000001` が存在する。
- `order_number_counters` に `2026-03-10` の行がない、または `last_sequence < 100` である。

なお、発生後に注文がロールバックされている場合でも、`order_number_counters` の採番更新は同一トランザクション内のため、最終状態だけでは採番前の値を直接確認できない。アプリログと PostgreSQL ログを優先する。

### 4.3 初期化実績と DB 差異を確認する

1. 初期化担当者から `scripts/init-local-postgres.ps1` の実行コマンド、実行時刻、標準出力、終了コードを取得する。
2. `-SkipSeed` を指定していないことを確認する。
3. スクリプトの `-DbHost`、`-Port`、`-Database`、`-Username` がアプリの [application.yml](../../src/main/resources/application.yml) と一致するか確認する。
4. 次の SQL でスキーマと seed の存在を確認する。

```sql
SELECT COUNT(*) AS orders_count FROM orders;
SELECT COUNT(*) AS member_count FROM members;
SELECT product_code, stock_quantity
FROM product_variants
WHERE product_code IN ('P0001-C01', 'P0001-C02')
ORDER BY product_code;
```

### 4.4 別候補を切り分ける

注文番号衝突が確認できない場合は、同じ初期 DB で次を一つずつ実施し、各試行の `requestId` とスタックトレースを保存する。

1. 会員 `member01@example.com`、商品1件で注文確定
2. 会員 `member01@example.com`、商品2件で注文確定
3. ゲスト、商品1件で注文確定
4. ゲスト、商品2件で注文確定

判定の目安は次のとおり。

- 全ケースで同じ `23505` が出る: 注文番号または別の一意制約を確認する。
- `order_place_start` 後に在庫不足ログが出る: seed 後の在庫値とカート数量を確認する。
- 注文ヘッダ INSERT 前に入力系例外が出る: セッションに保持されたフォームとデフォルト値を確認する。
- 注文保存後、コミット時または完了画面取得時に失敗する: トランザクションログ、メール送信設定、完了画面 SELECT の例外を確認する。

## 5. 影響範囲の推定

注文番号が同日 seed データと衝突する状態では、初期構築後の最初の注文から失敗する可能性がある。会員・ゲスト、商品1件・複数件にかかわらず影響し得る。ただし、既にカウンタが適切に同期された環境、seed 注文がない環境、または別日付の環境では同じ条件にならない可能性がある。

## 6. 結論

現時点の最有力原因は、初期 seed で投入された注文番号と、注文確定時に未同期の `order_number_counters` が生成する注文番号の衝突である。発生日時に対応する `orders` の注文番号、`order_number_counters` の状態、`unhandled_exception` の PostgreSQL エラーコードを取得すれば、実装変更なしで原因を確定できる。

本レポートでは障害原因の推定と調査手順のみを記載し、実装の修正は行っていない。