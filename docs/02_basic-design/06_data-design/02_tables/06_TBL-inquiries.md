# 問い合わせテーブル定義

## 1. inquiries（お問い合わせ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | inquiry_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | お問い合わせID |
| 2 | member_id | BIGINT | NULL | FK → members.member_id (SET NULL) | 会員ID |
| 3 | company_name | VARCHAR(120) | NULL | - | 会社名 |
| 4 | department_name | VARCHAR(120) | NULL | - | 部署名 |
| 5 | last_name | VARCHAR(50) | NOT NULL | - | 姓 |
| 6 | first_name | VARCHAR(50) | NOT NULL | - | 名 |
| 7 | email | VARCHAR(254) | NOT NULL | - | メールアドレス |
| 8 | phone | VARCHAR(12) | NULL | CHECK: 数字10〜12桁 | 電話番号 |
| 9 | inquiry_type | VARCHAR(20) | NOT NULL | CHECK: product/delivery_date/order/shipping/return_cancel/other | お問い合わせ種別 |
| 10 | order_phase | VARCHAR(20) | NOT NULL | CHECK: 'before_order' / 'after_order' | 注文状況区分 |
| 11 | product_name | VARCHAR(255) | NULL | - | お問い合わせ商品名 |
| 12 | product_code | VARCHAR(32) | NULL | - | 商品コード |
| 13 | message | VARCHAR(1000) | NOT NULL | - | お問い合わせ内容 |
| 14 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 15 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |
