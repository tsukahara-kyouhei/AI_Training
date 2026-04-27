# ER 図

## テーブル一覧

| ドメイン | テーブル名 | 論理名 |
|---|---|---|
| マスタ | `desk_top_shapes` | デスク天板形状マスタ |
| マスタ | `tastes` | テイスト統合マスタ |
| マスタ | `chair_functions` | チェア機能マスタ |
| マスタ | `chair_materials` | チェア素材マスタ |
| マスタ | `storage_usages` | 収納家具用途マスタ |
| 商品 | `colors` | カラーマスタ |
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
| 注文 | `order_number_counters` | 注文番号採番カウンタ |
| 注文 | `order_status_histories` | 注文ステータス履歴 |
| コンテンツ | `announcements` | お知らせ |
| コンテンツ | `inquiries` | お問い合わせ |
| バッチ | `tax_rates` | 消費税率 |
| バッチ | `popular_product_rankings` | 売れ筋ランキング |
| バッチ | `recommended_related_products` | おすすめ関連商品 |

---

## 全体 ER 図（概要）

主キー・外部キーと主要カラムのみ表示。詳細は各ドメイン図を参照。

```mermaid
erDiagram
    %% ── マスタ系 ──────────────────────────────────────
    desk_top_shapes {
        bigint  top_shape_id  PK  "天板形状ID"
        varchar display_name      "天板形状名"
    }
    tastes {
        bigint  taste_id      PK  "テイストID"
        varchar display_name      "テイスト名"
    }
    chair_functions {
        bigint  function_id   PK  "機能ID"
        varchar display_name      "機能名"
    }
    chair_materials {
        bigint  material_id   PK  "素材ID"
        varchar display_name      "素材名"
    }
    storage_usages {
        bigint  usage_id      PK  "用途ID"
        varchar display_name      "用途名"
    }
    colors {
        bigint  color_id      PK  "カラーID"
        varchar color_name        "カラー名"
        char    color_code        "カラーコード(#RRGGBB)"
    }
    tax_rates {
        bigint  tax_rate_id   PK  "消費税率ID"
        numeric tax_rate_percent  "消費税率(%)"
    }

    %% ── 商品系 ──────────────────────────────────────
    products {
        bigint  product_id    PK  "商品ID"
        varchar product_name      "商品名"
        varchar category_id       "カテゴリ(desk/chair/storage)"
    }
    product_variants {
        bigint  product_variant_id  PK    "商品バリアントID"
        bigint  product_id          FK    "商品ID"
        bigint  color_id            FK    "カラーID"
        varchar product_code        UK    "商品コード"
        numeric unit_price              "販売価格"
        int     stock_quantity          "在庫数"
    }
    product_desk_attributes {
        bigint  product_id    PK,FK  "商品ID"
        bigint  top_shape_id  FK     "天板形状ID"
        bigint  taste_id      FK     "テイストID"
    }
    product_chair_attributes {
        bigint  product_id    PK,FK  "商品ID"
        bigint  function_id   FK     "機能ID"
        bigint  material_id   FK     "素材ID"
        bigint  taste_id      FK     "テイストID"
    }
    product_storage_attributes {
        bigint  product_id    PK,FK  "商品ID"
        bigint  usage_id      FK     "用途ID"
        bigint  taste_id      FK     "テイストID"
    }

    %% ── 会員系 ──────────────────────────────────────
    members {
        bigint  member_id     PK  "会員ID"
        varchar email             "メールアドレス"
        varchar member_status     "会員状態(active/withdrawn)"
    }
    member_additional_addresses {
        bigint  member_address_id  PK  "会員追加お届け先ID"
        bigint  member_id          FK  "会員ID"
    }
    member_favorites {
        bigint  member_id   PK,FK  "会員ID"
        bigint  product_id  PK,FK  "商品ID"
    }

    %% ── 注文系 ──────────────────────────────────────
    orders {
        bigint  order_id       PK  "注文ID"
        varchar order_number   UK  "注文番号"
        bigint  member_id      FK  "会員ID(null=ゲスト)"
        varchar order_status       "注文ステータス"
        varchar customer_type      "顧客種別(guest/member)"
        numeric total_amount       "合計金額"
    }
    order_items {
        bigint  order_item_id  PK  "注文明細ID"
        bigint  order_id       FK  "注文ID"
        varchar product_code       "商品コード(スナップショット)"
        int     quantity           "数量"
    }
    order_number_counters {
        date    order_date     PK  "注文日"
        int     last_sequence      "最終連番"
    }
    order_status_histories {
        bigint  order_status_history_id  PK  "注文ステータス履歴ID"
        bigint  order_id                 FK  "注文ID"
        varchar status                       "ステータス"
        varchar changed_by_system            "変更元システム"
    }

    %% ── コンテンツ系 ──────────────────────────────────
    announcements {
        bigint  announcement_id  PK  "お知らせID"
        varchar title                "タイトル"
        boolean is_active            "有効フラグ"
    }
    inquiries {
        bigint  inquiry_id   PK  "お問い合わせID"
        bigint  member_id    FK  "会員ID(null=非会員)"
        varchar inquiry_type     "お問い合わせ種別"
    }

    %% ── バッチ系 ──────────────────────────────────────
    popular_product_rankings {
        date      ranking_date  PK    "集計日"
        smallint  rank          PK    "順位"
        bigint    product_id    FK    "商品ID"
        int       sold_quantity_1m     "直近1か月販売数量"
    }
    recommended_related_products {
        date      recommendation_date  PK    "推薦日"
        bigint    source_product_id    PK,FK "基準商品ID"
        smallint  rank                 PK    "順位"
        bigint    recommended_product_id FK  "推薦商品ID"
        numeric   score                      "類似度スコア"
    }

    %% ── リレーション ────────────────────────────────────
    products             ||--o{ product_variants              : "バリアントを持つ"
    products             ||--o| product_desk_attributes       : "デスク属性を持つ"
    products             ||--o| product_chair_attributes      : "チェア属性を持つ"
    products             ||--o| product_storage_attributes    : "収納属性を持つ"
    colors               ||--o{ product_variants              : "カラーが使われる"
    desk_top_shapes      ||--o{ product_desk_attributes       : "天板形状が使われる"
    tastes               ||--o{ product_desk_attributes       : "テイストが使われる"
    tastes               ||--o{ product_chair_attributes      : "テイストが使われる"
    tastes               ||--o{ product_storage_attributes    : "テイストが使われる"
    chair_functions      ||--o{ product_chair_attributes      : "機能が使われる"
    chair_materials      ||--o{ product_chair_attributes      : "素材が使われる"
    storage_usages       ||--o{ product_storage_attributes    : "用途が使われる"
    members              ||--o{ member_additional_addresses   : "追加お届け先を持つ"
    members              ||--o{ member_favorites              : "お気に入りを持つ"
    products             ||--o{ member_favorites              : "お気に入りされる"
    members              |o--o{ orders                        : "注文する"
    orders               ||--o{ order_items                   : "注文明細を持つ"
    orders               ||--o{ order_status_histories        : "ステータス履歴を持つ"
    members              |o--o{ inquiries                     : "問い合わせする"
    products             ||--o{ popular_product_rankings      : "ランキングに入る"
    products             ||--o{ recommended_related_products  : "関連元になる"
    products             ||--o{ recommended_related_products  : "関連先になる"
```

---

## 商品・マスタドメイン 詳細

```mermaid
erDiagram
    desk_top_shapes {
        bigint   top_shape_id  PK  "天板形状ID"
        varchar  display_name      "天板形状名"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    tastes {
        bigint   taste_id      PK  "テイストID"
        varchar  display_name      "テイスト名"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    chair_functions {
        bigint   function_id   PK  "機能ID"
        varchar  display_name      "機能名"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    chair_materials {
        bigint   material_id   PK  "素材ID"
        varchar  display_name      "素材名"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    storage_usages {
        bigint   usage_id      PK  "用途ID"
        varchar  display_name      "用途名"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    colors {
        bigint   color_id      PK  "カラーID"
        varchar  color_name    UK  "カラー名"
        char     color_code    UK  "カラーコード(#RRGGBB)"
        varchar  swatch_type       "色見本表示種別(solid/transparent_pattern)"
        int      sort_order        "表示順"
        boolean  is_active         "有効フラグ"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    products {
        bigint   product_id        PK  "商品ID"
        varchar  product_name          "商品名"
        varchar  category_id           "商品カテゴリID(desk/chair/storage)"
        text     description            "商品紹介文"
        boolean  assembly_available    "組立・設置可否"
        numeric  assembly_fee          "組立・設置費"
        boolean  has_variation         "シリーズバリエーション有無"
        bigint   variation_group_id    "バリエーショングループID"
        varchar  variation_name        "バリエーション名"
        timestamptz sale_start_at      "販売開始日時"
        timestamptz sale_end_at        "販売終了日時"
        timestamptz created_at         "作成日時"
        timestamptz updated_at         "更新日時"
    }
    product_variants {
        bigint   product_variant_id  PK    "商品バリアントID"
        bigint   product_id          FK    "商品ID"
        bigint   color_id            FK    "カラーID"
        varchar  product_code        UK    "商品コード"
        numeric  unit_price              "販売価格"
        int      stock_quantity          "在庫数"
        timestamptz created_at           "作成日時"
        timestamptz updated_at           "更新日時"
    }
    product_desk_attributes {
        bigint   product_id    PK,FK  "商品ID"
        bigint   top_shape_id  FK     "天板形状ID"
        bigint   taste_id      FK     "テイストID"
        int      width_mm              "幅(mm)"
        int      depth_mm              "奥行(mm)"
        int      height_mm             "高さ(mm)"
        timestamptz created_at         "作成日時"
        timestamptz updated_at         "更新日時"
    }
    product_chair_attributes {
        bigint   product_id    PK,FK  "商品ID"
        bigint   function_id   FK     "機能ID"
        bigint   material_id   FK     "素材ID"
        bigint   taste_id      FK     "テイストID"
        timestamptz created_at         "作成日時"
        timestamptz updated_at         "更新日時"
    }
    product_storage_attributes {
        bigint   product_id    PK,FK  "商品ID"
        bigint   usage_id      FK     "用途ID"
        bigint   taste_id      FK     "テイストID"
        timestamptz created_at         "作成日時"
        timestamptz updated_at         "更新日時"
    }

    products             ||--o{ product_variants              : "バリアントを持つ"
    products             ||--o| product_desk_attributes       : "デスク属性を持つ"
    products             ||--o| product_chair_attributes      : "チェア属性を持つ"
    products             ||--o| product_storage_attributes    : "収納属性を持つ"
    colors               ||--o{ product_variants              : "カラーが使われる"
    desk_top_shapes      ||--o{ product_desk_attributes       : "天板形状が使われる"
    tastes               ||--o{ product_desk_attributes       : "テイストが使われる"
    tastes               ||--o{ product_chair_attributes      : "テイストが使われる"
    tastes               ||--o{ product_storage_attributes    : "テイストが使われる"
    chair_functions      ||--o{ product_chair_attributes      : "機能が使われる"
    chair_materials      ||--o{ product_chair_attributes      : "素材が使われる"
    storage_usages       ||--o{ product_storage_attributes    : "用途が使われる"
```

---

## 会員ドメイン 詳細

```mermaid
erDiagram
    members {
        bigint      member_id              PK  "会員ID"
        varchar     personal_or_corporate      "個人/法人区分(personal/corporate)"
        varchar     last_name                  "姓"
        varchar     first_name                 "名"
        varchar     last_name_kana             "姓カナ"
        varchar     first_name_kana            "名カナ"
        varchar     company_name               "会社名(法人時必須)"
        varchar     department_name            "部署名"
        varchar     email              UK      "メールアドレス(小文字一意)"
        varchar     gender                     "性別(male/female/no_answer)"
        date        anniversary_date           "生年月日/記念日"
        varchar     password_hash              "パスワードハッシュ"
        boolean     newsletter_opt_in          "メールマガジン希望有無"
        char        postal_code                "郵便番号(7桁)"
        varchar     prefecture                 "都道府県"
        varchar     city                       "市区町村"
        varchar     address_line               "番地・ビル名"
        int         delivery_floor             "お届け先階数"
        boolean     has_elevator               "エレベーター有無"
        varchar     daytime_phone              "日中連絡可能電話番号"
        varchar     fax                        "FAX"
        varchar     member_status              "会員状態(active/withdrawn)"
        timestamptz withdrawn_at               "退会日時"
        timestamptz created_at                 "作成日時"
        timestamptz updated_at                 "更新日時"
    }
    member_additional_addresses {
        bigint  member_address_id  PK  "会員追加お届け先ID"
        bigint  member_id          FK  "会員ID"
        varchar last_name              "姓"
        varchar first_name             "名"
        varchar last_name_kana         "姓カナ"
        varchar first_name_kana        "名カナ"
        varchar company_name           "会社名"
        varchar department_name        "部署名"
        char    postal_code            "郵便番号(7桁)"
        varchar prefecture             "都道府県"
        varchar city                   "市区町村"
        varchar address_line           "番地・ビル名"
        int     delivery_floor         "お届け先階数"
        boolean has_elevator           "エレベーター有無"
        varchar daytime_phone          "日中連絡可能電話番号"
        varchar fax                    "FAX"
        timestamptz created_at         "作成日時"
        timestamptz updated_at         "更新日時"
    }
    member_favorites {
        bigint  member_id   PK,FK  "会員ID"
        bigint  product_id  PK,FK  "商品ID"
        timestamptz created_at     "作成日時"
        timestamptz updated_at     "更新日時"
    }
    products {
        bigint  product_id    PK  "商品ID"
        varchar product_name      "商品名"
        varchar category_id       "商品カテゴリID"
    }

    members  ||--o{ member_additional_addresses : "追加お届け先を持つ"
    members  ||--o{ member_favorites            : "お気に入りを持つ"
    products ||--o{ member_favorites            : "お気に入りされる"
```

---

## 注文ドメイン 詳細

```mermaid
erDiagram
    members {
        bigint  member_id     PK  "会員ID"
        varchar email             "メールアドレス"
        varchar member_status     "会員状態"
    }
    orders {
        bigint      order_id                PK  "注文ID"
        varchar     order_number            UK  "注文番号"
        timestamptz order_datetime              "注文日時"
        varchar     order_status                "注文ステータス(received/awaiting_payment/processing/completed/cancelled)"
        varchar     customer_type               "顧客種別(guest/member)"
        varchar     personal_or_corporate       "個人/法人区分"
        bigint      member_id               FK  "会員ID(null=ゲスト)"
        varchar     customer_last_name          "注文者姓"
        varchar     customer_first_name         "注文者名"
        varchar     customer_last_name_kana     "注文者姓カナ"
        varchar     customer_first_name_kana    "注文者名カナ"
        varchar     company_name               "会社名"
        varchar     department_name            "部署名"
        varchar     customer_email             "注文者メールアドレス"
        varchar     daytime_phone              "日中連絡可能電話番号"
        varchar     shipping_fax               "配送先FAX"
        char        shipping_postal_code       "配送先郵便番号"
        varchar     shipping_prefecture        "配送先都道府県"
        varchar     shipping_city              "配送先市区町村"
        varchar     shipping_address_line      "配送先番地・ビル名"
        int         shipping_floor             "配送先階数"
        boolean     shipping_has_elevator      "配送先エレベーター有無"
        varchar     payment_method             "決済手段(bank_transfer/cash_on_delivery/convenience_store)"
        jsonb       payment_instruction        "決済案内情報(コンビニ決済時のみ)"
        varchar     shipping_method            "配送方法区分(normal/assembly)"
        numeric     shipping_fee               "送料"
        numeric     assembly_fee_total         "組立・設置費合計"
        numeric     subtotal_amount            "商品小計"
        numeric     tax_amount                 "消費税額"
        numeric     total_amount               "合計金額"
        boolean     receipt_issued             "領収書発行フラグ"
        varchar     note                       "備考"
        timestamptz created_at                 "作成日時"
        timestamptz updated_at                 "更新日時"
    }
    order_items {
        bigint      order_item_id      PK  "注文明細ID"
        bigint      order_id           FK  "注文ID"
        varchar     product_code           "商品コード(受注時スナップショット)"
        varchar     product_name           "商品名(受注時スナップショット)"
        varchar     color_name             "カラー名(受注時スナップショット)"
        numeric     unit_price             "単価"
        int         quantity               "数量"
        numeric     line_subtotal          "明細小計"
        numeric     line_tax_amount        "明細消費税額"
        numeric     line_total_amount      "明細合計金額"
        boolean     assembly_available     "組立・設置可否"
        numeric     assembly_fee           "組立・設置費"
        timestamptz created_at             "作成日時"
        timestamptz updated_at             "更新日時"
    }
    order_number_counters {
        date    order_date      PK  "注文日"
        int     last_sequence       "最終連番"
        timestamptz created_at      "作成日時"
        timestamptz updated_at      "更新日時"
    }
    order_status_histories {
        bigint      order_status_history_id  PK  "注文ステータス履歴ID"
        bigint      order_id                 FK  "注文ID"
        varchar     status                       "ステータス"
        varchar     changed_by_system            "変更元システム"
        timestamptz changed_at                   "変更日時"
    }

    members |o--o{ orders                  : "注文する"
    orders  ||--o{ order_items             : "注文明細を持つ"
    orders  ||--o{ order_status_histories  : "ステータス履歴を持つ"
```

---

## コンテンツ・バッチドメイン 詳細

```mermaid
erDiagram
    announcements {
        bigint      announcement_id   PK  "お知らせID"
        varchar     title                 "タイトル"
        varchar     summary               "要約"
        text        body                  "本文"
        timestamptz published_start_at   "掲載開始日時"
        timestamptz published_end_at     "掲載終了日時"
        boolean     is_active             "有効フラグ"
        timestamptz created_at            "作成日時"
        timestamptz updated_at            "更新日時"
    }
    inquiries {
        bigint      inquiry_id       PK  "お問い合わせID"
        bigint      member_id        FK  "会員ID(null=非会員)"
        varchar     company_name         "会社名"
        varchar     department_name      "部署名"
        varchar     last_name            "姓"
        varchar     first_name           "名"
        varchar     email                "メールアドレス"
        varchar     phone                "電話番号"
        varchar     inquiry_type         "お問い合わせ種別(product/delivery_date/order/shipping/return_cancel/other)"
        varchar     order_phase          "注文状況(before_order/after_order)"
        varchar     product_name         "お問い合わせ商品名"
        varchar     product_code         "商品コード"
        varchar     message              "お問い合わせ内容"
        timestamptz created_at           "作成日時"
        timestamptz updated_at           "更新日時"
    }
    members {
        bigint  member_id     PK  "会員ID"
        varchar email             "メールアドレス"
        varchar member_status     "会員状態"
    }
    tax_rates {
        bigint      tax_rate_id       PK  "消費税率ID"
        numeric     tax_rate_percent      "消費税率(%)"
        timestamptz effective_start_at    "適用開始日時"
        timestamptz effective_end_at      "適用終了日時"
        boolean     is_active             "有効フラグ"
        timestamptz created_at            "作成日時"
        timestamptz updated_at            "更新日時"
    }
    popular_product_rankings {
        date     ranking_date       PK    "集計日"
        smallint rank               PK    "順位(1〜10)"
        bigint   product_id         FK    "商品ID"
        int      sold_quantity_1m         "直近1か月販売数量"
        timestamptz created_at            "作成日時"
        timestamptz updated_at            "更新日時"
    }
    recommended_related_products {
        date     recommendation_date   PK    "推薦日"
        bigint   source_product_id     PK,FK "基準商品ID"
        smallint rank                  PK    "順位(1〜4)"
        bigint   recommended_product_id FK   "推薦商品ID"
        numeric  score                       "類似度スコア"
        timestamptz created_at               "作成日時"
        timestamptz updated_at               "更新日時"
    }
    products {
        bigint  product_id    PK  "商品ID"
        varchar product_name      "商品名"
        varchar category_id       "商品カテゴリID"
    }

    members  |o--o{ inquiries                        : "問い合わせする"
    products ||--o{ popular_product_rankings         : "ランキングに入る"
    products ||--o{ recommended_related_products     : "関連元になる"
    products ||--o{ recommended_related_products     : "関連先になる"
```
