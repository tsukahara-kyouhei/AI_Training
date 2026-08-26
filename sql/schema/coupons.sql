-- クーポンマスタ
CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- 'amount' (定額) または 'rate' (定率)
    discount_value INT NOT NULL,      -- 割引額 または 割引率(%)
    min_order_amount INT DEFAULT 0,    -- 最小購入金額
    start_at TIMESTAMP NULL,           -- 有効期間(開始)
    end_at TIMESTAMP NULL,             -- 有効期間(終了)
    usage_limit INT NULL,              -- 全体の利用上限回数
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 会員別クーポン利用履歴
CREATE TABLE IF NOT EXISTS coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);