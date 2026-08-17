-- クーポンマスタテーブル
CREATE TABLE coupons (
    coupon_id BIGSERIAL PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(10) NOT NULL CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_value DECIMAL(10, 2) NOT NULL CHECK (discount_value > 0),
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    min_purchase_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (min_purchase_amount >= 0),
    usage_limit INT NOT NULL DEFAULT 1 CHECK (usage_limit > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_period_check CHECK (valid_from < valid_to)
);

CREATE INDEX idx_coupons_coupon_code ON coupons(coupon_code);
CREATE INDEX idx_coupons_is_active ON coupons(is_active);

-- 会員別利用実績テーブル
CREATE TABLE member_coupon_usage (
    usage_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL REFERENCES coupons(coupon_id) ON DELETE CASCADE,
    usage_count INT NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (member_id, coupon_id)
);

CREATE INDEX idx_member_coupon_usage_member_id ON member_coupon_usage(member_id);
CREATE INDEX idx_member_coupon_usage_coupon_id ON member_coupon_usage(coupon_id);
