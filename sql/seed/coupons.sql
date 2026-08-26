-- テスト用クーポンデータの挿入例
INSERT INTO coupons (
    coupon_code, 
    discount_type, 
    discount_value, 
    min_purchase_amount, 
    starts_at, 
    expires_at, 
    usage_limit_total, 
    usage_limit_per_customer, 
    is_active
) VALUES 
-- 1. 定額割引クーポン（1,000円引き、最低購入金額30,000円以上、2030年まで有効）
('SUMMER2030', 'fixed', 1000, 30000, '2026-01-01 00:00:00+09', '2030-12-31 23:59:59+09', 100, 1, TRUE),

-- 2. 定率割引クーポン（10%OFF、最低購入金額なし、2030年まで有効）
('WELCOME10', 'percentage', 10, 0, '2026-01-01 00:00:00+09', '2030-12-31 23:59:59+09', NULL, 1, TRUE);