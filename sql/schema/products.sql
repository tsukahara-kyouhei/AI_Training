CREATE TABLE IF NOT EXISTS colors (
    color_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    color_name VARCHAR(40) NOT NULL UNIQUE,
    color_code CHAR(7) NOT NULL UNIQUE CHECK (color_code ~ '^#[0-9A-Fa-f]{6}$'),
    swatch_type VARCHAR(32) NOT NULL DEFAULT 'solid',
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_colors_swatch_type
        CHECK (swatch_type IN ('solid', 'transparent_pattern'))
);
CREATE INDEX IF NOT EXISTS idx_colors_active_sort ON colors (is_active, sort_order);

CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category_id VARCHAR(20) NOT NULL CHECK (category_id IN ('desk', 'chair', 'storage')),
    description TEXT,
    assembly_available BOOLEAN NOT NULL DEFAULT FALSE,
    assembly_fee NUMERIC(12,0) NOT NULL DEFAULT 0,
    has_variation BOOLEAN NOT NULL DEFAULT FALSE,
    variation_group_id BIGINT,
    variation_name VARCHAR(120),
    sale_start_at TIMESTAMPTZ NOT NULL,
    sale_end_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (sale_end_at IS NULL OR sale_end_at > sale_start_at),
    CHECK (assembly_fee >= 0),
    CHECK (assembly_available OR assembly_fee = 0),
    CHECK (
        (has_variation = TRUE AND variation_group_id IS NOT NULL AND variation_name IS NOT NULL)
        OR
        (has_variation = FALSE AND variation_group_id IS NULL AND variation_name IS NULL)
    )
);
CREATE INDEX IF NOT EXISTS idx_products_category_sale_period
    ON products (category_id, sale_start_at, sale_end_at);
CREATE INDEX IF NOT EXISTS idx_products_sale_start_at
    ON products (sale_start_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_variation_group
    ON products (variation_group_id);
CREATE INDEX IF NOT EXISTS idx_products_created_at
    ON products (created_at DESC);

CREATE TABLE IF NOT EXISTS product_variants (
    product_variant_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(32) NOT NULL UNIQUE,
    color_id BIGINT NOT NULL,
    unit_price NUMERIC(12,0) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_variants_color FOREIGN KEY (color_id)
        REFERENCES colors (color_id) ON DELETE RESTRICT,
    CONSTRAINT uq_product_variants_product_color UNIQUE (product_id, color_id)
);
CREATE INDEX IF NOT EXISTS idx_product_variants_product ON product_variants (product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_color ON product_variants (color_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_price ON product_variants (unit_price);
CREATE INDEX IF NOT EXISTS idx_product_variants_in_stock
    ON product_variants (product_id)
    WHERE stock_quantity > 0;

CREATE TABLE IF NOT EXISTS product_desk_attributes (
    product_id BIGINT PRIMARY KEY,
    top_shape_id BIGINT NOT NULL,
    width_mm INTEGER NOT NULL CHECK (width_mm > 0),
    depth_mm INTEGER NOT NULL CHECK (depth_mm > 0),
    height_mm INTEGER NOT NULL CHECK (height_mm > 0),
    taste_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_desk_attributes_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_desk_attributes_top_shape FOREIGN KEY (top_shape_id)
        REFERENCES desk_top_shapes (top_shape_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_desk_attributes_taste FOREIGN KEY (taste_id)
        REFERENCES tastes (taste_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_product_desk_attributes_top_shape ON product_desk_attributes (top_shape_id);
CREATE INDEX IF NOT EXISTS idx_product_desk_attributes_taste ON product_desk_attributes (taste_id);
CREATE INDEX IF NOT EXISTS idx_product_desk_attributes_width ON product_desk_attributes (width_mm);
CREATE INDEX IF NOT EXISTS idx_product_desk_attributes_depth ON product_desk_attributes (depth_mm);
CREATE INDEX IF NOT EXISTS idx_product_desk_attributes_height ON product_desk_attributes (height_mm);

CREATE TABLE IF NOT EXISTS product_chair_attributes (
    product_id BIGINT PRIMARY KEY,
    function_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    taste_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_chair_attributes_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_chair_attributes_function FOREIGN KEY (function_id)
        REFERENCES chair_functions (function_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_chair_attributes_material FOREIGN KEY (material_id)
        REFERENCES chair_materials (material_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_chair_attributes_taste FOREIGN KEY (taste_id)
        REFERENCES tastes (taste_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_product_chair_attributes_function ON product_chair_attributes (function_id);
CREATE INDEX IF NOT EXISTS idx_product_chair_attributes_material ON product_chair_attributes (material_id);
CREATE INDEX IF NOT EXISTS idx_product_chair_attributes_taste ON product_chair_attributes (taste_id);

CREATE TABLE IF NOT EXISTS product_storage_attributes (
    product_id BIGINT PRIMARY KEY,
    usage_id BIGINT NOT NULL,
    taste_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_storage_attributes_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_storage_attributes_usage FOREIGN KEY (usage_id)
        REFERENCES storage_usages (usage_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_storage_attributes_taste FOREIGN KEY (taste_id)
        REFERENCES tastes (taste_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_product_storage_attributes_usage ON product_storage_attributes (usage_id);
CREATE INDEX IF NOT EXISTS idx_product_storage_attributes_taste ON product_storage_attributes (taste_id);

-- logical comments
COMMENT ON TABLE colors IS 'カラーマスタ';
COMMENT ON COLUMN colors.color_id IS 'カラーID';
COMMENT ON COLUMN colors.color_name IS 'カラー名';
COMMENT ON COLUMN colors.color_code IS 'カラーコード';
COMMENT ON COLUMN colors.swatch_type IS '色見本表示種別';
COMMENT ON COLUMN colors.sort_order IS '表示順';
COMMENT ON COLUMN colors.is_active IS '有効フラグ';
COMMENT ON COLUMN colors.created_at IS '作成日時';
COMMENT ON COLUMN colors.updated_at IS '更新日時';

COMMENT ON TABLE products IS '商品';
COMMENT ON COLUMN products.product_id IS '商品ID';
COMMENT ON COLUMN products.product_name IS '商品名';
COMMENT ON COLUMN products.category_id IS '商品カテゴリID';
COMMENT ON COLUMN products.description IS '商品紹介文';
COMMENT ON COLUMN products.assembly_available IS '組立・設置可否';
COMMENT ON COLUMN products.assembly_fee IS '組立・設置費';
COMMENT ON COLUMN products.has_variation IS 'シリーズバリエーション有無';
COMMENT ON COLUMN products.variation_group_id IS 'バリエーショングループID';
COMMENT ON COLUMN products.variation_name IS 'バリエーション名';
COMMENT ON COLUMN products.sale_start_at IS '販売開始日時';
COMMENT ON COLUMN products.sale_end_at IS '販売終了日時';
COMMENT ON COLUMN products.created_at IS '作成日時';
COMMENT ON COLUMN products.updated_at IS '更新日時';

COMMENT ON TABLE product_variants IS '商品バリアント';
COMMENT ON COLUMN product_variants.product_variant_id IS '商品バリアントID';
COMMENT ON COLUMN product_variants.product_id IS '商品ID';
COMMENT ON COLUMN product_variants.product_code IS '商品コード';
COMMENT ON COLUMN product_variants.color_id IS 'カラーID';
COMMENT ON COLUMN product_variants.unit_price IS '販売価格';
COMMENT ON COLUMN product_variants.stock_quantity IS '在庫数';
COMMENT ON COLUMN product_variants.created_at IS '作成日時';
COMMENT ON COLUMN product_variants.updated_at IS '更新日時';

COMMENT ON TABLE product_desk_attributes IS '商品デスク属性';
COMMENT ON COLUMN product_desk_attributes.product_id IS '商品ID';
COMMENT ON COLUMN product_desk_attributes.top_shape_id IS '天板形状ID';
COMMENT ON COLUMN product_desk_attributes.width_mm IS '幅(mm)';
COMMENT ON COLUMN product_desk_attributes.depth_mm IS '奥行(mm)';
COMMENT ON COLUMN product_desk_attributes.height_mm IS '高さ(mm)';
COMMENT ON COLUMN product_desk_attributes.taste_id IS 'テイストID';
COMMENT ON COLUMN product_desk_attributes.created_at IS '作成日時';
COMMENT ON COLUMN product_desk_attributes.updated_at IS '更新日時';

COMMENT ON TABLE product_chair_attributes IS '商品チェア属性';
COMMENT ON COLUMN product_chair_attributes.product_id IS '商品ID';
COMMENT ON COLUMN product_chair_attributes.function_id IS '機能ID';
COMMENT ON COLUMN product_chair_attributes.material_id IS '素材ID';
COMMENT ON COLUMN product_chair_attributes.taste_id IS 'テイストID';
COMMENT ON COLUMN product_chair_attributes.created_at IS '作成日時';
COMMENT ON COLUMN product_chair_attributes.updated_at IS '更新日時';

COMMENT ON TABLE product_storage_attributes IS '商品収納家具属性';
COMMENT ON COLUMN product_storage_attributes.product_id IS '商品ID';
COMMENT ON COLUMN product_storage_attributes.usage_id IS '用途ID';
COMMENT ON COLUMN product_storage_attributes.taste_id IS 'テイストID';
COMMENT ON COLUMN product_storage_attributes.created_at IS '作成日時';
COMMENT ON COLUMN product_storage_attributes.updated_at IS '更新日時';

-- FEAT-001: 半角→全角正規化関数（数字・英字）
-- 半角カタカナはアプリケーション層（TextNormalizer）で変換済みの値が渡されるため、ここでは変換不要。
CREATE OR REPLACE FUNCTION normalize_fullwidth(input text) RETURNS text AS $$
DECLARE
  result text := input;
BEGIN
  -- 半角数字 → 全角 (0-9 → ０-９)
  result := translate(result,
    '0123456789',
    '０１２３４５６７８９');
  -- 半角英字大文字 → 全角 (A-Z → Ａ-Ｚ)
  result := translate(result,
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ');
  -- 半角英字小文字 → 全角 (a-z → ａ-ｚ)
  result := translate(result,
    'abcdefghijklmnopqrstuvwxyz',
    'ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ');
  RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

