INSERT INTO
    announcements (
        title,
        summary,
        body,
        published_start_at,
        published_end_at,
        is_active,
        created_at,
        updated_at
    )
VALUES
    (
        '配送遅延のお知らせ',
        '一部地域で配送遅延が発生しています。',
        '天候不良により一部地域で配送遅延が発生しています。',
        NOW() - INTERVAL '3 days',
        NOW() + INTERVAL '14 days',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'コンビニ決済メンテナンス',
        '2月末にコンビニ決済メンテナンスを実施します。',
        '2月28日 01:00-03:00 にメンテナンスを実施します。',
        NOW() - INTERVAL '10 days',
        NOW() + INTERVAL '20 days',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '新商品入荷',
        'デスクシリーズ新商品を追加しました。',
        'デスクカテゴリに新商品を追加しました。',
        NOW() - INTERVAL '5 days',
        NOW() + INTERVAL '30 days',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '年末年始休業',
        '年末年始のサポート窓口休業日のお知らせです。',
        '12/29-1/3は窓口を休業します。',
        NOW() - INTERVAL '60 days',
        NOW() - INTERVAL '20 days',
        FALSE,
        NOW(),
        NOW()
    );

WITH
    active_members AS (
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY
                    member_id
            ) AS rn,
            member_id,
            personal_or_corporate
        FROM
            members
        WHERE
            member_status = 'active'
    ),
    variant_count AS (
        SELECT
            COUNT(*) AS cnt
        FROM
            product_variants
    ),
    base AS (
        SELECT
            s.n,
            CASE
                WHEN s.n % 2 = 0 THEN 'after_order'
                ELSE 'before_order'
            END AS order_phase,
            (
                ARRAY[
                    'product',
                    'delivery_date',
                    'order',
                    'shipping',
                    'return_cancel',
                    'other'
                ]
            ) [((s.n - 1) % 6) + 1] AS inquiry_type,
            CASE
                WHEN s.n <= 12 THEN (
                    SELECT
                        member_id
                    FROM
                        active_members
                    WHERE
                        rn = ((s.n - 1) % 6) + 1
                )
                ELSE NULL
            END AS member_id,
            CASE
                WHEN s.n <= 12 THEN (
                    SELECT
                        personal_or_corporate
                    FROM
                        active_members
                    WHERE
                        rn = ((s.n - 1) % 6) + 1
                )
                ELSE 'personal'
            END AS personal_or_corporate
        FROM
            generate_series(1, 20) AS s (n)
    ),
    picked_variant AS (
        SELECT
            b.n,
            pv.product_code,
            p.product_name
        FROM
            base b
            JOIN LATERAL (
                SELECT
                    pv.*
                FROM
                    product_variants pv
                ORDER BY
                    pv.product_variant_id
                OFFSET
                    (
                        (b.n * 5) % (
                            SELECT
                                cnt
                            FROM
                                variant_count
                        )
                    )
                LIMIT
                    1
            ) pv ON TRUE
            JOIN products p ON p.product_id = pv.product_id
    )
INSERT INTO
    inquiries (
        member_id,
        company_name,
        department_name,
        last_name,
        first_name,
        email,
        phone,
        inquiry_type,
        order_phase,
        product_name,
        product_code,
        message,
        created_at,
        updated_at
    )
SELECT
    b.member_id,
    CASE
        WHEN b.personal_or_corporate = 'corporate' THEN '問い合わせ法人' || LPAD(b.n::TEXT, 2, '0')
        ELSE NULL
    END,
    CASE
        WHEN b.personal_or_corporate = 'corporate' THEN '総務部'
        ELSE NULL
    END,
    '問い合わせ',
    'ユーザー' || LPAD(b.n::TEXT, 2, '0'),
    'inquiry' || LPAD(b.n::TEXT, 2, '0') || '@example.com',
    CASE
        WHEN b.n % 3 = 0 THEN NULL
        ELSE LPAD((8000000000 + b.n)::TEXT, 10, '0')
    END,
    b.inquiry_type,
    b.order_phase,
    pv.product_name,
    pv.product_code,
    'テストお問い合わせ本文 ' || b.n || '（ローカルデモ用）',
    NOW() - MAKE_INTERVAL(days => b.n),
    NOW() - MAKE_INTERVAL(days => b.n)
FROM
    base b
    JOIN picked_variant pv ON pv.n = b.n;
