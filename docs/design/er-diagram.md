# ER図

## 凡例

| 表記     | 意味         |
| -------- | ------------ | -------------- | ------------------- | --- | ------ |
| `PK`     | 主キー       |
| `FK`     | 外部キー     |
| `UQ`     | ユニーク制約 |
| `        |              | --o{`          | 1 対 0以上（1対多） |
| `        |              | --             |                     | `   | 1 対 1 |
| `}o--o{` | 多 対 多     |
| `o       | --o{`        | 0以上 対 0以上 |

---

## ER図

```mermaid
erDiagram

    %% ════════════════════════════════
    %% 会員系
    %% ════════════════════════════════

    members {
        bigint member_id PK
        varchar personal_or_corporate
        varchar last_name
        varchar first_name
        varchar last_name_kana
        varchar first_name_kana
        varchar company_name
        varchar department_name
        varchar email UQ
        varchar gender
        date    anniversary_date
        varchar password_hash
        boolean newsletter_opt_in
        char    postal_code
        varchar prefecture
        varchar city
        varchar address_line
        integer delivery_floor
        boolean has_elevator
        varchar daytime_phone
        varchar fax
        varchar member_status
        timestamptz withdrawn_at
        timestamptz created_at
        timestamptz updated_at
    }

    member_additional_addresses {
        bigint  member_address_id PK
        bigint  member_id FK
        varchar last_name
        varchar first_name
        varchar last_name_kana
        varchar first_name_kana
        varchar company_name
        varchar department_name
        char    postal_code
        varchar prefecture
        varchar city
        varchar address_line
        integer delivery_floor
        boolean has_elevator
        varchar daytime_phone
        varchar fax
        timestamptz created_at
        timestamptz updated_at
    }

    member_favorites {
        bigint member_id PK-FK
        bigint product_id PK-FK
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% 商品系
    %% ════════════════════════════════

    products {
        bigint  product_id PK
        varchar product_name
        varchar category_id
        text    description
        boolean assembly_available
        numeric assembly_fee
        boolean has_variation
        bigint  variation_group_id
        varchar variation_name
        timestamptz sale_start_at
        timestamptz sale_end_at
        timestamptz created_at
        timestamptz updated_at
    }

    product_variants {
        bigint  product_variant_id PK
        bigint  product_id FK
        varchar product_code UQ
        bigint  color_id FK
        numeric unit_price
        integer stock_quantity
        timestamptz created_at
        timestamptz updated_at
    }

    colors {
        bigint  color_id PK
        varchar color_name UQ
        char    color_code UQ
        varchar swatch_type
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% 商品カテゴリ属性
    %% ════════════════════════════════

    product_desk_attributes {
        bigint  product_id PK-FK
        bigint  top_shape_id FK
        integer width_mm
        integer depth_mm
        integer height_mm
        bigint  taste_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    product_chair_attributes {
        bigint  product_id PK-FK
        bigint  function_id FK
        bigint  material_id FK
        bigint  taste_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    product_storage_attributes {
        bigint  product_id PK-FK
        bigint  usage_id FK
        bigint  taste_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% マスタ（デスク）
    %% ════════════════════════════════

    desk_top_shapes {
        bigint  top_shape_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    desk_tastes {
        bigint  taste_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% マスタ（チェア）
    %% ════════════════════════════════

    chair_functions {
        bigint  function_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    chair_materials {
        bigint  material_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    chair_tastes {
        bigint  taste_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% マスタ（収納）
    %% ════════════════════════════════

    storage_usages {
        bigint  usage_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    storage_tastes {
        bigint  taste_id PK
        varchar display_name UQ
        integer sort_order
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% 注文系
    %% ════════════════════════════════

    orders {
        bigint  order_id PK
        varchar order_number UQ
        timestamptz order_datetime
        varchar order_status
        varchar customer_type
        varchar personal_or_corporate
        bigint  member_id FK
        varchar customer_last_name
        varchar customer_first_name
        varchar customer_last_name_kana
        varchar customer_first_name_kana
        varchar company_name
        varchar department_name
        varchar customer_email
        varchar daytime_phone
        varchar shipping_fax
        char    shipping_postal_code
        varchar shipping_prefecture
        varchar shipping_city
        varchar shipping_address_line
        integer shipping_floor
        boolean shipping_has_elevator
        varchar payment_method
        jsonb   payment_instruction
        varchar shipping_method
        numeric shipping_fee
        numeric assembly_fee_total
        numeric subtotal_amount
        numeric tax_amount
        numeric total_amount
        boolean receipt_issued
        varchar note
        timestamptz created_at
        timestamptz updated_at
    }

    order_items {
        bigint  order_item_id PK
        bigint  order_id FK
        varchar product_code
        varchar product_name
        varchar color_name
        numeric unit_price
        integer quantity
        numeric line_subtotal
        numeric line_tax_amount
        numeric line_total_amount
        boolean assembly_available
        numeric assembly_fee
        timestamptz created_at
        timestamptz updated_at
    }

    order_status_histories {
        bigint  order_status_history_id PK
        bigint  order_id FK
        varchar status
        varchar changed_by_system
        timestamptz changed_at
    }

    order_number_counters {
        date    order_date PK
        integer last_sequence
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% コンテンツ・問い合わせ
    %% ════════════════════════════════

    announcements {
        bigint  announcement_id PK
        varchar title
        varchar summary
        text    body
        timestamptz published_start_at
        timestamptz published_end_at
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    inquiries {
        bigint  inquiry_id PK
        bigint  member_id FK
        varchar company_name
        varchar department_name
        varchar last_name
        varchar first_name
        varchar email
        varchar phone
        varchar inquiry_type
        varchar order_phase
        varchar product_name
        varchar product_code
        varchar message
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% バッチ・ランキング
    %% ════════════════════════════════

    tax_rates {
        bigint  tax_rate_id PK
        numeric tax_rate_percent
        timestamptz effective_start_at
        timestamptz effective_end_at
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    popular_product_rankings {
        date    ranking_date PK
        smallint rank PK
        bigint  product_id FK
        integer sold_quantity_1m
        timestamptz created_at
        timestamptz updated_at
    }

    recommended_related_products {
        date    recommendation_date PK
        bigint  source_product_id PK-FK
        smallint rank PK
        bigint  recommended_product_id FK
        numeric score
        timestamptz created_at
        timestamptz updated_at
    }

    %% ════════════════════════════════
    %% Spring Batch メタデータ
    %% ════════════════════════════════

    batch_job_instance {
        bigint  job_instance_id PK
        bigint  version
        varchar job_name
        varchar job_key
    }

    batch_job_execution {
        bigint  job_execution_id PK
        bigint  version
        bigint  job_instance_id FK
        timestamp create_time
        timestamp start_time
        timestamp end_time
        varchar status
        varchar exit_code
        varchar exit_message
        timestamp last_updated
    }

    batch_job_execution_params {
        bigint  job_execution_id FK
        varchar parameter_name
        varchar parameter_type
        varchar parameter_value
        char    identifying
    }

    batch_job_execution_context {
        bigint  job_execution_id PK-FK
        varchar short_context
        text    serialized_context
    }

    batch_step_execution {
        bigint  step_execution_id PK
        bigint  version
        varchar step_name
        bigint  job_execution_id FK
        timestamp create_time
        timestamp start_time
        timestamp end_time
        varchar status
        bigint  commit_count
        bigint  read_count
        bigint  filter_count
        bigint  write_count
        bigint  read_skip_count
        bigint  write_skip_count
        bigint  process_skip_count
        bigint  rollback_count
        varchar exit_code
        varchar exit_message
        timestamp last_updated
    }

    batch_step_execution_context {
        bigint  step_execution_id PK-FK
        varchar short_context
        text    serialized_context
    }

    %% ════════════════════════════════
    %% リレーション定義
    %% ════════════════════════════════

    %% 会員系
    members ||--o{ member_additional_addresses : "1人の会員は複数の追加お届け先を持つ"
    members ||--o{ member_favorites : "1人の会員は複数のお気に入りを持つ"
    members ||--o{ orders : "1人の会員は複数の注文をする（ゲスト注文はNULL）"
    members ||--o{ inquiries : "1人の会員は複数のお問い合わせをする（ゲストはNULL）"

    %% 商品系
    products ||--o{ product_variants : "1つの商品は複数のカラーバリアントを持つ"
    products ||--o| product_desk_attributes : "デスク商品は1つのデスク属性を持つ"
    products ||--o| product_chair_attributes : "チェア商品は1つのチェア属性を持つ"
    products ||--o| product_storage_attributes : "収納商品は1つの収納属性を持つ"
    colors   ||--o{ product_variants : "1つのカラーは複数のバリアントで使われる"

    %% 商品属性 ↔ マスタ
    desk_top_shapes ||--o{ product_desk_attributes : "天板形状マスタ"
    desk_tastes     ||--o{ product_desk_attributes : "デスクテイストマスタ"
    chair_functions ||--o{ product_chair_attributes : "チェア機能マスタ"
    chair_materials ||--o{ product_chair_attributes : "チェア素材マスタ"
    chair_tastes    ||--o{ product_chair_attributes : "チェアテイストマスタ"
    storage_usages  ||--o{ product_storage_attributes : "収納用途マスタ"
    storage_tastes  ||--o{ product_storage_attributes : "収納テイストマスタ"

    %% お気に入り（中間テーブル）
    products ||--o{ member_favorites : "1つの商品は複数の会員にお気に入り登録される"

    %% 注文系
    orders ||--o{ order_items : "1つの注文は複数の注文明細を持つ"
    orders ||--o{ order_status_histories : "1つの注文は複数のステータス履歴を持つ"

    %% バッチ・ランキング
    products ||--o{ popular_product_rankings : "1つの商品は複数日のランキングに登場する"
    products ||--o{ recommended_related_products : "おすすめ元商品"
    products ||--o{ recommended_related_products : "おすすめ対象商品"

    %% Spring Batch メタデータ
    batch_job_instance       ||--o{ batch_job_execution         : "1つのジョブインスタンスは複数の実行を持つ"
    batch_job_execution      ||--o{ batch_job_execution_params  : "1つの実行は複数のパラメータを持つ"
    batch_job_execution      ||--||  batch_job_execution_context : "1つの実行は1つのコンテキストを持つ"
    batch_job_execution      ||--o{ batch_step_execution        : "1つの実行は複数のステップ実行を持つ"
    batch_step_execution     ||--||  batch_step_execution_context : "1つのステップ実行は1つのコンテキストを持つ"
```

---

## テーブルグループ概要

| グループ                 | テーブル数 | 主なテーブル                                                                        |
| ------------------------ | ---------- | ----------------------------------------------------------------------------------- |
| 会員系                   | 3          | `members`, `member_additional_addresses`, `member_favorites`                        |
| 商品系                   | 2          | `products`, `product_variants`                                                      |
| カラーマスタ             | 1          | `colors`                                                                            |
| 商品カテゴリ属性         | 3          | `product_desk_attributes`, `product_chair_attributes`, `product_storage_attributes` |
| カテゴリマスタ（デスク） | 2          | `desk_top_shapes`, `desk_tastes`                                                    |
| カテゴリマスタ（チェア） | 3          | `chair_functions`, `chair_materials`, `chair_tastes`                                |
| カテゴリマスタ（収納）   | 2          | `storage_usages`, `storage_tastes`                                                  |
| 注文系                   | 4          | `orders`, `order_items`, `order_status_histories`, `order_number_counters`          |
| コンテンツ・問い合わせ   | 2          | `announcements`, `inquiries`                                                        |
| バッチ・ランキング       | 3          | `tax_rates`, `popular_product_rankings`, `recommended_related_products`             |
| Spring Batch メタデータ  | 6          | `batch_job_instance`, `batch_job_execution`, `batch_step_execution` 等              |
| **合計**                 | **31**     |                                                                                     |

## 主要な設計ポイント

- **注文番号の採番**: `orders.order_number` は `order_number_counters` テーブルを使った日次連番で採番される（BUG-001 参照）。`orders` テーブルとの直接の FK はない
- **ゲスト注文対応**: `orders.member_id` は NULL 許容。`customer_type = 'guest'` の場合は NULL
- **商品属性の水平分割**: デスク・チェア・収納の属性は各カテゴリ専用テーブルに分けて管理（`product_desk_attributes` 等）
- **注文明細のスナップショット**: `order_items` は商品名・カラー名・単価を注文時点の値でコピー保持（`product_variants` への FK なし）
- **お気に入りの複合 PK**: `member_favorites` は `(member_id, product_id)` が主キー
- **ランキングの複合 PK**: `popular_product_rankings` は `(ranking_date, rank)` が主キー
- **Spring Batch メタデータ**: フレームワーク管理のテーブル群。アプリ業務テーブルとの FK はない
