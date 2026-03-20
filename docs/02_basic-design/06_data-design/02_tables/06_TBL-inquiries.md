# 問い合わせテーブル定義

## 1. inquiries（お問い合わせ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | inquiry_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | お問い合わせID |
| 2 | inquiry_type | VARCHAR(32) | NOT NULL | CHECK: product/order/delivery/return/other_before/other_after | お問い合わせ種別 |
| 3 | order_phase | VARCHAR(20) | NOT NULL | CHECK: 'before_order' / 'after_order' | 注文状況区分 |
| 4 | last_name | VARCHAR(50) | NOT NULL | - | 姓 |
| 5 | first_name | VARCHAR(50) | NOT NULL | - | 名 |
| 6 | email | VARCHAR(254) | NOT NULL | - | メールアドレス |
| 7 | product_name | VARCHAR(255) | NULL | - | お問い合わせ商品名 |
| 8 | product_code | VARCHAR(32) | NULL | - | 商品コード |
| 9 | message | TEXT | NOT NULL | 最大1000文字（アプリ層） | お問い合わせ内容 |
| 10 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 11 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |
