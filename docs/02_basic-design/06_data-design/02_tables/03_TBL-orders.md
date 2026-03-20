# 注文テーブル定義

## 1. orders（注文）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | order_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 注文ID |
| 2 | order_number | VARCHAR(20) | NOT NULL | UNIQUE, YYYYMMDD+連番 | 注文番号 |
| 3 | order_datetime | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 注文日時 |
| 4 | order_status | VARCHAR(20) | NOT NULL | DEFAULT 'received', CHECK: received/awaiting_payment/processing/completed/cancelled | 注文ステータス |
| 5 | customer_type | VARCHAR(10) | NOT NULL | CHECK: 'guest' / 'member' | 顧客種別 |
| 6 | personal_or_corporate | VARCHAR(10) | NOT NULL | CHECK: 'personal' / 'corporate' | 個人/法人区分 |
| 7 | member_id | BIGINT | NULL | FK → members.member_id (SET NULL), member時必須 | 会員ID |
| 8 | customer_last_name | VARCHAR(50) | NOT NULL | - | 注文者姓 |
| 9 | customer_first_name | VARCHAR(50) | NOT NULL | - | 注文者名 |
| 10 | customer_last_name_kana | VARCHAR(50) | NOT NULL | - | 注文者姓カナ |
| 11 | customer_first_name_kana | VARCHAR(50) | NOT NULL | - | 注文者名カナ |
| 12 | company_name | VARCHAR(120) | NULL | 法人時必須 | 会社名 |
| 13 | department_name | VARCHAR(120) | NULL | - | 部署名 |
| 14 | customer_email | VARCHAR(254) | NOT NULL | - | 注文者メールアドレス |
| 15 | daytime_phone | VARCHAR(12) | NOT NULL | CHECK: 数字10〜12桁 | 日中連絡可能電話番号 |
| 16 | shipping_fax | VARCHAR(12) | NULL | CHECK: 数字10〜12桁 | 届け先FAX |
| 17 | shipping_postal_code | CHAR(7) | NOT NULL | CHECK: 数字7桁 | 届け先郵便番号 |
| 18 | shipping_prefecture | VARCHAR(20) | NOT NULL | - | 届け先都道府県 |
| 19 | shipping_city | VARCHAR(120) | NOT NULL | - | 届け先市区町村 |
| 20 | shipping_address_line | VARCHAR(255) | NOT NULL | - | 届け先番地・ビル名 |
| 21 | shipping_floor | INTEGER | NOT NULL | CHECK >= 0 | 届け先階数 |
| 22 | shipping_has_elevator | BOOLEAN | NOT NULL | - | 届け先エレベーター有無 |
| 23 | payment_method | VARCHAR(20) | NOT NULL | CHECK: bank_transfer/cash_on_delivery/convenience_store | 支払方法 |
| 24 | payment_instruction | JSONB | NULL | コンビニ決済時のみ設定 | 決済情報 |
| 25 | shipping_method | VARCHAR(20) | NOT NULL | CHECK: 'normal' / 'assembly' | 配送方法 |
| 26 | shipping_fee | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 送料 |
| 27 | assembly_fee_total | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 組立費合計 |
| 28 | subtotal_amount | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 小計金額 |
| 29 | tax_amount | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 消費税額 |
| 30 | total_amount | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 合計金額 |
| 31 | receipt_issued | BOOLEAN | NOT NULL | DEFAULT FALSE | 領収書発行済フラグ |
| 32 | note | VARCHAR(500) | NULL | - | 備考 |
| 33 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 34 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

---

## 2. order_items（注文明細）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | order_item_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 注文明細ID |
| 2 | order_id | BIGINT | NOT NULL | FK → orders.order_id | 注文ID |
| 3 | product_code | VARCHAR(32) | NOT NULL | 注文時点のコード（スナップショット） | 商品コード |
| 4 | product_name | VARCHAR(255) | NOT NULL | 注文時点の商品名（スナップショット） | 商品名 |
| 5 | color_name | VARCHAR(40) | NOT NULL | 注文時点のカラー名（スナップショット） | カラー名 |
| 6 | unit_price | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 単価 |
| 7 | quantity | INTEGER | NOT NULL | DEFAULT 1, CHECK: 1〜99 | 数量 |
| 8 | line_subtotal | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 明細小計 |
| 9 | line_tax_amount | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 明細消費税額 |
| 10 | line_total_amount | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 明細合計 |
| 11 | assembly_available | BOOLEAN | NOT NULL | DEFAULT FALSE | 組立・設置可否 |
| 12 | assembly_fee | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 組立・設置費 |
| 13 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 14 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

> **注意:** 商品名・カラー名は注文時点の値をスナップショットとして保持する。商品マスタへの参照は持たない。

---

## 3. order_number_counters（注文番号採番カウンタ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | order_date | DATE | NOT NULL | PK | 注文日 |
| 2 | last_sequence | INTEGER | NOT NULL | CHECK >= 0 | 最終シーケンス番号 |
| 3 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 4 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

> **注文番号の採番ルール:** `YYYYMMDD` + ゼロ埋め連番（例: `20260101-0001`）

---

## 4. order_status_histories（注文ステータス履歴）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | order_status_history_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 履歴ID |
| 2 | order_id | BIGINT | NOT NULL | FK → orders.order_id | 注文ID |
| 3 | status | VARCHAR(20) | NOT NULL | CHECK: received/awaiting_payment/processing/completed/cancelled | ステータス |
| 4 | changed_by_system | VARCHAR(40) | NOT NULL | - | 変更実行システム名 |
| 5 | changed_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 変更日時 |
