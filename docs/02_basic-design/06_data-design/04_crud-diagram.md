# CRUD図

機能（行）×テーブル（列）の操作マトリクス。  
凡例: **C** = INSERT / **R** = SELECT / **U** = UPDATE / **D** = DELETE / `-` = 操作なし

---

## 1. 会員・認証系テーブル

| 機能 | members | member_additional_addresses | member_favorites |
|---|:---:|:---:|:---:|
| F-REG: 会員登録入力・確認 | C | - | - |
| F-AUTH: ログイン | R | - | - |
| F-MY: プロフィール編集 | R, U | - | - |
| F-MY: 追加お届け先管理 | - | C, R, U, D | - |
| F-MY: お気に入り一覧 | - | - | R |
| F-MY: 退会 | U | - | D |
| F-CAT: お気に入り登録/解除 | - | - | C, D |

---

## 2. 商品系テーブル

| 機能 | products | product_variants | colors | product_desk_attributes | product_chair_attributes | product_storage_attributes |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| F-CAT: 商品一覧・検索 | R | R | R | R | R | R |
| F-CAT: 商品詳細 | R | R | R | R | R | R |
| F-CAT: お気に入り登録/解除 | R | - | - | - | - | - |
| F-CRT: カート追加 | R | R | - | - | - | - |
| F-CO: チェックアウト入力 | R | R | - | - | - | - |
| F-CO: 注文確定 | - | U | - | - | - | - |
| F-MY: お気に入り一覧 | R | R | R | - | - | - |
| F-MY: 再購入 | R | R | - | - | - | - |

> **注意:** `product_variants.stock_quantity` は注文確定時に UPDATE（在庫接減）。
> **補足:** 「F-CAT: 商品一覧・検索」のRには、検索結果画面（`/products/search`）でのテイスト絞込み時に3カテゴリの属性テーブルをカテゴリ横断で参照するケースを含む。

---

## 3. カート・注文系テーブル

| 機能 | shopping_cart | cart_lines | orders | order_items | order_number_counters | order_status_histories | tax_rates |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| F-CRT: カート表示 | R | R | - | - | - | - | R |
| F-CRT: 商品をカートに追加 | C, R | C, R | - | - | - | - | - |
| F-CRT: 数量変更 | - | U | - | - | - | - | - |
| F-CRT: 商品削除 | - | D | - | - | - | - | - |
| F-CRT: カート全削除 | - | D | - | - | - | - | - |
| F-CO: 支払方法選択 | R | R | - | - | - | - | R |
| F-CO: 注文情報入力 | R | R | - | - | - | - | R |
| F-CO: 注文確認 | R | R | - | - | - | - | R |
| F-CO: 注文確定 | D | D | C | C | R, U | C | R |
| F-MY: 注文履歴一覧 | - | - | R | - | - | - | - |
| F-MY: 注文詳細 | - | - | R | R | - | R | - |
| F-MY: 再購入 | C, R | C, R | - | - | - | - | - |

---

## 4. コンテンツ・お問い合わせ系テーブル

| 機能 | announcements | inquiries |
|---|:---:|:---:|
| F-ANN: お知らせ一覧 | R | - |
| F-ANN: お知らせ詳細 | R | - |
| F-CON: お問い合わせ入力・確認 | - | - |
| F-CON: お問い合わせ送信 | - | C |

---

## 5. マスタ・バッチ系テーブル

| 機能 | desk_top_shapes | desk_tastes | chair_functions | chair_materials | chair_tastes | storage_usages | storage_tastes | popular_product_rankings | recommended_related_products |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| F-CAT: 商品フィルタ | R | R | R | R | R | R | R | - | - |
| F-CAT: 商品詳細（属性表示） | R | R | R | R | R | R | R | - | - |
| F-CAT: 売れ筋ランキング表示 | - | - | - | - | - | - | - | R | - |
| F-CAT: 関連商品表示 | - | - | - | - | - | - | - | - | R |
| F-BAT: ランキング計算ジョブ | - | - | - | - | - | - | - | C, U, D | - |
| F-BAT: レコメンド計算ジョブ | - | - | - | - | - | - | - | - | C, U, D |

> **マスタテーブル:** アプリケーションからは参照のみ。登録・更新は本システムの管理画面外（直接DB操作）で行う。
> **補足:** 「F-CAT: 商品フィルタ」のRには、検索結果画面（`/products/search`）でテイストマスタ（`desk_tastes`, `chair_tastes`, `storage_tastes`）を3カテゴリ分まとめて参照するケースを含む。

---

## 6. まとめ（テーブル操作集計）

| テーブル | C | R | U | D | 主な操作機能 |
|---|:---:|:---:|:---:|:---:|---|
| members | F-REG | F-AUTH, F-MY | F-MY, F-AUTH | - | 会員登録・プロフィール編集・退会 |
| member_additional_addresses | F-MY | F-MY, F-CO | F-MY | F-MY | お届け先管理 |
| member_favorites | F-CAT | F-MY | - | F-CAT, F-MY | お気に入り登録/解除 |
| products | - | F-CAT, F-CRT, F-CO, F-MY | - | - | 商品表示全般（参照のみ） |
| product_variants | - | F-CAT, F-CRT, F-CO, F-MY | F-CO | - | 在庫管理（注文確定時に接減） |
| colors | - | F-CAT | - | - | 商品表示（参照のみ） |
| shopping_cart | F-CRT | F-CRT, F-CO | - | F-CO | カート管理 |
| cart_lines | F-CRT, F-MY | F-CRT, F-CO | F-CRT | F-CRT, F-CO | カートライン管理 |
| orders | F-CO | F-CO, F-MY | - | - | 注文登録・履歴参照 |
| order_items | F-CO | F-MY | - | - | 注文明細登録・参照 |
| order_number_counters | - | F-CO | F-CO | - | 注文番号採番 |
| order_status_histories | F-CO | F-MY | - | - | ステータス変更履歴 |
| tax_rates | - | F-CRT, F-CO | - | - | 税率参照（注文確定時） |
| announcements | - | F-ANN | - | - | お知らせ表示（参照のみ） |
| inquiries | F-CON | - | - | - | 問い合わせ受付 |
| popular_product_rankings | F-BAT | F-CAT | F-BAT | F-BAT | バッチで更新・カタログで参照 |
| recommended_related_products | F-BAT | F-CAT | F-BAT | F-BAT | バッチで更新・カタログで参照 |
| desk_top_shapes ほか6マスタ | - | F-CAT | - | - | 商品属性フィルタ（参照のみ） |
