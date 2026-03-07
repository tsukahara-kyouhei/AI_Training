CREATE TABLE IF NOT EXISTS desk_top_shapes (
    top_shape_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_desk_top_shapes_active_sort ON desk_top_shapes (is_active, sort_order);

CREATE TABLE IF NOT EXISTS desk_tastes (
    taste_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_desk_tastes_active_sort ON desk_tastes (is_active, sort_order);

CREATE TABLE IF NOT EXISTS chair_functions (
    function_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chair_functions_active_sort ON chair_functions (is_active, sort_order);

CREATE TABLE IF NOT EXISTS chair_materials (
    material_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chair_materials_active_sort ON chair_materials (is_active, sort_order);

CREATE TABLE IF NOT EXISTS chair_tastes (
    taste_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chair_tastes_active_sort ON chair_tastes (is_active, sort_order);

CREATE TABLE IF NOT EXISTS storage_usages (
    usage_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_storage_usages_active_sort ON storage_usages (is_active, sort_order);

CREATE TABLE IF NOT EXISTS storage_tastes (
    taste_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_storage_tastes_active_sort ON storage_tastes (is_active, sort_order);

-- logical comments
COMMENT ON TABLE desk_top_shapes IS 'デスク天板形状マスタ';
COMMENT ON COLUMN desk_top_shapes.top_shape_id IS '天板形状ID';
COMMENT ON COLUMN desk_top_shapes.display_name IS '天板形状名';
COMMENT ON COLUMN desk_top_shapes.sort_order IS '表示順';
COMMENT ON COLUMN desk_top_shapes.is_active IS '有効フラグ';
COMMENT ON COLUMN desk_top_shapes.created_at IS '作成日時';
COMMENT ON COLUMN desk_top_shapes.updated_at IS '更新日時';

COMMENT ON TABLE desk_tastes IS 'デスクテイストマスタ';
COMMENT ON COLUMN desk_tastes.taste_id IS 'テイストID';
COMMENT ON COLUMN desk_tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN desk_tastes.sort_order IS '表示順';
COMMENT ON COLUMN desk_tastes.is_active IS '有効フラグ';
COMMENT ON COLUMN desk_tastes.created_at IS '作成日時';
COMMENT ON COLUMN desk_tastes.updated_at IS '更新日時';

COMMENT ON TABLE chair_functions IS 'チェア機能マスタ';
COMMENT ON COLUMN chair_functions.function_id IS '機能ID';
COMMENT ON COLUMN chair_functions.display_name IS '機能名';
COMMENT ON COLUMN chair_functions.sort_order IS '表示順';
COMMENT ON COLUMN chair_functions.is_active IS '有効フラグ';
COMMENT ON COLUMN chair_functions.created_at IS '作成日時';
COMMENT ON COLUMN chair_functions.updated_at IS '更新日時';

COMMENT ON TABLE chair_materials IS 'チェア素材マスタ';
COMMENT ON COLUMN chair_materials.material_id IS '素材ID';
COMMENT ON COLUMN chair_materials.display_name IS '素材名';
COMMENT ON COLUMN chair_materials.sort_order IS '表示順';
COMMENT ON COLUMN chair_materials.is_active IS '有効フラグ';
COMMENT ON COLUMN chair_materials.created_at IS '作成日時';
COMMENT ON COLUMN chair_materials.updated_at IS '更新日時';

COMMENT ON TABLE chair_tastes IS 'チェアテイストマスタ';
COMMENT ON COLUMN chair_tastes.taste_id IS 'テイストID';
COMMENT ON COLUMN chair_tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN chair_tastes.sort_order IS '表示順';
COMMENT ON COLUMN chair_tastes.is_active IS '有効フラグ';
COMMENT ON COLUMN chair_tastes.created_at IS '作成日時';
COMMENT ON COLUMN chair_tastes.updated_at IS '更新日時';

COMMENT ON TABLE storage_usages IS '収納家具用途マスタ';
COMMENT ON COLUMN storage_usages.usage_id IS '用途ID';
COMMENT ON COLUMN storage_usages.display_name IS '用途名';
COMMENT ON COLUMN storage_usages.sort_order IS '表示順';
COMMENT ON COLUMN storage_usages.is_active IS '有効フラグ';
COMMENT ON COLUMN storage_usages.created_at IS '作成日時';
COMMENT ON COLUMN storage_usages.updated_at IS '更新日時';

COMMENT ON TABLE storage_tastes IS '収納家具テイストマスタ';
COMMENT ON COLUMN storage_tastes.taste_id IS 'テイストID';
COMMENT ON COLUMN storage_tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN storage_tastes.sort_order IS '表示順';
COMMENT ON COLUMN storage_tastes.is_active IS '有効フラグ';
COMMENT ON COLUMN storage_tastes.created_at IS '作成日時';
COMMENT ON COLUMN storage_tastes.updated_at IS '更新日時';
