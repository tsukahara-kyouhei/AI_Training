# ER図

## 全体図（関連性）

商品、会員、注文の主要リレーションシップ：

```mermaid
erDiagram
    PRODUCTS ||--o{ PRODUCT_VARIANTS : "has variants"
    PRODUCTS ||--o| PRODUCT_DESK_ATTRIBUTES : "desk"
    PRODUCTS ||--o| PRODUCT_CHAIR_ATTRIBUTES : "chair"
    PRODUCTS ||--o| PRODUCT_STORAGE_ATTRIBUTES : "storage"
    PRODUCTS ||--o{ MEMBER_FAVORITES : "favorited"
    PRODUCTS ||--o{ POPULAR_PRODUCT_RANKINGS : "ranked"
    PRODUCTS ||--o{ RECOMMENDED_RELATED_PRODUCTS : "recommended"
    COLORS ||--o{ PRODUCT_VARIANTS : "has"
    
    MEMBERS ||--o{ MEMBER_ADDITIONAL_ADDRESSES : "has"
    MEMBERS ||--o{ MEMBER_FAVORITES : "has"
    MEMBERS ||--o{ ORDERS : "places"
    MEMBERS ||--o{ INQUIRIES : "submits"
    
    ORDERS ||--o{ ORDER_ITEMS : "contains"
    ORDERS ||--o{ ORDER_STATUS_HISTORIES : "history"
    
    DESK_TOP_SHAPES ||--o{ PRODUCT_DESK_ATTRIBUTES : "shape"
    CHAIR_FUNCTIONS ||--o{ PRODUCT_CHAIR_ATTRIBUTES : "function"
    CHAIR_MATERIALS ||--o{ PRODUCT_CHAIR_ATTRIBUTES : "material"
    STORAGE_USAGES ||--o{ PRODUCT_STORAGE_ATTRIBUTES : "usage"
    TASTES ||--o{ PRODUCT_DESK_ATTRIBUTES : "taste"
    TASTES ||--o{ PRODUCT_CHAIR_ATTRIBUTES : "taste"
    TASTES ||--o{ PRODUCT_STORAGE_ATTRIBUTES : "taste"
```

## マスタテーブル

```mermaid
erDiagram
    COLORS {
        bigint color_id PK "カラーID"
        varchar color_name UK "カラー名"
        char color_code UK "カラーコード (#RRGGBB)"
        varchar swatch_type "色見本種別 (solid/transparent_pattern)"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    DESK_TOP_SHAPES {
        bigint top_shape_id PK "天板形状ID"
        varchar display_name UK "天板形状名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    CHAIR_FUNCTIONS {
        bigint function_id PK "機能ID"
        varchar display_name UK "機能名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    CHAIR_MATERIALS {
        bigint material_id PK "素材ID"
        varchar display_name UK "素材名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    STORAGE_USAGES {
        bigint usage_id PK "用途ID"
        varchar display_name UK "用途名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    TASTES {
        bigint taste_id PK "テイストID"
        varchar display_name UK "テイスト名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    TAX_RATES {
        bigint tax_rate_id PK "消費税率ID"
        numeric tax_rate_percent "消費税率 (%)"
        timestamptz effective_start_at "適用開始日時"
        timestamptz effective_end_at "適用終了日時"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }
```

## 商品テーブル

```mermaid
erDiagram
    PRODUCTS {
        bigint product_id PK "商品ID"
        varchar product_name "商品名"
        varchar category_id "カテゴリID"
        text description "商品紹介文"
        boolean assembly_available "組立・設置可否"
        numeric assembly_fee "組立・設置費"
        boolean has_variation "バリエーション有無"
        bigint variation_group_id "グループID"
        varchar variation_name "バリエーション名"
        timestamptz sale_start_at "販売開始日時"
        timestamptz sale_end_at "販売終了日時"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_VARIANTS {
        bigint product_variant_id PK "バリアントID"
        bigint product_id FK "商品ID"
        varchar product_code UK "商品コード"
        bigint color_id FK "カラーID"
        numeric unit_price "販売価格"
        int stock_quantity "在庫数"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_DESK_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint top_shape_id FK "天板形状ID"
        int width_mm "幅 (mm)"
        int depth_mm "奥行 (mm)"
        int height_mm "高さ (mm)"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_CHAIR_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint function_id FK "機能ID"
        bigint material_id FK "素材ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_STORAGE_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint usage_id FK "用途ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    COLORS {
        bigint color_id PK "カラーID"
        varchar color_name UK "カラー名"
        char color_code UK "カラーコード"
        varchar swatch_type "色見本種別"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    DESK_TOP_SHAPES {
        bigint top_shape_id PK "形状ID"
        varchar display_name UK "形状名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    CHAIR_FUNCTIONS {
        bigint function_id PK "機能ID"
        varchar display_name UK "機能名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    CHAIR_MATERIALS {
        bigint material_id PK "素材ID"
        varchar display_name UK "素材名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    STORAGE_USAGES {
        bigint usage_id PK "用途ID"
        varchar display_name UK "用途名"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    TASTES {
        bigint taste_id PK "テイストID"
        varchar display_name UK "名前"
        int sort_order "表示順"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCTS ||--o{ PRODUCT_VARIANTS : "variants"
    COLORS ||--o{ PRODUCT_VARIANTS : "色"
    PRODUCTS ||--o| PRODUCT_DESK_ATTRIBUTES : "デスク属性"
    PRODUCTS ||--o| PRODUCT_CHAIR_ATTRIBUTES : "チェア属性"
    PRODUCTS ||--o| PRODUCT_STORAGE_ATTRIBUTES : "収納属性"
```

## 商品・マスタ属性の関連

```mermaid
erDiagram
    PRODUCT_DESK_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint top_shape_id FK "形状ID"
        int width_mm "幅mm"
        int depth_mm "奥行mm"
        int height_mm "高さmm"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_CHAIR_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint function_id FK "機能ID"
        bigint material_id FK "素材ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    PRODUCT_STORAGE_ATTRIBUTES {
        bigint product_id PK,FK "商品ID"
        bigint usage_id FK "用途ID"
        bigint taste_id FK "テイストID"
        timestamptz created_at
        timestamptz updated_at
    }

    DESK_TOP_SHAPES {
        bigint top_shape_id PK "形状ID"
        varchar display_name "形状名"
    }

    CHAIR_FUNCTIONS {
        bigint function_id PK "機能ID"
        varchar display_name "機能名"
    }

    CHAIR_MATERIALS {
        bigint material_id PK "素材ID"
        varchar display_name "素材名"
    }

    STORAGE_USAGES {
        bigint usage_id PK "用途ID"
        varchar display_name "用途名"
    }

    TASTES {
        bigint taste_id PK "テイストID"
        varchar display_name "名前"
    }

    DESK_TOP_SHAPES ||--o{ PRODUCT_DESK_ATTRIBUTES : ""
    TASTES ||--o{ PRODUCT_DESK_ATTRIBUTES : ""
    CHAIR_FUNCTIONS ||--o{ PRODUCT_CHAIR_ATTRIBUTES : ""
    CHAIR_MATERIALS ||--o{ PRODUCT_CHAIR_ATTRIBUTES : ""
    TASTES ||--o{ PRODUCT_CHAIR_ATTRIBUTES : ""
    STORAGE_USAGES ||--o{ PRODUCT_STORAGE_ATTRIBUTES : ""
    TASTES ||--o{ PRODUCT_STORAGE_ATTRIBUTES : ""
```

## 会員テーブル

```mermaid
erDiagram
    MEMBERS {
        bigint member_id PK "会員ID"
        varchar personal_or_corporate "個人/法人区分"
        varchar last_name "姓"
        varchar first_name "名"
        varchar last_name_kana "姓カナ"
        varchar first_name_kana "名カナ"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar email UK "メールアドレス"
        varchar gender "性別"
        date anniversary_date "生年月日/記念日"
        varchar password_hash "パスワード"
        boolean newsletter_opt_in "ニュースレター"
        char postal_code "郵便番号"
        varchar prefecture "都道府県"
        varchar city "市区町村"
        varchar address_line "番地・ビル名"
        int delivery_floor "階数"
        boolean has_elevator "エレベーター"
        varchar daytime_phone "日中電話"
        varchar fax "FAX"
        varchar member_status "ステータス"
        timestamptz withdrawn_at "退会日時"
        timestamptz created_at
        timestamptz updated_at
    }

    MEMBER_ADDITIONAL_ADDRESSES {
        bigint member_address_id PK "追加住所ID"
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
        int delivery_floor "階数"
        boolean has_elevator "エレベーター"
        varchar daytime_phone "日中電話"
        varchar fax "FAX"
        timestamptz created_at
        timestamptz updated_at
    }

    MEMBER_FAVORITES {
        bigint member_id PK,FK "会員ID"
        bigint product_id PK,FK "商品ID"
        timestamptz created_at
        timestamptz updated_at
    }

    MEMBERS ||--o{ MEMBER_ADDITIONAL_ADDRESSES : "追加住所"
    MEMBERS ||--o{ MEMBER_FAVORITES : "お気に入り"
```
```

## 注文テーブル

```mermaid
erDiagram
    ORDERS {
        bigint order_id PK "注文ID"
        varchar order_number UK "注文番号"
        timestamptz order_datetime "注文日時"
        varchar order_status "ステータス"
        varchar customer_type "顧客種別"
        varchar personal_or_corporate "個人/法人"
        bigint member_id FK "会員ID"
        varchar customer_last_name "注文者姓"
        varchar customer_first_name "注文者名"
        varchar customer_last_name_kana "注文者姓カナ"
        varchar customer_first_name_kana "注文者名カナ"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar customer_email "メールアドレス"
        varchar daytime_phone "日中電話"
        varchar shipping_fax "配送先FAX"
        char shipping_postal_code "配送先郵便番号"
        varchar shipping_prefecture "配送先都道府県"
        varchar shipping_city "配送先市区町村"
        varchar shipping_address_line "配送先番地"
        int shipping_floor "配送先階数"
        boolean shipping_has_elevator "エレベーター"
        varchar payment_method "決済手段"
        jsonb payment_instruction "決済案内"
        varchar shipping_method "配送方法"
        numeric shipping_fee "送料"
        numeric assembly_fee_total "組立費合計"
        numeric subtotal_amount "小計"
        numeric tax_amount "消費税"
        numeric total_amount "合計"
        boolean receipt_issued "領収書発行"
        varchar note "備考"
        timestamptz created_at
        timestamptz updated_at
    }

    ORDER_ITEMS {
        bigint order_item_id PK "明細ID"
        bigint order_id FK "注文ID"
        varchar product_code "商品コード"
        varchar product_name "商品名"
        varchar color_name "カラー名"
        numeric unit_price "単価"
        int quantity "数量"
        numeric line_subtotal "明細小計"
        numeric line_tax_amount "明細税額"
        numeric line_total_amount "明細合計"
        boolean assembly_available "組立可"
        numeric assembly_fee "組立費"
        timestamptz created_at
        timestamptz updated_at
    }

    ORDER_STATUS_HISTORIES {
        bigint order_status_history_id PK "履歴ID"
        bigint order_id FK "注文ID"
        varchar status "ステータス"
        varchar changed_by_system "変更システム"
        timestamptz changed_at "変更日時"
    }

    ORDER_NUMBER_COUNTERS {
        date order_date PK "注文日"
        int last_sequence "最終連番"
        timestamptz created_at
        timestamptz updated_at
    }

    ORDERS ||--o{ ORDER_ITEMS : "明細"
    ORDERS ||--o{ ORDER_STATUS_HISTORIES : "履歴"
```

## 分析・コンテンツテーブル

```mermaid
erDiagram
    POPULAR_PRODUCT_RANKINGS {
        date ranking_date PK "集計日"
        smallint rank PK "順位"
        bigint product_id FK,UK "商品ID"
        int sold_quantity_1m "販売数量"
        timestamptz created_at
        timestamptz updated_at
    }

    RECOMMENDED_RELATED_PRODUCTS {
        date recommendation_date PK "推薦日"
        bigint source_product_id PK,FK,UK "基準商品ID"
        smallint rank PK "順位"
        bigint recommended_product_id FK,UK "推薦商品ID"
        numeric score "スコア"
        timestamptz created_at
        timestamptz updated_at
    }

    ANNOUNCEMENTS {
        bigint announcement_id PK "ID"
        varchar title "タイトル"
        varchar summary "要約"
        text body "本文"
        timestamptz published_start_at "掲載開始"
        timestamptz published_end_at "掲載終了"
        boolean is_active "有効フラグ"
        timestamptz created_at
        timestamptz updated_at
    }

    INQUIRIES {
        bigint inquiry_id PK "ID"
        bigint member_id FK "会員ID"
        varchar company_name "会社名"
        varchar department_name "部署名"
        varchar last_name "姓"
        varchar first_name "名"
        varchar email "メール"
        varchar phone "電話"
        varchar inquiry_type "種別"
        varchar order_phase "注文状況"
        varchar product_name "商品名"
        varchar product_code "商品コード"
        varchar message "内容"
        timestamptz created_at
        timestamptz updated_at
    }
```

---

## テーブル一覧

| グループ | テーブル名 | 論理名 |
|---|---|---|
| マスタ | `colors` | カラーマスタ |
| マスタ | `desk_top_shapes` | デスク天板形状マスタ |
| マスタ | `chair_functions` | チェア機能マスタ |
| マスタ | `chair_materials` | チェア素材マスタ |
| マスタ | `storage_usages` | 収納家具用途マスタ |
| マスタ | `tastes` | テイストマスタ（統合） |
| マスタ | `tax_rates` | 消費税率 |
| 商品 | `products` | 商品 |
| 商品 | `product_variants` | 商品バリアント |
| 商品 | `product_desk_attributes` | 商品デスク属性 |
| 商品 | `product_chair_attributes` | 商品チェア属性 |
| 商品 | `product_storage_attributes` | 商品収納家具属性 |
| 会員 | `members` | 会員 |
| 会員 | `member_additional_addresses` | 会員追加お届け先 |
| 会員 | `member_favorites` | 会員お気に入り |
| 注文 | `orders` | 注文 |
| 注文 | `order_items` | 注文明細 |
| 注文 | `order_status_histories` | 注文ステータス履歴 |
| 注文 | `order_number_counters` | 注文番号採番カウンタ |
| バッチ/分析 | `popular_product_rankings` | 売れ筋ランキング |
| バッチ/分析 | `recommended_related_products` | おすすめ関連商品 |
| コンテンツ | `announcements` | お知らせ |
| コンテンツ | `inquiries` | お問い合わせ |

## 設計上の補足

- **`order_items` は注文時スナップショット**: 商品名・カラー名・単価は注文確定時点の値を直接保持し、`products` / `product_variants` への FK は持たない。
- **`orders.member_id` は `SET NULL`**: 会員退会時でも注文レコードは保持される。`customer_type = 'member'` かつ `member_id IS NOT NULL` の整合は CHECK 制約で保証。
- **`order_number_counters` はスタンドアロン**: 注文番号の日別連番管理テーブル。`orders` や他テーブルとの FK は持たない。
- **`tastes` は共通マスタ**: デスク・チェア・収納家具の3カテゴリ属性テーブル（`product_*_attributes`）から共通して参照される。
- **`product_*_attributes` の PK は `products.product_id` と同値**: 1対1（PK共有）の識別リレーションシップ。
- **`recommended_related_products` の `products` への FK は2本**: `source_product_id`（基準商品）と `recommended_product_id`（推薦商品）がそれぞれ `products` を参照する。
