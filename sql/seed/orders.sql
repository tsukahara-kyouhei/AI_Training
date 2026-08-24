\ir test-data/orders.sql
-- 会員1による商品35の購入完了テストデータ
INSERT INTO orders (
    order_id, order_number, order_status, customer_type, personal_or_corporate,
    member_id, customer_last_name, customer_first_name, customer_last_name_kana,
    customer_first_name_kana, customer_email, daytime_phone, shipping_postal_code,
    shipping_prefecture, shipping_city, shipping_address_line, shipping_floor,
    shipping_has_elevator, payment_method, shipping_method,
    subtotal_amount, tax_amount, total_amount
) VALUES (
    999, 'ORD-TEST-999', 'completed', 'member', 'personal',
    1, 'テスト', '太郎', 'テスト', 'タロウ', 'test@example.com',
    '09012345678', '1000001', '東京都', '千代田区', '千代田1-1', 1, true,
    'bank_transfer', 'normal', 98000, 9800, 107800
) ON CONFLICT (order_id) DO NOTHING;

INSERT INTO order_items (
    order_id, product_code, product_name, color_name, unit_price, quantity, line_subtotal, line_tax_amount, line_total_amount
) VALUES (
    999, 'P0035-C01', 'Clave エグゼクティブチェア', '黒系', 98000, 1, 98000, 9800, 107800
);