# CRUD図

本プロジェクトの機能（画面・バッチ）とデータベーステーブルの CRUD 関係を示します。

凡例: **C** = CREATE（INSERT）、**R** = READ（SELECT）、**U** = UPDATE、**D** = DELETE

---

## 1. 画面・機能一覧

| No. | 機能名 | 主なコントローラ / ジョブ |
|-----|--------|--------------------------|
| 1 | トップページ | `HomeController` |
| 2 | 商品一覧（カタログ） | `CatalogController` |
| 3 | 商品詳細 | `CatalogController` |
| 4 | カート | `CartController` |
| 5 | 注文情報入力・確認 | `CartController` |
| 6 | 注文完了 | `CartController` |
| 7 | ログイン・ログアウト | `AuthController` |
| 8 | 会員登録 | `MemberRegistrationController` |
| 9 | マイページ TOP | `MyPageController` |
| 10 | 会員情報編集 | `MyPageController` |
| 11 | 退会 | `MyPageController` |
| 12 | 追加お届け先一覧 | `MyPageController` |
| 13 | 追加お届け先 登録・編集・削除 | `MyPageController` |
| 14 | お気に入り一覧 | `MyPageController` |
| 15 | お気に入り 登録・削除 | `CatalogController` / `MyPageController` |
| 16 | 購入履歴一覧 | `MyPageController` |
| 17 | 購入履歴詳細 | `MyPageController` |
| 18 | お知らせ一覧 | `ContentController` |
| 19 | お問い合わせ | `ContactController` |
| 20 | バッチ：売れ筋ランキング集計 | `PopularRankingJob` |
| 21 | バッチ：おすすめ関連商品算出 | `RecommendedRelatedJob` |

---

## 2. CRUD マトリクス

### 商品・マスタ系テーブル

| 機能 | colors | products | product\_variants | product\_desk\_attributes | product\_chair\_attributes | product\_storage\_attributes | desk\_top\_shapes | chair\_functions | chair\_materials | storage\_usages | tastes |
|------|--------|----------|--------------------|---------------------------|----------------------------|------------------------------|-------------------|------------------|------------------|-----------------|--------|
| 1. トップページ | | R | R | | | | | | | | |
| 2. 商品一覧 | R | R | R | R | R | R | R | R | R | R | R |
| 3. 商品詳細 | R | R | R | R | R | R | | | | | |
| 4. カート | R | R | R | | | | | | | | |
| 5. 注文情報入力・確認 | | R | R | | | | | | | | |
| 6. 注文完了 | | | | | | | | | | | |
| 14. お気に入り一覧 | R | R | R | | | | | | | | |
| 15. お気に入り登録・削除 | | R | | | | | | | | | |
| 20. バッチ：売れ筋ランキング | | R | R | | | | | | | | |
| 21. バッチ：おすすめ関連商品 | | R | R | | | | | | | | |

### 会員系テーブル

| 機能 | members | member\_additional\_addresses | member\_favorites |
|------|---------|-------------------------------|-------------------|
| 7. ログイン・ログアウト | R | | |
| 8. 会員登録 | C | | |
| 9. マイページ TOP | R | | |
| 10. 会員情報編集 | R, U | | |
| 11. 退会 | U | | |
| 12. 追加お届け先一覧 | | R | |
| 13. 追加お届け先 登録・編集・削除 | | C, R, U, D | |
| 14. お気に入り一覧 | | | R |
| 15. お気に入り 登録・削除 | | | C, D |
| 16. 購入履歴一覧 | R | | |
| 17. 購入履歴詳細 | R | | |
| 19. お問い合わせ | R | | |

### 注文系テーブル

| 機能 | orders | order\_items | order\_status\_histories | order\_number\_counters |
|------|--------|--------------|--------------------------|-------------------------|
| 5. 注文情報入力・確認 | R | | | |
| 6. 注文完了 | C, R | C | C | C, U |
| 16. 購入履歴一覧 | R | | | |
| 17. 購入履歴詳細 | R | R | R | |

### コンテンツ・問い合わせ系テーブル

| 機能 | announcements | inquiries |
|------|---------------|-----------|
| 1. トップページ | R | |
| 18. お知らせ一覧 | R | |
| 19. お問い合わせ | | C |

### バッチ・分析系テーブル

| 機能 | popular\_product\_rankings | recommended\_related\_products | tax\_rates |
|------|---------------------------|-------------------------------|------------|
| 1. トップページ | R | | |
| 3. 商品詳細 | | R | |
| 4. カート | | | R |
| 5. 注文情報入力・確認 | | | R |
| 6. 注文完了 | | | R |
| 20. バッチ：売れ筋ランキング | C, D | | |
| 21. バッチ：おすすめ関連商品 | | C, D | |

---

## 3. テーブル別 CRUD サマリ

| テーブル名 | 論理名 | C | R | U | D | 操作機能 |
|-----------|--------|:-:|:-:|:-:|:-:|---------|
| `members` | 会員 | ○ | ○ | ○ | | 会員登録、ログイン、マイページ、情報編集、退会、購入履歴、お問い合わせ入力補完 |
| `member_additional_addresses` | 追加お届け先 | ○ | ○ | ○ | ○ | マイページ追加お届け先管理 |
| `member_favorites` | お気に入り | ○ | ○ | | ○ | 商品詳細/お気に入り一覧 |
| `products` | 商品 | | ○ | | | 商品一覧・詳細・カート・バッチ集計 |
| `product_variants` | 商品バリアント | | ○ | | | 商品一覧・詳細・カート・バッチ集計 |
| `colors` | カラーマスタ | | ○ | | | 商品一覧・詳細・カート |
| `product_desk_attributes` | デスク属性 | | ○ | | | 商品一覧・詳細 |
| `product_chair_attributes` | チェア属性 | | ○ | | | 商品一覧・詳細 |
| `product_storage_attributes` | 収納家具属性 | | ○ | | | 商品一覧・詳細 |
| `desk_top_shapes` | 天板形状マスタ | | ○ | | | 商品一覧（フィルタ） |
| `chair_functions` | チェア機能マスタ | | ○ | | | 商品一覧（フィルタ） |
| `chair_materials` | チェア素材マスタ | | ○ | | | 商品一覧（フィルタ） |
| `storage_usages` | 収納用途マスタ | | ○ | | | 商品一覧（フィルタ） |
| `tastes` | テイストマスタ | | ○ | | | 商品一覧（フィルタ） |
| `orders` | 注文 | ○ | ○ | | | 注文確定・完了・購入履歴 |
| `order_items` | 注文明細 | ○ | ○ | | | 注文確定・購入履歴詳細 |
| `order_status_histories` | 注文ステータス履歴 | ○ | ○ | | | 注文確定・購入履歴詳細 |
| `order_number_counters` | 注文番号採番 | ○ | | ○ | | 注文番号採番（UPSERT） |
| `tax_rates` | 消費税率 | | ○ | | | カート・注文処理 |
| `popular_product_rankings` | 売れ筋ランキング | ○ | ○ | | ○ | トップページ表示・バッチ集計（全置換） |
| `recommended_related_products` | おすすめ関連商品 | ○ | ○ | | ○ | 商品詳細表示・バッチ算出（全置換） |
| `announcements` | お知らせ | | ○ | | | トップページ・お知らせ一覧 |
| `inquiries` | お問い合わせ | ○ | | | | お問い合わせフォーム送信 |

---

## 4. 補足

- **カートはDB非永続**: カート情報はCookieに保持する（`CartCookieStore`）。`cart`テーブルは存在せず、DBへのアクセスはカート表示時の商品スナップショット取得（`product_variants`, `products`, `colors`, `tax_rates` のREAD）のみ。
- **注文確定時のスナップショット**: `order_items` へは注文確定時点の商品名・カラー名・単価を直接書き込む。`products`/`product_variants` への FK は持たない設計のため、注文後に商品情報が変わっても履歴は影響を受けない。
- **`order_number_counters` の UPSERT**: 注文確定時に当日の行がなければINSERT、あればUPDATEする（採番カウンタ）。
- **バッチの全置換**: `popular_product_rankings` と `recommended_related_products` は、バッチ実行のたびに対象日の行を DELETE→INSERT する全置換方式。
- **マスタテーブル（`colors`, `desk_top_shapes` 等）**: アプリ画面からのCRUD操作は行わず、シードデータ投入（SQL）のみで管理する。
