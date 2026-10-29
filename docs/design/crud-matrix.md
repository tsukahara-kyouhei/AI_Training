# CRUD 図

本表は、実装済みの Service／Repository／MyBatis Mapper と `sql/schema` のDDLをもとに、ユースケース単位でデータベース操作を整理したものです。

## 凡例

| 記号 | 意味 |
|---|---|
| C | Create：登録・追加 |
| R | Read：参照・検索・集計 |
| U | Update：更新 |
| D | Delete：削除 |
| - | 該当操作なし |

複数の記号は、同じユースケース内で複数の操作を行うことを表します。`R` は一覧表示だけでなく、存在確認、入力補完、集計元データの取得も含みます。

## 商品・検索

| 機能／ユースケース | products | product_variants | colors | 商品属性・カテゴリマスタ | popular_product_rankings | recommended_related_products | tax_rates |
|---|---|---|---|---|---|---|---|
| トップ表示（新着・ランキング） | R | R | R | - | R | - | - |
| 商品検索・新着一覧 | R | R | R | R | - | - | - |
| カテゴリ一覧 | R | R | R | R | - | - | - |
| 商品詳細表示 | R | R | R | R | - | R | R |
| 最近見た商品取得 | R | R | R | - | - | - | - |
| 絞り込み候補取得 | - | - | R | R | - | - | - |

## 会員・お気に入り

| 機能／ユースケース | members | member_additional_addresses | member_favorites | products | product_variants | colors |
|---|---|---|---|---|---|---|
| ログイン認証・セッション会員確認 | R | - | - | - | - | - |
| 会員登録 | C | - | - | - | - | - |
| 会員情報表示・メール重複確認 | R | - | - | - | - | - |
| 会員情報変更 | R/U | - | - | - | - | - |
| 退会 | U | - | - | - | - | - |
| 追加お届け先一覧・詳細 | - | R | - | - | - | - |
| 追加お届け先登録 | - | C | - | - | - | - |
| 追加お届け先変更 | - | R/U | - | - | - | - |
| 追加お届け先削除 | - | D | - | - | - | - |
| お気に入り一覧・登録状態確認 | R | - | R | R | R | R |
| お気に入り追加 | - | - | C | - | - | - |
| お気に入り解除 | - | - | D | - | - | - |

## カート・購入・注文履歴

| 機能／ユースケース | members | products | product_variants | colors | tax_rates | order_number_counters | orders | order_items | order_status_histories |
|---|---|---|---|---|---|---|---|---|---|
| カート表示・カート操作 | - | R | R | R | R | - | - | - | - |
| 注文入力の会員情報補完 | R | - | - | - | - | - | - | - | - |
| 注文確定・注文番号採番 | - | - | R | - | R | C/U | C | C | C |
| 注文完了表示 | - | - | - | - | - | - | R | - | - |
| 会員購入履歴一覧 | - | - | - | - | - | - | R | - | - |
| 会員購入履歴詳細 | - | - | - | - | - | - | R | R | R |
| 購入履歴から再購入 | - | - | R | - | - | - | R | R | - |

## お問い合わせ・コンテンツ

| 機能／ユースケース | members | inquiries | announcements |
|---|---|---|---|
| お問い合わせ入力の会員情報補完 | R | - | - |
| お問い合わせ送信 | R | C | - |
| お知らせ一覧表示 | - | - | R |

## 集計・推薦バッチ

| 機能／ユースケース | orders | order_items | product_variants | products | popular_product_rankings | recommended_related_products |
|---|---|---|---|---|---|---|
| 売れ筋ランキング候補集計 | R | R | R | R | - |
| 売れ筋ランキング再生成 | R | R | R | R | C/R/D | - |
| おすすめ関連商品候補集計 | R | R | R | - | - |
| おすすめ関連商品再生成 | R | R | R | - | - | C/R/D |

## Spring Batch（システム）

| 機能／ユースケース | batch_job_instance | batch_job_execution | batch_job_execution_params | batch_step_execution | batch_step_execution_context | batch_job_execution_context |
|---|---|---|---|---|---|---|
| ジョブ実行管理 | C/R/U | C/R/U | C/R | C/R/U | C/R/U | C/R/U |
| ジョブ実行履歴・状態参照 | R | R | R | R | R | R |

## テーブル別 CRUD 対応一覧

機能別表の対象を横断して確認できるよう、テーブルごとのCRUD有無をまとめます。

| テーブル | C | R | U | D | 主な利用機能 |
|---|---|---|---|---|---|
| products | - | あり | - | - | 商品検索・一覧・詳細 |
| product_variants | - | あり | - | - | 商品検索・在庫再確認・カート・再購入 |
| colors | - | あり | - | - | 商品表示・カラー候補 |
| product_desk_attributes | - | あり | - | - | デスク検索・詳細 |
| product_chair_attributes | - | あり | - | - | チェア検索・詳細 |
| product_storage_attributes | - | あり | - | - | 収納家具検索・詳細 |
| desk_top_shapes / desk_tastes | - | あり | - | - | デスク絞り込み候補・検索 |
| chair_functions / chair_materials / chair_tastes | - | あり | - | - | チェア絞り込み候補・検索 |
| storage_usages / storage_tastes | - | あり | - | - | 収納家具絞り込み候補・検索 |
| members | あり | あり | あり | - | 認証・会員登録・プロフィール・退会 |
| member_additional_addresses | あり | あり | あり | あり | 追加お届け先管理 |
| member_favorites | あり | あり | - | あり | お気に入り管理 |
| orders | あり | あり | - | - | 注文確定・完了・購入履歴 |
| order_number_counters | あり | - | あり | - | 注文番号の日次採番（UPSERT） |
| order_items | あり | あり | - | - | 注文明細・購入履歴・バッチ集計 |
| order_status_histories | あり | あり | - | - | 注文ステータス履歴 |
| announcements | - | あり | - | - | お知らせ表示 |
| inquiries | あり | - | - | - | お問い合わせ送信 |
| tax_rates | - | あり | - | - | 税率参照 |
| popular_product_rankings | あり | - | - | あり | 売れ筋ランキング再生成 |
| recommended_related_products | あり | - | - | あり | おすすめ関連商品再生成 |
| batch_job_instance | あり | あり | あり | - | Spring Batchジョブ管理 |
| batch_job_execution | あり | あり | あり | - | Spring Batch実行管理 |
| batch_job_execution_params | あり | あり | - | - | ジョブパラメータ |
| batch_step_execution | あり | あり | あり | - | Step実行管理 |
| batch_step_execution_context | あり | あり | あり | - | Step実行コンテキスト |
| batch_job_execution_context | あり | あり | あり | - | Job実行コンテキスト |

## 補足

- 商品、マスタ、税率、既存のお知らせは、現行アプリケーションの画面・サービスでは参照処理が中心です。登録・更新・削除の管理機能は本プロジェクトの実装範囲に含まれていません。
- カートはセッション／Cookieで管理されるため、カート自体のDB CRUDはありません。カート表示時には商品・バリアント・カラー・税率を参照します。
- 注文確定では、注文番号カウンタに対して「未登録ならINSERT、登録済みなら連番UPDATE」を行います。`orders`、`order_items`、`order_status_histories` は登録されます。
- 注文確定時の `product_variants` は在庫数を再確認するために参照されます。現行のWebフロント・バッチ実装には、購入による `stock_quantity` の引き落とし更新（U）は存在しません。したがって本表では、注文確定行の `product_variants` を `R` としています。
- ランキングとおすすめ関連商品は、再生成対象を算出するため注文・明細・商品関連データを参照した後、対象日データを削除して再登録する全置換です。そのため機能別表では `C/R/D` と記載しています。
- `order_items.product_code`、`inquiries.product_code`、`inquiries.product_name` は保持用の値であり、DDL上の外部キーではありません。
- `tax_rates` は注文と直接関連付けず、注文処理・商品表示・バッチ処理の時点で有効なレコードを参照します。
- 注文ステータスの変更（発送・キャンセル等）や、商品・会員データの管理者向け登録・更新・削除機能は、現行のユースケース（Webフロント・バッチ）に含まれていないため記載していません。
- Spring Batchメタデータはフレームワークが実行時に管理します。通常業務の削除処理は実装上確認できないため、Dは記載していません。
- SQLスキーマに存在する `batch_job_execution_seq`、`batch_job_seq`、`batch_step_execution_seq` はテーブルではなくシーケンスのため、本表の対象外です。
