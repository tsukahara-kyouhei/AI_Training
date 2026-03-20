# 会員テーブル定義

## 1. members（会員）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | member_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 会員ID |
| 2 | personal_or_corporate | VARCHAR(10) | NOT NULL | CHECK: 'personal' / 'corporate' | 個人/法人区分 |
| 3 | last_name | VARCHAR(50) | NOT NULL | - | 姓 |
| 4 | first_name | VARCHAR(50) | NOT NULL | - | 名 |
| 5 | last_name_kana | VARCHAR(50) | NOT NULL | - | 姓カナ |
| 6 | first_name_kana | VARCHAR(50) | NOT NULL | - | 名カナ |
| 7 | company_name | VARCHAR(120) | NULL | 法人時必須（CHECK制約） | 会社名 |
| 8 | department_name | VARCHAR(120) | NULL | - | 部署名 |
| 9 | email | VARCHAR(254) | NOT NULL | UNIQUE（小文字化インデックス） | メールアドレス |
| 10 | gender | VARCHAR(10) | NOT NULL | CHECK: 'male' / 'female' / 'no_answer' | 性別 |
| 11 | anniversary_date | DATE | NOT NULL | - | 生年月日/記念日 |
| 12 | password_hash | VARCHAR(255) | NOT NULL | BCryptハッシュ値 | パスワードハッシュ |
| 13 | newsletter_opt_in | BOOLEAN | NOT NULL | DEFAULT FALSE | メールマガジン希望有無 |
| 14 | postal_code | CHAR(7) | NOT NULL | CHECK: 数字7桁 | 郵便番号 |
| 15 | prefecture | VARCHAR(20) | NOT NULL | - | 都道府県 |
| 16 | city | VARCHAR(120) | NOT NULL | - | 市区町村 |
| 17 | address_line | VARCHAR(255) | NOT NULL | - | 番地・ビル名 |
| 18 | delivery_floor | INTEGER | NOT NULL | CHECK: >= 0 | お届け先階数 |
| 19 | has_elevator | BOOLEAN | NOT NULL | - | エレベーター有無 |
| 20 | daytime_phone | VARCHAR(12) | NOT NULL | CHECK: 数字10〜12桁 | 日中連絡可能電話番号 |
| 21 | fax | VARCHAR(12) | NULL | CHECK: 数字10〜12桁（入力時） | FAX |
| 22 | member_status | VARCHAR(10) | NOT NULL | DEFAULT 'active', CHECK: 'active' / 'withdrawn' | 会員状態 |
| 23 | withdrawn_at | TIMESTAMPTZ | NULL | 退会時に設定 | 退会日時 |
| 24 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 25 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**インデックス:**

| インデックス名 | 列 | 種別 |
|---|---|---|
| uq_members_email_lower | LOWER(email) | UNIQUE |
| idx_members_status | member_status | 通常 |
| idx_members_created_at | created_at DESC | 通常 |

---

## 2. member_additional_addresses（会員追加お届け先）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | member_address_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 会員追加お届け先ID |
| 2 | member_id | BIGINT | NOT NULL | FK → members.member_id (CASCADE) | 会員ID |
| 3 | last_name | VARCHAR(50) | NOT NULL | - | 姓 |
| 4 | first_name | VARCHAR(50) | NOT NULL | - | 名 |
| 5 | last_name_kana | VARCHAR(50) | NOT NULL | - | 姓カナ |
| 6 | first_name_kana | VARCHAR(50) | NOT NULL | - | 名カナ |
| 7 | company_name | VARCHAR(120) | NULL | - | 会社名 |
| 8 | department_name | VARCHAR(120) | NULL | - | 部署名 |
| 9 | postal_code | CHAR(7) | NOT NULL | CHECK: 数字7桁 | 郵便番号 |
| 10 | prefecture | VARCHAR(20) | NOT NULL | - | 都道府県 |
| 11 | city | VARCHAR(120) | NOT NULL | - | 市区町村 |
| 12 | address_line | VARCHAR(255) | NOT NULL | - | 番地・ビル名 |
| 13 | delivery_floor | INTEGER | NOT NULL | CHECK: >= 0 | お届け先階数 |
| 14 | has_elevator | BOOLEAN | NOT NULL | - | エレベーター有無 |
| 15 | daytime_phone | VARCHAR(12) | NOT NULL | CHECK: 数字10〜12桁 | 日中連絡可能電話番号 |
| 16 | fax | VARCHAR(12) | NULL | CHECK: 数字10〜12桁（入力時） | FAX |
| 17 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 18 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**制約:** 1会員あたり最大20件（アプリ層で制御）

---

## 3. member_favorites（会員お気に入り）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | member_id | BIGINT | NOT NULL | PK, FK → members.member_id (CASCADE) | 会員ID |
| 2 | product_id | BIGINT | NOT NULL | PK, FK → products.product_id (CASCADE) | 商品ID |
| 3 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 4 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**制約:** 1会員あたり最大100件（アプリ層で制御）
