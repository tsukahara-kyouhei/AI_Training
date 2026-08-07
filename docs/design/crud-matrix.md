# CRUD マトリクス

以下は画面・機能と主要テーブル間の CRUD 関係を示したマトリクスです。
行：画面／機能、列：主要テーブル。交差セルに C（作成）、R（参照）、U（更新）、D（削除）を表記しています。

| 画面/機能                      | products | product_variants | colors | product_desk_attributes | product_chair_attributes | product_storage_attributes | members | member_additional_addresses | member_favorites | orders | order_items | order_status_histories |
| ------------------------------ | :------: | :--------------: | :----: | :---------------------: | :----------------------: | :------------------------: | :-----: | :-------------------------: | :--------------: | :----: | :---------: | :--------------------: |
| Home (トップ)                  |    R     |        R         |   R    |                         |                          |                            |         |                             |                  |        |             |                        |
| 商品検索 (/products/search)    |    R     |        R         |   R    |            R            |            R             |             R              |         |                             |                  |        |             |                        |
| 商品詳細 (/products/{code})    |    R     |        R         |   R    |            R            |            R             |             R              |         |                             |                  |        |             |                        |
| カートに追加 (UI)              |          |                  |        |                         |                          |                            |         |                             |                  |        |             |                        |
| カート画面 (/cart)             |    R     |        R         |        |                         |                          |                            |         |                             |                  |        |             |                        |
| 注文入力 (/checkout/input)     |    R     |        R         |        |                         |                          |                            |    R    |              R              |                  |        |             |                        |
| 注文確認 (/checkout/confirm)   |    R     |        R         |        |                         |                          |                            |    R    |              R              |                  |        |             |                        |
| 注文確定（注文作成）           |          |        U         |        |                         |                          |                            |         |                             |                  |   C    |      C      |           C            |
| 注文完了 (/checkout/complete)  |    R     |        R         |        |                         |                          |                            |    R    |                             |                  |   R    |      R      |           R            |
| 注文履歴 (/mypage/orders)      |          |                  |        |                         |                          |                            |    R    |                             |                  |   R    |      R      |           R            |
| 注文詳細 (/mypage/orders/{id}) |          |                  |        |                         |                          |                            |    R    |                             |                  |   R    |      R      |           R            |
| ログイン (/login)              |          |                  |        |                         |                          |                            |    R    |                             |                  |        |             |                        |
| 会員登録 (/member/register)    |          |                  |        |                         |                          |                            |    C    |                             |                  |        |             |                        |
| 会員情報編集 (マイページ)      |          |                  |        |                         |                          |                            |    U    |            C/U/D            |                  |        |             |                        |
| お気に入り追加/削除            |          |                  |        |                         |                          |                            |         |                             |      C / D       |        |             |                        |
| お知らせ表示                   |    R     |                  |        |                         |                          |                            |         |                             |                  |        |             |                        |
| 問い合わせ送信                 |          |                  |        |                         |                          |                            |         |                             |                  |        |             |                        |

> 注記:
>
> - `order_items` は注文確定時に `orders` と併せて作成される（C）。
> - 在庫更新は `product_variants` の `stock_quantity` を更新するため、注文作成時に U が発生する可能性があります（簡潔化のため表には記載せず、\"注文確定\" 行に U を示していません）。
> - カートは本スキーマに永続テーブルが定義されていないため、セッションやクライアント側に保持される想定で DB 操作はなしとしています。
> - 管理者向け商品マスタ編集画面等が存在する場合は `products` / `product_*_attributes` に対して C/U/D 操作が発生します（管理画面は本表に含めていません）。

ファイル: [docs/design/crud-matrix.md](docs/design/crud-matrix.md)
