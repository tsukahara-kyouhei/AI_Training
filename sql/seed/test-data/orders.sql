WITH active_members AS (
    SELECT
        ROW_NUMBER() OVER (ORDER BY member_id) AS rn,
        member_id,
        personal_or_corporate,
        last_name,
        first_name,
        last_name_kana,
        first_name_kana,
        company_name,
        department_name,
        email
    FROM members
    WHERE member_status = 'active'
),
prepared AS (
    SELECT
        s.n,
        CASE
            WHEN s.n <= 55 THEN NOW() - MAKE_INTERVAL(days => ((s.n - 1) % 7), hours => (s.n % 6), mins => (s.n % 50))
            ELSE NOW() - MAKE_INTERVAL(days => (8 + ((s.n - 56) % 23)), hours => (s.n % 6), mins => (s.n % 40))
        END AS order_datetime,
        CASE
            WHEN s.n <= 20 THEN 'received'
            WHEN s.n <= 35 THEN 'awaiting_payment'
            WHEN s.n <= 55 THEN 'processing'
            WHEN s.n <= 95 THEN 'completed'
            ELSE 'cancelled'
        END AS final_status,
        CASE WHEN s.n <= 70 THEN 'member' ELSE 'guest' END AS customer_type,
        am.member_id,
        am.personal_or_corporate AS m_personal_or_corporate,
        am.last_name AS m_last_name,
        am.first_name AS m_first_name,
        am.last_name_kana AS m_last_name_kana,
        am.first_name_kana AS m_first_name_kana,
        am.company_name AS m_company_name,
        am.department_name AS m_department_name,
        am.email AS m_email
    FROM generate_series(1, 100) AS s(n)
    LEFT JOIN active_members am
      ON am.rn = ((s.n - 1) % (SELECT COUNT(*) FROM active_members)) + 1
)
INSERT INTO orders (
    order_number,
    order_datetime,
    order_status,
    customer_type,
    personal_or_corporate,
    member_id,
    customer_last_name,
    customer_first_name,
    customer_last_name_kana,
    customer_first_name_kana,
    company_name,
    department_name,
    customer_email,
    daytime_phone,
    shipping_fax,
    shipping_postal_code,
    shipping_prefecture,
    shipping_city,
    shipping_address_line,
    shipping_floor,
    shipping_has_elevator,
    payment_method,
    payment_instruction,
    shipping_method,
    shipping_fee,
    assembly_fee_total,
    subtotal_amount,
    tax_amount,
    total_amount,
    receipt_issued,
    note,
    created_at,
    updated_at
)
SELECT
    'ORD' || TO_CHAR((p.order_datetime AT TIME ZONE 'Asia/Tokyo')::date, 'YYYYMMDD') || '-' || LPAD(p.n::TEXT, 6, '0'),
    p.order_datetime,
    p.final_status,
    p.customer_type,
    CASE
        WHEN p.customer_type = 'member' THEN p.m_personal_or_corporate
        WHEN p.n % 4 = 0 THEN 'corporate'
        ELSE 'personal'
    END,
    CASE WHEN p.customer_type = 'member' THEN p.member_id ELSE NULL END,
    CASE WHEN p.customer_type = 'member' THEN p.m_last_name ELSE 'ゲスト' || LPAD(p.n::TEXT, 3, '0') END,
    CASE WHEN p.customer_type = 'member' THEN p.m_first_name ELSE '購入者' END,
    CASE WHEN p.customer_type = 'member' THEN p.m_last_name_kana ELSE 'ゲスト' END,
    CASE WHEN p.customer_type = 'member' THEN p.m_first_name_kana ELSE 'コウニュウシャ' END,
    CASE
        WHEN p.customer_type = 'member' THEN p.m_company_name
        WHEN p.n % 4 = 0 THEN 'ゲスト法人' || LPAD(p.n::TEXT, 3, '0')
        ELSE NULL
    END,
    CASE
        WHEN p.customer_type = 'member' THEN p.m_department_name
        WHEN p.n % 4 = 0 THEN '総務部'
        ELSE NULL
    END,
    CASE WHEN p.customer_type = 'member' THEN p.m_email ELSE 'guest' || LPAD(p.n::TEXT, 3, '0') || '@example.com' END,
    LPAD((9000000000 + p.n)::TEXT, 10, '0'),
    CASE WHEN p.n % 3 = 0 THEN NULL ELSE LPAD((3100000000 + p.n)::TEXT, 10, '0') END,
    LPAD((1000000 + p.n)::TEXT, 7, '0'),
    (ARRAY['東京都', '神奈川県', '大阪府', '愛知県', '福岡県'])[ ((p.n - 1) % 5) + 1 ],
    (ARRAY['千代田区', '横浜市西区', '大阪市北区', '名古屋市中区', '福岡市博多区'])[ ((p.n - 1) % 5) + 1 ],
    'サンプル町' || ((p.n % 9) + 1) || '-' || ((p.n % 13) + 1) || '-' || ((p.n % 17) + 1),
    ((p.n % 8) + 1),
    (p.n % 2 = 0),
    CASE
        WHEN p.n % 10 BETWEEN 0 AND 3 THEN 'bank_transfer'
        WHEN p.n % 10 BETWEEN 4 AND 6 THEN 'cash_on_delivery'
        ELSE 'convenience_store'
    END,
    CASE
        WHEN p.n % 10 BETWEEN 7 AND 9 THEN
            jsonb_build_object(
                'payment_no', 'CVS' || LPAD((100000 + p.n)::TEXT, 6, '0'),
                'payment_limit_date', TO_CHAR((p.order_datetime + INTERVAL '3 day') AT TIME ZONE 'Asia/Tokyo', 'YYYY-MM-DD')
            )
        ELSE NULL
    END,
    CASE WHEN p.n % 10 < 7 THEN 'normal' ELSE 'assembly' END,
    0,
    0,
    0,
    0,
    0,
    FALSE,
    NULL,
    p.order_datetime,
    p.order_datetime
FROM prepared p;

WITH variant_count AS (
    SELECT COUNT(*) AS cnt FROM product_variants
),
line_plan AS (
    SELECT
        o.order_id,
        o.shipping_method,
        CASE
            WHEN o.order_id % 3 = 1 THEN 1
            WHEN o.order_id % 3 = 2 THEN 2
            ELSE 3
        END AS item_count
    FROM orders o
),
expanded AS (
    SELECT
        lp.order_id,
        lp.shipping_method,
        gs.idx
    FROM line_plan lp
    CROSS JOIN LATERAL generate_series(1, lp.item_count) AS gs(idx)
),
picked AS (
    SELECT
        e.order_id,
        e.shipping_method,
        e.idx,
        pv.product_code,
        p.product_name,
        c.color_name,
        pv.unit_price,
        p.assembly_available,
        p.assembly_fee AS base_assembly_fee,
        ((e.order_id + e.idx) % 3) + 1 AS quantity
    FROM expanded e
    JOIN LATERAL (
        SELECT pv.*
        FROM product_variants pv
        ORDER BY pv.product_variant_id
        OFFSET ((e.order_id * 7 + e.idx) % (SELECT cnt FROM variant_count))
        LIMIT 1
    ) pv ON TRUE
    JOIN products p ON p.product_id = pv.product_id
    JOIN colors c ON c.color_id = pv.color_id
)
INSERT INTO order_items (
    order_id,
    product_code,
    product_name,
    color_name,
    unit_price,
    quantity,
    line_subtotal,
    line_tax_amount,
    line_total_amount,
    assembly_available,
    assembly_fee,
    created_at,
    updated_at
)
SELECT
    p.order_id,
    p.product_code,
    p.product_name,
    p.color_name,
    p.unit_price,
    p.quantity,
    (p.unit_price * p.quantity)::NUMERIC(12,0),
    FLOOR((p.unit_price * p.quantity) * 0.10)::NUMERIC(12,0),
    (
      (p.unit_price * p.quantity)
      + FLOOR((p.unit_price * p.quantity) * 0.10)
      + CASE
            WHEN p.shipping_method = 'assembly' AND p.assembly_available
                THEN p.base_assembly_fee * p.quantity
            ELSE 0
        END
    )::NUMERIC(12,0),
    p.assembly_available,
    CASE
        WHEN p.shipping_method = 'assembly' AND p.assembly_available THEN p.base_assembly_fee
        ELSE 0
    END::NUMERIC(12,0),
    NOW(),
    NOW()
FROM picked p;

WITH agg AS (
    SELECT
        o.order_id,
        COALESCE(SUM(oi.line_subtotal), 0)::NUMERIC(12,0) AS subtotal_amount,
        COALESCE(SUM(oi.assembly_fee * oi.quantity), 0)::NUMERIC(12,0) AS assembly_fee_total
    FROM orders o
    LEFT JOIN order_items oi ON oi.order_id = o.order_id
    GROUP BY o.order_id
),
calc AS (
    SELECT
        a.order_id,
        a.subtotal_amount,
        a.assembly_fee_total,
        CASE
            WHEN (a.subtotal_amount + a.assembly_fee_total) >= 5000 THEN 0
            ELSE 800
        END::NUMERIC(12,0) AS shipping_fee
    FROM agg a
)
UPDATE orders o
SET
    subtotal_amount = c.subtotal_amount,
    assembly_fee_total = c.assembly_fee_total,
    shipping_fee = c.shipping_fee,
    tax_amount = FLOOR((c.subtotal_amount + c.assembly_fee_total + c.shipping_fee) * 0.10)::NUMERIC(12,0),
    total_amount = (
        c.subtotal_amount
        + c.assembly_fee_total
        + c.shipping_fee
        + FLOOR((c.subtotal_amount + c.assembly_fee_total + c.shipping_fee) * 0.10)
    )::NUMERIC(12,0),
    updated_at = NOW()
FROM calc c
WHERE o.order_id = c.order_id;

INSERT INTO order_status_histories (order_id, status, changed_by_system, changed_at)
SELECT
    o.order_id,
    'received',
    'office-order',
    o.order_datetime
FROM orders o;

INSERT INTO order_status_histories (order_id, status, changed_by_system, changed_at)
SELECT
    o.order_id,
    'awaiting_payment',
    'back-office',
    o.order_datetime + INTERVAL '1 hour'
FROM orders o
WHERE o.order_status IN ('awaiting_payment', 'processing', 'completed', 'cancelled');

INSERT INTO order_status_histories (order_id, status, changed_by_system, changed_at)
SELECT
    o.order_id,
    'processing',
    'back-office',
    o.order_datetime + INTERVAL '2 hour'
FROM orders o
WHERE o.order_status IN ('processing', 'completed');

INSERT INTO order_status_histories (order_id, status, changed_by_system, changed_at)
SELECT
    o.order_id,
    'completed',
    'back-office',
    o.order_datetime + INTERVAL '24 hour'
FROM orders o
WHERE o.order_status = 'completed';

INSERT INTO order_status_histories (order_id, status, changed_by_system, changed_at)
SELECT
    o.order_id,
    'cancelled',
    'back-office',
    o.order_datetime + INTERVAL '6 hour'
FROM orders o
WHERE o.order_status = 'cancelled';

-- order_number_counters を orders の実データから自動計算（新規注文との衝突防止）
-- 各日付ごとに最大連番を記録することで、同日の新規注文採番がシードデータと衝突しない
INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
SELECT
    (order_datetime AT TIME ZONE 'Asia/Tokyo')::date            AS order_date,
    MAX(CAST(SUBSTRING(order_number FROM 13 FOR 6) AS INTEGER)) AS last_sequence,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM orders
WHERE order_number ~ '^ORD[0-9]{8}-[0-9]{6}$'
GROUP BY (order_datetime AT TIME ZONE 'Asia/Tokyo')::date
ON CONFLICT (order_date) DO UPDATE
    SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
