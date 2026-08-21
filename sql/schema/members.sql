CREATE TABLE IF NOT EXISTS members (
    member_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    personal_or_corporate VARCHAR(10) NOT NULL CHECK (personal_or_corporate IN ('personal', 'corporate')),
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    company_name VARCHAR(120),
    department_name VARCHAR(120),
    email VARCHAR(254) NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('male', 'female', 'no_answer')),
    anniversary_date DATE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    newsletter_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    postal_code CHAR(7) NOT NULL CHECK (postal_code ~ '^[0-9]{7}$'),
    prefecture VARCHAR(20) NOT NULL,
    city VARCHAR(120) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    delivery_floor INTEGER NOT NULL CHECK (delivery_floor >= 0),
    has_elevator BOOLEAN NOT NULL,
    daytime_phone VARCHAR(12) NOT NULL CHECK (daytime_phone ~ '^[0-9]{10,12}$'),
    fax VARCHAR(12) CHECK (fax IS NULL OR fax ~ '^[0-9]{10,12}$'),
    member_status VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (member_status IN ('active', 'withdrawn')),
    withdrawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (personal_or_corporate <> 'corporate' OR company_name IS NOT NULL),
    CHECK (member_status <> 'withdrawn' OR withdrawn_at IS NOT NULL)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_members_email_lower ON members ((LOWER(email)));
CREATE INDEX IF NOT EXISTS idx_members_status ON members (member_status);
CREATE INDEX IF NOT EXISTS idx_members_created_at ON members (created_at DESC);

CREATE TABLE IF NOT EXISTS member_additional_addresses (
    member_address_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    company_name VARCHAR(120),
    department_name VARCHAR(120),
    postal_code CHAR(7) NOT NULL CHECK (postal_code ~ '^[0-9]{7}$'),
    prefecture VARCHAR(20) NOT NULL,
    city VARCHAR(120) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    delivery_floor INTEGER NOT NULL CHECK (delivery_floor >= 0),
    has_elevator BOOLEAN NOT NULL,
    daytime_phone VARCHAR(12) NOT NULL CHECK (daytime_phone ~ '^[0-9]{10,12}$'),
    fax VARCHAR(12) CHECK (fax IS NULL OR fax ~ '^[0-9]{10,12}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_additional_addresses_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_member_additional_addresses_member_created
    ON member_additional_addresses (member_id, created_at ASC);

CREATE TABLE IF NOT EXISTS product_review (
    review_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_review_member_product UNIQUE (member_id, product_id),
    CONSTRAINT fk_product_review_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_review_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_product_review_product_created
    ON product_review (product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_review_member_created
    ON product_review (member_id, created_at DESC);

-- logical comments
COMMENT ON TABLE members IS '会員';
COMMENT ON COLUMN members.member_id IS '会員ID';
COMMENT ON COLUMN members.personal_or_corporate IS '個人/法人区分';
COMMENT ON COLUMN members.last_name IS '姓';
COMMENT ON COLUMN members.first_name IS '名';
COMMENT ON COLUMN members.last_name_kana IS '姓カナ';
COMMENT ON COLUMN members.first_name_kana IS '名カナ';
COMMENT ON COLUMN members.company_name IS '会社名';
COMMENT ON COLUMN members.department_name IS '部署名';
COMMENT ON COLUMN members.email IS 'メールアドレス';
COMMENT ON COLUMN members.gender IS '性別';
COMMENT ON COLUMN members.anniversary_date IS '生年月日/記念日';
COMMENT ON COLUMN members.password_hash IS 'パスワードハッシュ';
COMMENT ON COLUMN members.newsletter_opt_in IS 'メールマガジン希望有無';
COMMENT ON COLUMN members.postal_code IS '郵便番号';
COMMENT ON COLUMN members.prefecture IS '都道府県';
COMMENT ON COLUMN members.city IS '市区町村';
COMMENT ON COLUMN members.address_line IS '番地・ビル名';
COMMENT ON COLUMN members.delivery_floor IS 'お届け先階数';
COMMENT ON COLUMN members.has_elevator IS 'エレベーター有無';
COMMENT ON COLUMN members.daytime_phone IS '日中連絡可能電話番号';
COMMENT ON COLUMN members.fax IS 'FAX';
COMMENT ON COLUMN members.member_status IS '会員状態';
COMMENT ON COLUMN members.withdrawn_at IS '退会日時';
COMMENT ON COLUMN members.created_at IS '作成日時';
COMMENT ON COLUMN members.updated_at IS '更新日時';

COMMENT ON TABLE member_additional_addresses IS '会員追加お届け先';
COMMENT ON COLUMN member_additional_addresses.member_address_id IS '会員追加お届け先ID';
COMMENT ON COLUMN member_additional_addresses.member_id IS '会員ID';
COMMENT ON COLUMN member_additional_addresses.last_name IS '姓';
COMMENT ON COLUMN member_additional_addresses.first_name IS '名';
COMMENT ON COLUMN member_additional_addresses.last_name_kana IS '姓カナ';
COMMENT ON COLUMN member_additional_addresses.first_name_kana IS '名カナ';
COMMENT ON COLUMN member_additional_addresses.company_name IS '会社名';
COMMENT ON COLUMN member_additional_addresses.department_name IS '部署名';
COMMENT ON COLUMN member_additional_addresses.postal_code IS '郵便番号';
COMMENT ON COLUMN member_additional_addresses.prefecture IS '都道府県';
COMMENT ON COLUMN member_additional_addresses.city IS '市区町村';
COMMENT ON COLUMN member_additional_addresses.address_line IS '番地・ビル名';
COMMENT ON COLUMN member_additional_addresses.delivery_floor IS 'お届け先階数';
COMMENT ON COLUMN member_additional_addresses.has_elevator IS 'エレベーター有無';
COMMENT ON COLUMN member_additional_addresses.daytime_phone IS '日中連絡可能電話番号';
COMMENT ON COLUMN member_additional_addresses.fax IS 'FAX';
COMMENT ON COLUMN member_additional_addresses.created_at IS '作成日時';
COMMENT ON COLUMN member_additional_addresses.updated_at IS '更新日時';

COMMENT ON TABLE product_review IS '商品レビュー';
COMMENT ON COLUMN product_review.review_id IS 'レビューID';
COMMENT ON COLUMN product_review.member_id IS '会員ID';
COMMENT ON COLUMN product_review.product_id IS '商品ID';
COMMENT ON COLUMN product_review.rating IS '評価';
COMMENT ON COLUMN product_review.title IS 'タイトル';
COMMENT ON COLUMN product_review.body IS '本文';
COMMENT ON COLUMN product_review.published IS '公開状態';
COMMENT ON COLUMN product_review.created_at IS '作成日時';
COMMENT ON COLUMN product_review.updated_at IS '更新日時';
