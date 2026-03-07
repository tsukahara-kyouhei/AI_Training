-- データ件数/分布の簡易確認クエリ（初期化ログ確認用）
\echo '--- post seed checks ---'
SELECT COUNT(*) AS members_count FROM members;
SELECT COUNT(*) AS products_count FROM products;
SELECT COUNT(*) AS product_variants_count FROM product_variants;
SELECT COUNT(*) AS orders_count FROM orders;
SELECT order_status, COUNT(*) AS count_by_status FROM orders GROUP BY order_status ORDER BY order_status;
SELECT COUNT(*) AS inquiries_count FROM inquiries;
\echo '--- end post seed checks ---'
