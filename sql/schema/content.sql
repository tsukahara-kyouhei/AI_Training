CREATE TABLE IF NOT EXISTS announcements (
    announcement_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    body TEXT,
    published_start_at TIMESTAMPTZ NOT NULL,
    published_end_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        published_end_at IS NULL
        OR published_end_at > published_start_at
    )
);

CREATE INDEX IF NOT EXISTS idx_announcements_active_period ON announcements (published_start_at, published_end_at)
WHERE
    is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON announcements (created_at DESC);

CREATE TABLE IF NOT EXISTS inquiries (
    inquiry_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT,
    company_name VARCHAR(120),
    department_name VARCHAR(120),
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(12) CHECK (
        phone IS NULL
        OR phone ~ '^[0-9]{10,12}$'
    ),
    inquiry_type VARCHAR(20) NOT NULL CHECK (
        inquiry_type IN (
            'product',
            'delivery_date',
            'order',
            'shipping',
            'return_cancel',
            'other'
        )
    ),
    order_phase VARCHAR(20) NOT NULL CHECK (order_phase IN ('before_order', 'after_order')),
    product_name VARCHAR(255),
    product_code VARCHAR(32),
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inquiries_member FOREIGN KEY (member_id) REFERENCES members (member_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_inquiries_created_at ON inquiries (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inquiries_member_created_at ON inquiries (member_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inquiries_type_phase ON inquiries (inquiry_type, order_phase);

-- logical comments
COMMENT ON TABLE announcements IS 'お知らせ';

COMMENT ON COLUMN announcements.announcement_id IS 'お知らせID';

COMMENT ON COLUMN announcements.title IS 'タイトル';

COMMENT ON COLUMN announcements.summary IS '要約';

COMMENT ON COLUMN announcements.body IS '本文';

COMMENT ON COLUMN announcements.published_start_at IS '掲載開始日時';

COMMENT ON COLUMN announcements.published_end_at IS '掲載終了日時';

COMMENT ON COLUMN announcements.is_active IS '有効フラグ';

COMMENT ON COLUMN announcements.created_at IS '作成日時';

COMMENT ON COLUMN announcements.updated_at IS '更新日時';

COMMENT ON TABLE inquiries IS 'お問い合わせ';

COMMENT ON COLUMN inquiries.inquiry_id IS 'お問い合わせID';

COMMENT ON COLUMN inquiries.member_id IS '会員ID';

COMMENT ON COLUMN inquiries.company_name IS '会社名';

COMMENT ON COLUMN inquiries.department_name IS '部署名';

COMMENT ON COLUMN inquiries.last_name IS '姓';

COMMENT ON COLUMN inquiries.first_name IS '名';

COMMENT ON COLUMN inquiries.email IS 'メールアドレス';

COMMENT ON COLUMN inquiries.phone IS '電話番号';

COMMENT ON COLUMN inquiries.inquiry_type IS 'お問い合わせ種別';

COMMENT ON COLUMN inquiries.order_phase IS '注文状況';

COMMENT ON COLUMN inquiries.product_name IS 'お問い合わせ商品名';

COMMENT ON COLUMN inquiries.product_code IS '商品コード';

COMMENT ON COLUMN inquiries.message IS 'お問い合わせ内容';

COMMENT ON COLUMN inquiries.created_at IS '作成日時';

COMMENT ON COLUMN inquiries.updated_at IS '更新日時';
