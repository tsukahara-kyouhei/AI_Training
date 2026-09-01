-- =====================================================================
-- 1. クーポンマスタテーブル
-- =====================================================================
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('fixed', 'percentage')),
    discount_value NUMERIC(12, 0) NOT NULL CHECK (discount_value > 0),
    min_purchase_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (min_purchase_amount >= 0),
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    usage_limit_total INTEGER CHECK (
        usage_limit_total IS NULL
        OR usage_limit_total > 0
    ),
    usage_limit_per_customer INTEGER CHECK (
        usage_limit_per_customer IS NULL
        OR usage_limit_per_customer > 0
    ),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_at < expires_at)
);

CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons (coupon_code);

CREATE INDEX IF NOT EXISTS idx_coupons_active_period ON coupons (is_active, starts_at, expires_at);

-- =====================================================================
-- 2. 会員別クーポン利用実績テーブル
-- =====================================================================
CREATE TABLE IF NOT EXISTS customer_coupon_usages (
    customer_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id, coupon_id),
    CONSTRAINT fk_customer_coupon_usages_member FOREIGN KEY (customer_id) REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (coupon_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_coupon_usages_coupon_id ON customer_coupon_usages (coupon_id);

-- =====================================================================
-- 3. 既存の orders テーブルへのカラム追加（スナップショット用）
-- =====================================================================
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS applied_coupon_code VARCHAR(50);

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12, 0) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0);

-- =====================================================================
-- 4. 論理名コメントの追加
-- =====================================================================
COMMENT ON TABLE coupons IS 'クーポンマスタ';

COMMENT ON COLUMN coupons.coupon_id IS 'クーポンID';

COMMENT ON COLUMN coupons.coupon_code IS 'クーポンコード';

COMMENT ON COLUMN coupons.discount_type IS '割引タイプ (fixed: 定額, percentage: 定率)';

COMMENT ON COLUMN coupons.discount_value IS '割引値 (金額またはパーセンテージ)';

COMMENT ON COLUMN coupons.min_purchase_amount IS '最低購入金額';

COMMENT ON COLUMN coupons.starts_at IS '利用開始日時';

COMMENT ON COLUMN coupons.expires_at IS '利用終了日時';

COMMENT ON COLUMN coupons.usage_limit_total IS '全体利用回数上限';

COMMENT ON COLUMN coupons.usage_limit_per_customer IS '会員別利用回数上限';

COMMENT ON COLUMN coupons.is_active IS '有効フラグ';

COMMENT ON TABLE customer_coupon_usages IS '会員別クーポン利用実績';

COMMENT ON COLUMN customer_coupon_usages.customer_id IS '会員ID';

COMMENT ON COLUMN customer_coupon_usages.coupon_id IS 'クーポンID';

COMMENT ON COLUMN customer_coupon_usages.usage_count IS '利用回数';

COMMENT ON COLUMN customer_coupon_usages.last_used_at IS '最終利用日時';

COMMENT ON COLUMN orders.applied_coupon_code IS '適用クーポンコード（スナップショット）';

COMMENT ON COLUMN orders.discount_amount IS 'クーポン割引額';