CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    order_datetime TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_status VARCHAR(20) NOT NULL DEFAULT 'received' CHECK (
        order_status IN (
            'received',
            'awaiting_payment',
            'processing',
            'completed',
            'cancelled'
        )
    ),
    customer_type VARCHAR(10) NOT NULL CHECK (customer_type IN ('guest', 'member')),
    personal_or_corporate VARCHAR(10) NOT NULL CHECK (
        personal_or_corporate IN ('personal', 'corporate')
    ),
    member_id BIGINT,
    customer_last_name VARCHAR(50) NOT NULL,
    customer_first_name VARCHAR(50) NOT NULL,
    customer_last_name_kana VARCHAR(50) NOT NULL,
    customer_first_name_kana VARCHAR(50) NOT NULL,
    company_name VARCHAR(120),
    department_name VARCHAR(120),
    customer_email VARCHAR(254) NOT NULL,
    daytime_phone VARCHAR(12) NOT NULL CHECK (daytime_phone ~ '^[0-9]{10,12}$'),
    shipping_fax VARCHAR(12) CHECK (
        shipping_fax IS NULL
        OR shipping_fax ~ '^[0-9]{10,12}$'
    ),
    shipping_postal_code CHAR(7) NOT NULL CHECK (shipping_postal_code ~ '^[0-9]{7}$'),
    shipping_prefecture VARCHAR(20) NOT NULL,
    shipping_city VARCHAR(120) NOT NULL,
    shipping_address_line VARCHAR(255) NOT NULL,
    shipping_floor INTEGER NOT NULL CHECK (shipping_floor >= 0),
    shipping_has_elevator BOOLEAN NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (
        payment_method IN (
            'bank_transfer',
            'cash_on_delivery',
            'convenience_store'
        )
    ),
    payment_instruction JSONB,
    shipping_method VARCHAR(20) NOT NULL CHECK (shipping_method IN ('normal', 'assembly')),
    shipping_fee NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    assembly_fee_total NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (assembly_fee_total >= 0),
    subtotal_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (subtotal_amount >= 0),
    tax_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    receipt_issued BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES members (member_id) ON DELETE SET NULL,
    CHECK (
        (
            customer_type = 'member'
            AND member_id IS NOT NULL
        )
        OR (
            customer_type = 'guest'
            AND member_id IS NULL
        )
    ),
    CHECK (
        personal_or_corporate <> 'corporate'
        OR company_name IS NOT NULL
    ),
    CHECK (
        payment_instruction IS NULL
        OR jsonb_typeof(payment_instruction) = 'object'
    ),
    CHECK (
        (
            payment_method = 'convenience_store'
            AND payment_instruction IS NOT NULL
        )
        OR (
            payment_method <> 'convenience_store'
            AND payment_instruction IS NULL
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_orders_member_datetime ON orders (member_id, order_datetime DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_datetime ON orders (order_status, order_datetime DESC);

CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at DESC);

CREATE TABLE IF NOT EXISTS order_number_counters (
    order_date DATE PRIMARY KEY,
    last_sequence INTEGER NOT NULL CHECK (last_sequence >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    color_name VARCHAR(40) NOT NULL,
    unit_price NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 99),
    line_subtotal NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (line_subtotal >= 0),
    line_tax_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (line_tax_amount >= 0),
    line_total_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (line_total_amount >= 0),
    assembly_available BOOLEAN NOT NULL DEFAULT FALSE,
    assembly_fee NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (assembly_fee >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product_code ON order_items (product_code);

CREATE TABLE IF NOT EXISTS order_status_histories (
    order_status_history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (
        status IN (
            'received',
            'awaiting_payment',
            'processing',
            'completed',
            'cancelled'
        )
    ),
    changed_by_system VARCHAR(40) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_histories_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status_histories_order_changed_at ON order_status_histories (order_id, changed_at DESC);

-- logical comments
COMMENT ON TABLE orders IS '注文';

COMMENT ON COLUMN orders.order_id IS '注文ID';

COMMENT ON COLUMN orders.order_number IS '注文番号';

COMMENT ON COLUMN orders.order_datetime IS '注文日時';

COMMENT ON COLUMN orders.order_status IS '注文ステータス';

COMMENT ON COLUMN orders.customer_type IS '顧客種別';

COMMENT ON COLUMN orders.personal_or_corporate IS '個人/法人区分';

COMMENT ON COLUMN orders.member_id IS '会員ID';

COMMENT ON COLUMN orders.customer_last_name IS '注文者姓';

COMMENT ON COLUMN orders.customer_first_name IS '注文者名';

COMMENT ON COLUMN orders.customer_last_name_kana IS '注文者姓カナ';

COMMENT ON COLUMN orders.customer_first_name_kana IS '注文者名カナ';

COMMENT ON COLUMN orders.company_name IS '会社名';

COMMENT ON COLUMN orders.department_name IS '部署名';

COMMENT ON COLUMN orders.customer_email IS '注文者メールアドレス';

COMMENT ON COLUMN orders.daytime_phone IS '日中連絡可能電話番号';

COMMENT ON COLUMN orders.shipping_fax IS '配送先FAX';

COMMENT ON COLUMN orders.shipping_postal_code IS '配送先郵便番号';

COMMENT ON COLUMN orders.shipping_prefecture IS '配送先都道府県';

COMMENT ON COLUMN orders.shipping_city IS '配送先市区町村';

COMMENT ON COLUMN orders.shipping_address_line IS '配送先番地・ビル名';

COMMENT ON COLUMN orders.shipping_floor IS '配送先階数';

COMMENT ON COLUMN orders.shipping_has_elevator IS '配送先エレベーター有無';

COMMENT ON COLUMN orders.payment_method IS '決済手段';

COMMENT ON COLUMN orders.payment_instruction IS '決済案内情報';

COMMENT ON COLUMN orders.shipping_method IS '配送方法区分';

COMMENT ON COLUMN orders.shipping_fee IS '送料';

COMMENT ON COLUMN orders.assembly_fee_total IS '組立・設置費合計';

COMMENT ON COLUMN orders.subtotal_amount IS '商品小計';

COMMENT ON COLUMN orders.tax_amount IS '消費税額';

COMMENT ON COLUMN orders.total_amount IS '合計金額';

COMMENT ON COLUMN orders.receipt_issued IS '領収書発行フラグ';

COMMENT ON COLUMN orders.note IS '備考';

COMMENT ON COLUMN orders.created_at IS '作成日時';

COMMENT ON COLUMN orders.updated_at IS '更新日時';

COMMENT ON TABLE order_number_counters IS '注文番号採番カウンタ';

COMMENT ON COLUMN order_number_counters.order_date IS '注文日';

COMMENT ON COLUMN order_number_counters.last_sequence IS '最終連番';

COMMENT ON COLUMN order_number_counters.created_at IS '作成日時';

COMMENT ON COLUMN order_number_counters.updated_at IS '更新日時';

COMMENT ON TABLE order_items IS '注文明細';

COMMENT ON COLUMN order_items.order_item_id IS '注文明細ID';

COMMENT ON COLUMN order_items.order_id IS '注文ID';

COMMENT ON COLUMN order_items.product_code IS '商品コード';

COMMENT ON COLUMN order_items.product_name IS '商品名';

COMMENT ON COLUMN order_items.color_name IS 'カラー名';

COMMENT ON COLUMN order_items.unit_price IS '単価';

COMMENT ON COLUMN order_items.quantity IS '数量';

COMMENT ON COLUMN order_items.line_subtotal IS '明細小計';

COMMENT ON COLUMN order_items.line_tax_amount IS '明細消費税額';

COMMENT ON COLUMN order_items.line_total_amount IS '明細合計金額';

COMMENT ON COLUMN order_items.assembly_available IS '組立・設置可否';

COMMENT ON COLUMN order_items.assembly_fee IS '組立・設置費';

COMMENT ON COLUMN order_items.created_at IS '作成日時';

COMMENT ON COLUMN order_items.updated_at IS '更新日時';

COMMENT ON TABLE order_status_histories IS '注文ステータス履歴';

COMMENT ON COLUMN order_status_histories.order_status_history_id IS '注文ステータス履歴ID';

COMMENT ON COLUMN order_status_histories.order_id IS '注文ID';

COMMENT ON COLUMN order_status_histories.status IS 'ステータス';

COMMENT ON COLUMN order_status_histories.changed_by_system IS '変更元システム';

COMMENT ON COLUMN order_status_histories.changed_at IS '変更日時';
