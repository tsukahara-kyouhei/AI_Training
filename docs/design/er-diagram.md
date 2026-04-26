# ER図

> 最終更新: 2026-04-04  
> ※ 全テーブルに共通する `created_at`（作成日時）・`updated_at`（更新日時）カラムは省略しています。  
> ※ 他ドメインのテーブルを参照する場合、`<<外部>>` として PK のみ略記しています。

---

## 目次

1. [会員ドメイン](#1-会員ドメイン)
2. [商品ドメイン](#2-商品ドメイン)
3. [商品カテゴリ属性・マスタドメイン](#3-商品カテゴリ属性マスタドメイン)
4. [注文ドメイン](#4-注文ドメイン)
5. [コンテンツドメイン](#5-コンテンツドメイン)
6. [バッチ・ランキングドメイン](#6-バッチランキングドメイン)
7. [Spring Batch メタデータ](#7-spring-batch-メタデータ)
8. [テーブル一覧サマリ](#8-テーブル一覧サマリ)

---

## 1. 会員ドメイン

| テーブル | 論理名 |
|---------|--------|
| `members` | 会員 |
| `member_additional_addresses` | 会員追加お届け先 |
| `member_favorites` | 会員お気に入り |

```mermaid
erDiagram

    members {
        bigint      member_id              PK "会員ID"
        varchar     personal_or_corporate     "個人/法人区分"
        varchar     last_name                 "姓"
        varchar     first_name                "名"
        varchar     last_name_kana            "姓カナ"
        varchar     first_name_kana           "名カナ"
        varchar     company_name              "会社名"
        varchar     department_name           "部署名"
        varchar     email                     "メールアドレス"
        varchar     gender                    "性別"
        date        anniversary_date          "生年月日/記念日"
        varchar     password_hash             "パスワードハッシュ"
        boolean     newsletter_opt_in         "メールマガジン希望"
        char        postal_code               "郵便番号"
        varchar     prefecture                "都道府県"
        varchar     city                      "市区町村"
        varchar     address_line              "番地・ビル名"
        integer     delivery_floor            "お届け先階数"
        boolean     has_elevator              "エレベーター有無"
        varchar     daytime_phone             "日中連絡可能電話番号"
        varchar     fax                       "FAX"
        varchar     member_status             "会員状態(active/withdrawn)"
        timestamptz withdrawn_at              "退会日時"
    }

    member_additional_addresses {
        bigint  member_address_id    PK "会員追加お届け先ID"
        bigint  member_id            FK "会員ID"
        varchar last_name               "姓"
        varchar first_name              "名"
        varchar last_name_kana          "姓カナ"
        varchar first_name_kana         "名カナ"
        varchar company_name            "会社名"
        varchar department_name         "部署名"
        char    postal_code             "郵便番号"
        varchar prefecture              "都道府県"
        varchar city                    "市区町村"
        varchar address_line            "番地・ビル名"
        integer delivery_floor          "お届け先階数"
        boolean has_elevator            "エレベーター有無"
        varchar daytime_phone           "日中連絡可能電話番号"
        varchar fax                     "FAX"
    }

    member_favorites {
        bigint member_id    PK,FK "会員ID"
        bigint product_id   PK,FK "商品ID <<外部>>"
    }

    products["products <<外部>>"] {
        bigint product_id PK "商品ID"
    }

    members                 ||--o{ member_additional_addresses : "追加お届け先を持つ"
    members                 ||--o{ member_favorites            : "お気に入り登録"
    products                ||--o{ member_favorites            : "お気に入り"
```

---

## 2. 商品ドメイン

| テーブル | 論理名 |
|---------|--------|
| `products` | 商品 |
| `colors` | カラーマスタ |
| `product_variants` | 商品バリアント |

```mermaid
erDiagram

    products {
        bigint      product_id          PK "商品ID"
        varchar     product_name           "商品名"
        varchar     category_id            "商品カテゴリID(desk/chair/storage)"
        text        description            "商品紹介文"
        boolean     assembly_available     "組立・設置可否"
        numeric     assembly_fee           "組立・設置費"
        boolean     has_variation          "シリーズバリエーション有無"
        bigint      variation_group_id     "バリエーショングループID"
        varchar     variation_name         "バリエーション名"
        timestamptz sale_start_at          "販売開始日時"
        timestamptz sale_end_at            "販売終了日時"
    }

    colors {
        bigint  color_id     PK "カラーID"
        varchar color_name      "カラー名"
        char    color_code      "カラーコード(#RRGGBB)"
        varchar swatch_type     "色見本表示種別"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    product_variants {
        bigint  product_variant_id  PK "商品バリアントID"
        bigint  product_id          FK "商品ID"
        varchar product_code           "商品コード"
        bigint  color_id            FK "カラーID"
        numeric unit_price             "販売価格"
        integer stock_quantity         "在庫数"
    }

    products         ||--o{ product_variants : "バリアントを持つ"
    colors           ||--o{ product_variants : "カラー"
```

---

## 3. 商品カテゴリ属性・マスタドメイン

カテゴリ別の拡張属性テーブルと、絞り込み検索用のマスタテーブルです。

| テーブル | 論理名 |
|---------|--------|
| `product_desk_attributes` | 商品デスク属性 |
| `product_chair_attributes` | 商品チェア属性 |
| `product_storage_attributes` | 商品収納家具属性 |
| `desk_top_shapes` | デスク天板形状マスタ |
| `desk_tastes` | デスクテイストマスタ |
| `chair_functions` | チェア機能マスタ |
| `chair_materials` | チェア素材マスタ |
| `chair_tastes` | チェアテイストマスタ |
| `storage_usages` | 収納家具用途マスタ |
| `storage_tastes` | 収納家具テイストマスタ |

```mermaid
erDiagram

    products["products <<外部>>"] {
        bigint product_id PK "商品ID"
    }

    product_desk_attributes {
        bigint  product_id    PK,FK "商品ID"
        bigint  top_shape_id  FK    "天板形状ID"
        integer width_mm            "幅(mm)"
        integer depth_mm            "奥行(mm)"
        integer height_mm           "高さ(mm)"
        bigint  taste_id      FK    "テイストID"
    }

    product_chair_attributes {
        bigint product_id    PK,FK "商品ID"
        bigint function_id   FK    "機能ID"
        bigint material_id   FK    "素材ID"
        bigint taste_id      FK    "テイストID"
    }

    product_storage_attributes {
        bigint product_id  PK,FK "商品ID"
        bigint usage_id    FK    "用途ID"
        bigint taste_id    FK    "テイストID"
    }

    desk_top_shapes {
        bigint  top_shape_id  PK "天板形状ID"
        varchar display_name     "天板形状名"
        integer sort_order       "表示順"
        boolean is_active        "有効フラグ"
    }

    desk_tastes {
        bigint  taste_id     PK "テイストID"
        varchar display_name    "テイスト名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    chair_functions {
        bigint  function_id  PK "機能ID"
        varchar display_name    "機能名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    chair_materials {
        bigint  material_id  PK "素材ID"
        varchar display_name    "素材名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    chair_tastes {
        bigint  taste_id     PK "テイストID"
        varchar display_name    "テイスト名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    storage_usages {
        bigint  usage_id     PK "用途ID"
        varchar display_name    "用途名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    storage_tastes {
        bigint  taste_id     PK "テイストID"
        varchar display_name    "テイスト名"
        integer sort_order      "表示順"
        boolean is_active       "有効フラグ"
    }

    products           ||--o| product_desk_attributes    : "デスク属性"
    products           ||--o| product_chair_attributes   : "チェア属性"
    products           ||--o| product_storage_attributes : "収納家具属性"

    desk_top_shapes    ||--o{ product_desk_attributes    : "天板形状"
    desk_tastes        ||--o{ product_desk_attributes    : "テイスト"

    chair_functions    ||--o{ product_chair_attributes   : "機能"
    chair_materials    ||--o{ product_chair_attributes   : "素材"
    chair_tastes       ||--o{ product_chair_attributes   : "テイスト"

    storage_usages     ||--o{ product_storage_attributes : "用途"
    storage_tastes     ||--o{ product_storage_attributes : "テイスト"
```

---

## 4. 注文ドメイン

| テーブル | 論理名 |
|---------|--------|
| `orders` | 注文 |
| `order_number_counters` | 注文番号採番カウンタ |
| `order_items` | 注文明細 |
| `order_status_histories` | 注文ステータス履歴 |

```mermaid
erDiagram

    members["members <<外部>>"] {
        bigint member_id PK "会員ID"
    }

    orders {
        bigint      order_id               PK "注文ID"
        varchar     order_number              "注文番号"
        timestamptz order_datetime            "注文日時"
        varchar     order_status              "注文ステータス"
        varchar     customer_type             "顧客種別(guest/member)"
        varchar     personal_or_corporate     "個人/法人区分"
        bigint      member_id             FK  "会員ID(ゲスト時NULL)"
        varchar     customer_last_name        "注文者姓"
        varchar     customer_first_name       "注文者名"
        varchar     customer_last_name_kana   "注文者姓カナ"
        varchar     customer_first_name_kana  "注文者名カナ"
        varchar     company_name              "会社名"
        varchar     department_name           "部署名"
        varchar     customer_email            "注文者メールアドレス"
        varchar     daytime_phone             "日中連絡可能電話番号"
        varchar     shipping_fax              "配送先FAX"
        char        shipping_postal_code      "配送先郵便番号"
        varchar     shipping_prefecture       "配送先都道府県"
        varchar     shipping_city             "配送先市区町村"
        varchar     shipping_address_line     "配送先番地・ビル名"
        integer     shipping_floor            "配送先階数"
        boolean     shipping_has_elevator     "配送先エレベーター有無"
        varchar     payment_method            "決済手段"
        jsonb       payment_instruction       "決済案内情報"
        varchar     shipping_method           "配送方法区分"
        numeric     shipping_fee              "送料"
        numeric     assembly_fee_total        "組立・設置費合計"
        numeric     subtotal_amount           "商品小計"
        numeric     tax_amount                "消費税額"
        numeric     total_amount              "合計金額"
        boolean     receipt_issued            "領収書発行フラグ"
        varchar     note                      "備考"
    }

    order_number_counters {
        date    order_date      PK "注文日"
        integer last_sequence      "最終連番"
    }

    order_items {
        bigint  order_item_id       PK "注文明細ID"
        bigint  order_id            FK "注文ID"
        varchar product_code           "商品コード(スナップショット)"
        varchar product_name           "商品名(スナップショット)"
        varchar color_name             "カラー名(スナップショット)"
        numeric unit_price             "単価"
        integer quantity               "数量"
        numeric line_subtotal          "明細小計"
        numeric line_tax_amount        "明細消費税額"
        numeric line_total_amount      "明細合計金額"
        boolean assembly_available     "組立・設置可否"
        numeric assembly_fee           "組立・設置費"
    }

    order_status_histories {
        bigint      order_status_history_id  PK "注文ステータス履歴ID"
        bigint      order_id                 FK "注文ID"
        varchar     status                      "ステータス"
        varchar     changed_by_system           "変更元システム"
        timestamptz changed_at                  "変更日時"
    }

    members  |o--o{ orders                : "注文"
    orders   ||--|{ order_items           : "注文明細を持つ"
    orders   ||--o{ order_status_histories : "ステータス履歴"
```

> **注意**: `order_items` は注文確定時点の商品情報（商品名・カラー名・単価）をスナップショットとして保持します。`products` / `product_variants` への FK は持たず、商品マスタ変更の影響を受けません。

---

## 5. コンテンツドメイン

| テーブル | 論理名 |
|---------|--------|
| `announcements` | お知らせ |
| `inquiries` | お問い合わせ |

```mermaid
erDiagram

    members["members <<外部>>"] {
        bigint member_id PK "会員ID"
    }

    announcements {
        bigint      announcement_id   PK "お知らせID"
        varchar     title                "タイトル"
        varchar     summary              "要約"
        text        body                 "本文"
        timestamptz published_start_at   "掲載開始日時"
        timestamptz published_end_at     "掲載終了日時"
        boolean     is_active            "有効フラグ"
    }

    inquiries {
        bigint  inquiry_id       PK "お問い合わせID"
        bigint  member_id        FK "会員ID(非会員時NULL)"
        varchar company_name        "会社名"
        varchar department_name     "部署名"
        varchar last_name           "姓"
        varchar first_name          "名"
        varchar email               "メールアドレス"
        varchar phone               "電話番号"
        varchar inquiry_type        "お問い合わせ種別"
        varchar order_phase         "注文状況(before_order/after_order)"
        varchar product_name        "お問い合わせ商品名"
        varchar product_code        "商品コード"
        varchar message             "お問い合わせ内容"
    }

    members |o--o{ inquiries : "お問い合わせ"
```

---

## 6. バッチ・ランキングドメイン

| テーブル | 論理名 |
|---------|--------|
| `tax_rates` | 消費税率 |
| `popular_product_rankings` | 売れ筋ランキング |
| `recommended_related_products` | おすすめ関連商品 |

```mermaid
erDiagram

    products["products <<外部>>"] {
        bigint product_id PK "商品ID"
    }

    tax_rates {
        bigint      tax_rate_id         PK "消費税率ID"
        numeric     tax_rate_percent       "消費税率(%)"
        timestamptz effective_start_at     "適用開始日時"
        timestamptz effective_end_at       "適用終了日時"
        boolean     is_active              "有効フラグ"
    }

    popular_product_rankings {
        date     ranking_date       PK "集計日"
        smallint rank               PK "順位(1〜10)"
        bigint   product_id         FK "商品ID"
        integer  sold_quantity_1m      "直近1か月販売数量"
    }

    recommended_related_products {
        date     recommendation_date    PK    "推薦日"
        bigint   source_product_id      PK,FK "基準商品ID"
        smallint rank                   PK    "順位(1〜4)"
        bigint   recommended_product_id FK    "推薦商品ID"
        numeric  score                        "類似度スコア"
    }

    products ||--o{ popular_product_rankings       : "売れ筋ランキング"
    products ||--o{ recommended_related_products   : "基準商品(source)"
    products ||--o{ recommended_related_products   : "推薦商品(target)"
```

---

## 7. Spring Batch メタデータ

Spring Batch フレームワークが自動管理するジョブ実行管理テーブルです。

| テーブル | 論理名 |
|---------|--------|
| `batch_job_instance` | ジョブインスタンス |
| `batch_job_execution` | ジョブ実行 |
| `batch_job_execution_params` | ジョブ実行パラメータ |
| `batch_job_execution_context` | ジョブ実行コンテキスト |
| `batch_step_execution` | ステップ実行 |
| `batch_step_execution_context` | ステップ実行コンテキスト |


```mermaid
erDiagram

    batch_job_instance {
        bigint  job_instance_id  PK "ジョブインスタンスID"
        bigint  version             "バージョン"
        varchar job_name            "ジョブ名"
        varchar job_key             "ジョブキー"
    }

    batch_job_execution {
        bigint    job_execution_id  PK "ジョブ実行ID"
        bigint    version              "バージョン"
        bigint    job_instance_id   FK "ジョブインスタンスID"
        timestamp create_time          "作成日時"
        timestamp start_time           "開始日時"
        timestamp end_time             "終了日時"
        varchar   status               "ステータス"
        varchar   exit_code            "終了コード"
        varchar   exit_message         "終了メッセージ"
        timestamp last_updated         "最終更新日時"
    }

    batch_job_execution_params {
        bigint  job_execution_id  FK "ジョブ実行ID"
        varchar parameter_name       "パラメータ名"
        varchar parameter_type       "パラメータ型"
        varchar parameter_value      "パラメータ値"
        char    identifying          "識別フラグ"
    }

    batch_job_execution_context {
        bigint job_execution_id  PK,FK "ジョブ実行ID"
        varchar short_context          "コンテキスト(短縮)"
        text    serialized_context     "コンテキスト(シリアライズ)"
    }

    batch_step_execution {
        bigint    step_execution_id  PK "ステップ実行ID"
        bigint    version               "バージョン"
        varchar   step_name             "ステップ名"
        bigint    job_execution_id   FK "ジョブ実行ID"
        timestamp create_time           "作成日時"
        timestamp start_time            "開始日時"
        timestamp end_time              "終了日時"
        varchar   status                "ステータス"
        bigint    commit_count          "コミット件数"
        bigint    read_count            "読み込み件数"
        bigint    filter_count          "フィルタ件数"
        bigint    write_count           "書き込み件数"
        bigint    read_skip_count       "読み込みスキップ件数"
        bigint    write_skip_count      "書き込みスキップ件数"
        bigint    process_skip_count    "処理スキップ件数"
        bigint    rollback_count        "ロールバック件数"
        varchar   exit_code             "終了コード"
        varchar   exit_message          "終了メッセージ"
        timestamp last_updated          "最終更新日時"
    }

    batch_step_execution_context {
        bigint step_execution_id  PK,FK "ステップ実行ID"
        varchar short_context           "コンテキスト(短縮)"
        text    serialized_context      "コンテキスト(シリアライズ)"
    }

    batch_job_instance         ||--|{ batch_job_execution          : "実行"
    batch_job_execution        ||--o{ batch_job_execution_params   : "パラメータ"
    batch_job_execution        ||--|{ batch_step_execution         : "ステップ実行"
    batch_job_execution        ||--|| batch_job_execution_context  : "コンテキスト"
    batch_step_execution       ||--|| batch_step_execution_context : "コンテキスト"
```

---

## 8. テーブル一覧サマリ

| ドメイン | テーブル名 | 論理名 |
|---------|-----------|--------|
| 会員 | `members` | 会員 |
| 会員 | `member_additional_addresses` | 会員追加お届け先 |
| 会員 | `member_favorites` | 会員お気に入り |
| 商品 | `products` | 商品 |
| 商品 | `colors` | カラーマスタ |
| 商品 | `product_variants` | 商品バリアント |
| 商品 | `product_desk_attributes` | 商品デスク属性 |
| 商品 | `product_chair_attributes` | 商品チェア属性 |
| 商品 | `product_storage_attributes` | 商品収納家具属性 |
| マスタ | `desk_top_shapes` | デスク天板形状マスタ |
| マスタ | `desk_tastes` | デスクテイストマスタ |
| マスタ | `chair_functions` | チェア機能マスタ |
| マスタ | `chair_materials` | チェア素材マスタ |
| マスタ | `chair_tastes` | チェアテイストマスタ |
| マスタ | `storage_usages` | 収納家具用途マスタ |
| マスタ | `storage_tastes` | 収納家具テイストマスタ |
| 注文 | `orders` | 注文 |
| 注文 | `order_number_counters` | 注文番号採番カウンタ |
| 注文 | `order_items` | 注文明細 |
| 注文 | `order_status_histories` | 注文ステータス履歴 |
| コンテンツ | `announcements` | お知らせ |
| コンテンツ | `inquiries` | お問い合わせ |
| バッチ | `tax_rates` | 消費税率 |
| バッチ | `popular_product_rankings` | 売れ筋ランキング |
| バッチ | `recommended_related_products` | おすすめ関連商品 |
| Spring Batch | `batch_job_instance` | ジョブインスタンス |
| Spring Batch | `batch_job_execution` | ジョブ実行 |
| Spring Batch | `batch_job_execution_params` | ジョブ実行パラメータ |
| Spring Batch | `batch_job_execution_context` | ジョブ実行コンテキスト |
| Spring Batch | `batch_step_execution` | ステップ実行 |
| Spring Batch | `batch_step_execution_context` | ステップ実行コンテキスト |
