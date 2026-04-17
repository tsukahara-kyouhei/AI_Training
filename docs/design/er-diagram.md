# ER 図

最終更新: 2026-03-10

---

## テーブル一覧

| グループ | テーブル名 | 論理名 |
|---------|-----------|--------|
| 会員 | members | 会員 |
| 会員 | member_additional_addresses | 会員追加お届け先 |
| 会員 | member_favorites | 会員お気に入り |
| 商品 | products | 商品 |
| 商品 | product_variants | 商品バリアント |
| 商品 | colors | カラーマスタ |
| 商品 | product_desk_attributes | 商品デスク属性 |
| 商品 | product_chair_attributes | 商品チェア属性 |
| 商品 | product_storage_attributes | 商品収納家具属性 |
| 商品マスタ | desk_top_shapes | デスク天板形状マスタ |
| 商品マスタ | desk_tastes | デスクテイストマスタ |
| 商品マスタ | chair_functions | チェア機能マスタ |
| 商品マスタ | chair_materials | チェア素材マスタ |
| 商品マスタ | chair_tastes | チェアテイストマスタ |
| 商品マスタ | storage_usages | 収納家具用途マスタ |
| 商品マスタ | storage_tastes | 収納家具テイストマスタ |
| 注文 | orders | 注文 |
| 注文 | order_items | 注文明細 |
| 注文 | order_status_histories | 注文ステータス履歴 |
| 注文 | order_number_counters | 注文番号採番カウンタ |
| コンテンツ | announcements | お知らせ |
| コンテンツ | inquiries | お問い合わせ |
| バッチ・システム | tax_rates | 消費税率 |
| バッチ・システム | popular_product_rankings | 売れ筋ランキング |
| バッチ・システム | recommended_related_products | おすすめ関連商品 |
| Spring Batch メタデータ | batch_job_instance | ジョブインスタンス |
| Spring Batch メタデータ | batch_job_execution | ジョブ実行 |
| Spring Batch メタデータ | batch_job_execution_params | ジョブ実行パラメータ |
| Spring Batch メタデータ | batch_step_execution | ステップ実行 |
| Spring Batch メタデータ | batch_step_execution_context | ステップ実行コンテキスト |
| Spring Batch メタデータ | batch_job_execution_context | ジョブ実行コンテキスト |

---

## 1. 全体リレーション概要図

テーブル間のリレーションを一覧化した簡略図です（カラム定義は省略）。

```mermaid
erDiagram
    members ||--o{ member_additional_addresses : "追加お届け先"
    members ||--o{ member_favorites : "お気に入り"
    members |o--o{ orders : "会員注文（ゲストはNULL）"
    members |o--o{ inquiries : "お問い合わせ（未ログインはNULL）"

    products ||--o{ member_favorites : "お気に入りされる"
    products ||--o{ product_variants : "バリアント"
    products ||--o| product_desk_attributes : "デスク属性(1対1)"
    products ||--o| product_chair_attributes : "チェア属性(1対1)"
    products ||--o| product_storage_attributes : "収納家具属性(1対1)"
    products ||--o{ popular_product_rankings : "ランキング"
    products ||--o{ recommended_related_products : "基準商品"
    products ||--o{ recommended_related_products : "推薦商品"

    colors ||--o{ product_variants : "カラー"

    desk_top_shapes ||--o{ product_desk_attributes : "天板形状"
    desk_tastes ||--o{ product_desk_attributes : "テイスト"
    chair_functions ||--o{ product_chair_attributes : "機能"
    chair_materials ||--o{ product_chair_attributes : "素材"
    chair_tastes ||--o{ product_chair_attributes : "テイスト"
    storage_usages ||--o{ product_storage_attributes : "用途"
    storage_tastes ||--o{ product_storage_attributes : "テイスト"

    orders ||--|{ order_items : "注文明細"
    orders ||--o{ order_status_histories : "ステータス履歴"

    batch_job_instance ||--o{ batch_job_execution : "実行"
    batch_job_execution ||--o{ batch_job_execution_params : "パラメータ"
    batch_job_execution ||--o{ batch_step_execution : "ステップ実行"
    batch_step_execution ||--o| batch_step_execution_context : "コンテキスト"
    batch_job_execution ||--o| batch_job_execution_context : "コンテキスト"
```

---

## 2. 業務テーブル詳細 ER 図

### 2-1. 会員ドメイン

```mermaid
erDiagram
    members {
        bigint member_id PK "会員ID"
        varchar personal_or_corporate "個人/法人区分"
        varchar last_name "姓"
        varchar first_name "名"
        varchar last_name_kana "姓カナ"
        varchar first_name_kana "名カナ"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar email "メールアドレス"
        varchar gender "性別"
        date anniversary_date "生年月日/記念日"
        varchar password_hash "パスワードハッシュ"
        boolean newsletter_opt_in "メールマガジン希望"
        char postal_code "郵便番号"
        varchar prefecture "都道府県"
        varchar city "市区町村"
        varchar address_line "番地・ビル名"
        integer delivery_floor "お届け先階数"
        boolean has_elevator "エレベーター有無"
        varchar daytime_phone "日中電話番号"
        varchar fax "FAX"
        varchar member_status "会員状態"
        timestamptz withdrawn_at "退会日時"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    member_additional_addresses {
        bigint member_address_id PK "会員追加お届け先ID"
        bigint member_id FK "会員ID"
        varchar last_name "姓"
        varchar first_name "名"
        varchar last_name_kana "姓カナ"
        varchar first_name_kana "名カナ"
        varchar company_name "会社名"
        varchar department_name "部署名"
        char postal_code "郵便番号"
        varchar prefecture "都道府県"
        varchar city "市区町村"
        varchar address_line "番地・ビル名"
        integer delivery_floor "お届け先階数"
        boolean has_elevator "エレベーター有無"
        varchar daytime_phone "日中電話番号"
        varchar fax "FAX"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    member_favorites {
        bigint member_id PK "会員ID（複合PK）"
        bigint product_id PK "商品ID（複合PK）"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    products {
        bigint product_id PK "商品ID（参照のみ）"
    }

    members ||--o{ member_additional_addresses : "追加お届け先を持つ"
    members ||--o{ member_favorites : "お気に入りに追加する"
    products ||--o{ member_favorites : "お気に入りされる"
```

### 2-2. 商品ドメイン

```mermaid
erDiagram
    products {
        bigint product_id PK "商品ID"
        varchar product_name "商品名"
        varchar category_id "商品カテゴリID (desk/chair/storage)"
        text description "商品紹介文"
        boolean assembly_available "組立・設置可否"
        numeric assembly_fee "組立・設置費"
        boolean has_variation "シリーズバリエーション有無"
        bigint variation_group_id "バリエーショングループID"
        varchar variation_name "バリエーション名"
        timestamptz sale_start_at "販売開始日時"
        timestamptz sale_end_at "販売終了日時"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    colors {
        bigint color_id PK "カラーID"
        varchar color_name "カラー名"
        char color_code "カラーコード (#RRGGBB)"
        varchar swatch_type "色見本表示種別"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    product_variants {
        bigint product_variant_id PK "商品バリアントID"
        bigint product_id FK "商品ID"
        varchar product_code "商品コード"
        bigint color_id FK "カラーID"
        numeric unit_price "販売価格"
        integer stock_quantity "在庫数"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    product_desk_attributes {
        bigint product_id PK "商品ID（1対1）"
        bigint top_shape_id FK "天板形状ID"
        integer width_mm "幅(mm)"
        integer depth_mm "奥行(mm)"
        integer height_mm "高さ(mm)"
        bigint taste_id FK "テイストID"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    product_chair_attributes {
        bigint product_id PK "商品ID（1対1）"
        bigint function_id FK "機能ID"
        bigint material_id FK "素材ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    product_storage_attributes {
        bigint product_id PK "商品ID（1対1）"
        bigint usage_id FK "用途ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    desk_top_shapes {
        bigint top_shape_id PK "天板形状ID"
        varchar display_name "天板形状名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    desk_tastes {
        bigint taste_id PK "テイストID"
        varchar display_name "テイスト名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    chair_functions {
        bigint function_id PK "機能ID"
        varchar display_name "機能名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    chair_materials {
        bigint material_id PK "素材ID"
        varchar display_name "素材名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    chair_tastes {
        bigint taste_id PK "テイストID"
        varchar display_name "テイスト名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    storage_usages {
        bigint usage_id PK "用途ID"
        varchar display_name "用途名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    storage_tastes {
        bigint taste_id PK "テイストID"
        varchar display_name "テイスト名"
        integer sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    products ||--o{ product_variants : "バリアントを持つ"
    colors ||--o{ product_variants : "カラーが使われる"
    products ||--o| product_desk_attributes : "デスク属性（1対1）"
    products ||--o| product_chair_attributes : "チェア属性（1対1）"
    products ||--o| product_storage_attributes : "収納家具属性（1対1）"
    desk_top_shapes ||--o{ product_desk_attributes : "天板形状"
    desk_tastes ||--o{ product_desk_attributes : "テイスト"
    chair_functions ||--o{ product_chair_attributes : "機能"
    chair_materials ||--o{ product_chair_attributes : "素材"
    chair_tastes ||--o{ product_chair_attributes : "テイスト"
    storage_usages ||--o{ product_storage_attributes : "用途"
    storage_tastes ||--o{ product_storage_attributes : "テイスト"
```

### 2-3. 注文ドメイン

```mermaid
erDiagram
    members {
        bigint member_id PK "会員ID（参照のみ）"
    }

    orders {
        bigint order_id PK "注文ID"
        varchar order_number "注文番号"
        timestamptz order_datetime "注文日時"
        varchar order_status "注文ステータス"
        varchar customer_type "顧客種別 (guest/member)"
        varchar personal_or_corporate "個人/法人区分"
        bigint member_id FK "会員ID（ゲスト注文はNULL）"
        varchar customer_last_name "注文者姓"
        varchar customer_first_name "注文者名"
        varchar customer_last_name_kana "注文者姓カナ"
        varchar customer_first_name_kana "注文者名カナ"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar customer_email "注文者メールアドレス"
        varchar daytime_phone "日中電話番号"
        varchar shipping_fax "配送先FAX"
        char shipping_postal_code "配送先郵便番号"
        varchar shipping_prefecture "配送先都道府県"
        varchar shipping_city "配送先市区町村"
        varchar shipping_address_line "配送先番地・ビル名"
        integer shipping_floor "配送先階数"
        boolean shipping_has_elevator "配送先エレベーター有無"
        varchar payment_method "決済手段"
        jsonb payment_instruction "決済案内情報（コンビニ決済時）"
        varchar shipping_method "配送方法区分 (normal/assembly)"
        numeric shipping_fee "送料"
        numeric assembly_fee_total "組立・設置費合計"
        numeric subtotal_amount "商品小計"
        numeric tax_amount "消費税額"
        numeric total_amount "合計金額"
        boolean receipt_issued "領収書発行フラグ"
        varchar note "備考"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    order_number_counters {
        date order_date PK "注文日"
        integer last_sequence "最終連番"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    order_items {
        bigint order_item_id PK "注文明細ID"
        bigint order_id FK "注文ID"
        varchar product_code "商品コード（スナップショット）"
        varchar product_name "商品名（スナップショット）"
        varchar color_name "カラー名（スナップショット）"
        numeric unit_price "単価"
        integer quantity "数量"
        numeric line_subtotal "明細小計"
        numeric line_tax_amount "明細消費税額"
        numeric line_total_amount "明細合計金額"
        boolean assembly_available "組立・設置可否"
        numeric assembly_fee "組立・設置費"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    order_status_histories {
        bigint order_status_history_id PK "注文ステータス履歴ID"
        bigint order_id FK "注文ID"
        varchar status "ステータス"
        varchar changed_by_system "変更元システム"
        timestamptz changed_at "変更日時"
    }

    members |o--o{ orders : "会員が注文（ゲストはNULL）"
    orders ||--|{ order_items : "注文明細を持つ"
    orders ||--o{ order_status_histories : "ステータス履歴を持つ"
```

### 2-4. コンテンツ・バッチドメイン

```mermaid
erDiagram
    announcements {
        bigint announcement_id PK "お知らせID"
        varchar title "タイトル"
        varchar summary "要約"
        text body "本文"
        timestamptz published_start_at "掲載開始日時"
        timestamptz published_end_at "掲載終了日時"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    members {
        bigint member_id PK "会員ID（参照のみ）"
    }

    inquiries {
        bigint inquiry_id PK "お問い合わせID"
        bigint member_id FK "会員ID（未ログイン時はNULL）"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar last_name "姓"
        varchar first_name "名"
        varchar email "メールアドレス"
        varchar phone "電話番号"
        varchar inquiry_type "お問い合わせ種別"
        varchar order_phase "注文状況 (before_order/after_order)"
        varchar product_name "お問い合わせ商品名"
        varchar product_code "商品コード"
        varchar message "お問い合わせ内容"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    tax_rates {
        bigint tax_rate_id PK "消費税率ID"
        numeric tax_rate_percent "消費税率(%)"
        timestamptz effective_start_at "適用開始日時"
        timestamptz effective_end_at "適用終了日時"
        boolean is_active "有効フラグ"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    products {
        bigint product_id PK "商品ID（参照のみ）"
    }

    popular_product_rankings {
        date ranking_date PK "集計日（複合PK）"
        smallint rank PK "順位（複合PK）"
        bigint product_id FK "商品ID"
        integer sold_quantity_1m "直近1か月販売数量"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    recommended_related_products {
        date recommendation_date PK "推薦日（複合PK）"
        bigint source_product_id PK "基準商品ID（複合PK）"
        smallint rank PK "順位（複合PK）"
        bigint recommended_product_id FK "推薦商品ID"
        numeric score "類似度スコア"
        timestamptz created_at "作成日時"
        timestamptz updated_at "更新日時"
    }

    members |o--o{ inquiries : "お問い合わせ（未ログインはNULL）"
    products ||--o{ popular_product_rankings : "ランキングに登場する"
    products ||--o{ recommended_related_products : "基準商品として使われる"
    products ||--o{ recommended_related_products : "推薦商品として使われる"
```

---

## 3. Spring Batch メタデータ ER 図

Spring Framework が Job / Step 実行状態の管理に使用するシステムテーブルです。

```mermaid
erDiagram
    batch_job_instance {
        bigint job_instance_id PK "ジョブインスタンスID"
        bigint version "バージョン"
        varchar job_name "ジョブ名"
        varchar job_key "ジョブキー"
    }

    batch_job_execution {
        bigint job_execution_id PK "ジョブ実行ID"
        bigint version "バージョン"
        bigint job_instance_id FK "ジョブインスタンスID"
        timestamp create_time "作成日時"
        timestamp start_time "開始日時"
        timestamp end_time "終了日時"
        varchar status "ステータス"
        varchar exit_code "終了コード"
        varchar exit_message "終了メッセージ"
        timestamp last_updated "最終更新日時"
    }

    batch_job_execution_params {
        bigint job_execution_id FK "ジョブ実行ID"
        varchar parameter_name "パラメータ名"
        varchar parameter_type "パラメータ型"
        varchar parameter_value "パラメータ値"
        char identifying "識別フラグ"
    }

    batch_step_execution {
        bigint step_execution_id PK "ステップ実行ID"
        bigint version "バージョン"
        varchar step_name "ステップ名"
        bigint job_execution_id FK "ジョブ実行ID"
        timestamp create_time "作成日時"
        timestamp start_time "開始日時"
        timestamp end_time "終了日時"
        varchar status "ステータス"
        bigint commit_count "コミット数"
        bigint read_count "読取件数"
        bigint filter_count "フィルタ件数"
        bigint write_count "書込件数"
        bigint read_skip_count "読取スキップ件数"
        bigint write_skip_count "書込スキップ件数"
        bigint process_skip_count "処理スキップ件数"
        bigint rollback_count "ロールバック数"
        varchar exit_code "終了コード"
        varchar exit_message "終了メッセージ"
        timestamp last_updated "最終更新日時"
    }

    batch_step_execution_context {
        bigint step_execution_id PK "ステップ実行ID"
        varchar short_context "短縮コンテキスト"
        text serialized_context "シリアライズ済みコンテキスト"
    }

    batch_job_execution_context {
        bigint job_execution_id PK "ジョブ実行ID"
        varchar short_context "短縮コンテキスト"
        text serialized_context "シリアライズ済みコンテキスト"
    }

    batch_job_instance ||--o{ batch_job_execution : "実行される"
    batch_job_execution ||--o{ batch_job_execution_params : "パラメータを持つ"
    batch_job_execution ||--o{ batch_step_execution : "ステップ実行を持つ"
    batch_step_execution ||--o| batch_step_execution_context : "コンテキストを持つ"
    batch_job_execution ||--o| batch_job_execution_context : "コンテキストを持つ"
```

---

## 備考

| 項目 | 内容 |
|------|------|
| DB | PostgreSQL |
| 主キー | 基本的に `BIGINT GENERATED ALWAYS AS IDENTITY`（自動採番）。複合PKはその都度記載 |
| 外部キー | ON DELETE CASCADE（子テーブル連鎖削除）または ON DELETE SET NULL（NULLクリア）を使い分け |
| 注文明細 | 注文時点の商品情報をスナップショットとして保持（商品テーブルの変更に影響されない） |
| ゲスト注文 | `orders.member_id` は NULL 許容（ゲスト購入対応） |
| バリエーション | 同一シリーズの色違い商品は `products.variation_group_id` で紐付け |
| カテゴリ別属性 | デスク・チェア・収納家具はそれぞれ専用の属性テーブルを持つ（TPT設計） |
