-- FEAT-001: キーワード検索性能改善のための GIN インデックス追加
-- pg_trgm 拡張を使用した ILIKE 中間一致検索の高速化

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- description（必須: 長文テキスト、B-tree では ILIKE 非対応）
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING gin (description gin_trgm_ops);

-- product_name（推奨: 既存 ILIKE 検索の高速化）
CREATE INDEX IF NOT EXISTS idx_products_product_name_trgm
    ON products USING gin (product_name gin_trgm_ops);

-- variation_name（推奨: 新規追加検索フィールドの高速化）
CREATE INDEX IF NOT EXISTS idx_products_variation_name_trgm
    ON products USING gin (variation_name gin_trgm_ops);
