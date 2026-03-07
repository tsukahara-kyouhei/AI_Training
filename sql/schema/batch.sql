CREATE TABLE IF NOT EXISTS tax_rates (
    tax_rate_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tax_rate_percent NUMERIC(5,2) NOT NULL CHECK (tax_rate_percent > 0 AND tax_rate_percent <= 100),
    effective_start_at TIMESTAMPTZ NOT NULL,
    effective_end_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (effective_end_at IS NULL OR effective_end_at > effective_start_at)
);
CREATE INDEX IF NOT EXISTS idx_tax_rates_active_start ON tax_rates (is_active, effective_start_at DESC);
CREATE INDEX IF NOT EXISTS idx_tax_rates_period ON tax_rates (effective_start_at, effective_end_at);

CREATE TABLE IF NOT EXISTS popular_product_rankings (
    ranking_date DATE NOT NULL,
    rank SMALLINT NOT NULL CHECK (rank BETWEEN 1 AND 10),
    product_id BIGINT NOT NULL,
    sold_quantity_1m INTEGER NOT NULL DEFAULT 0 CHECK (sold_quantity_1m >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ranking_date, rank),
    CONSTRAINT uq_popular_product_rankings_product UNIQUE (ranking_date, product_id),
    CONSTRAINT fk_popular_product_rankings_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_popular_rankings_product_date
    ON popular_product_rankings (product_id, ranking_date DESC);

CREATE TABLE IF NOT EXISTS recommended_related_products (
    recommendation_date DATE NOT NULL,
    source_product_id BIGINT NOT NULL,
    rank SMALLINT NOT NULL CHECK (rank BETWEEN 1 AND 4),
    recommended_product_id BIGINT NOT NULL,
    score NUMERIC(10,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_date, source_product_id, rank),
    CONSTRAINT uq_recommended_related_products_pair UNIQUE (recommendation_date, source_product_id, recommended_product_id),
    CONSTRAINT fk_recommended_related_products_source FOREIGN KEY (source_product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_recommended_related_products_target FOREIGN KEY (recommended_product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CHECK (source_product_id <> recommended_product_id)
);
CREATE INDEX IF NOT EXISTS idx_recommended_related_products_source_date
    ON recommended_related_products (source_product_id, recommendation_date DESC);
CREATE INDEX IF NOT EXISTS idx_recommended_related_products_recommended
    ON recommended_related_products (recommended_product_id);

-- logical comments
COMMENT ON TABLE tax_rates IS '消費税率';
COMMENT ON COLUMN tax_rates.tax_rate_id IS '消費税率ID';
COMMENT ON COLUMN tax_rates.tax_rate_percent IS '消費税率';
COMMENT ON COLUMN tax_rates.effective_start_at IS '適用開始日時';
COMMENT ON COLUMN tax_rates.effective_end_at IS '適用終了日時';
COMMENT ON COLUMN tax_rates.is_active IS '有効フラグ';
COMMENT ON COLUMN tax_rates.created_at IS '作成日時';
COMMENT ON COLUMN tax_rates.updated_at IS '更新日時';

COMMENT ON TABLE popular_product_rankings IS '売れ筋ランキング';
COMMENT ON COLUMN popular_product_rankings.ranking_date IS '集計日';
COMMENT ON COLUMN popular_product_rankings.rank IS '順位';
COMMENT ON COLUMN popular_product_rankings.product_id IS '商品ID';
COMMENT ON COLUMN popular_product_rankings.sold_quantity_1m IS '直近1か月販売数量';
COMMENT ON COLUMN popular_product_rankings.created_at IS '作成日時';
COMMENT ON COLUMN popular_product_rankings.updated_at IS '更新日時';

COMMENT ON TABLE recommended_related_products IS 'おすすめ関連商品';
COMMENT ON COLUMN recommended_related_products.recommendation_date IS '推薦日';
COMMENT ON COLUMN recommended_related_products.source_product_id IS '基準商品ID';
COMMENT ON COLUMN recommended_related_products.rank IS '順位';
COMMENT ON COLUMN recommended_related_products.recommended_product_id IS '推薦商品ID';
COMMENT ON COLUMN recommended_related_products.score IS '類似度スコア';
COMMENT ON COLUMN recommended_related_products.created_at IS '作成日時';
COMMENT ON COLUMN recommended_related_products.updated_at IS '更新日時';
