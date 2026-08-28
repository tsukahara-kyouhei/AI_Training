CREATE TABLE IF NOT EXISTS product_reviews (
    review_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title VARCHAR(100),
    body TEXT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_reviews_member
        FOREIGN KEY (member_id)
        REFERENCES members (member_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_product_reviews_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_product_reviews_member_product
        UNIQUE (member_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_published_created
    ON product_reviews (product_id, is_published, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_reviews_member_product
    ON product_reviews (member_id, product_id);

COMMENT ON TABLE product_reviews IS '商品レビュー';

COMMENT ON COLUMN product_reviews.review_id IS 'レビューID';
COMMENT ON COLUMN product_reviews.member_id IS '会員ID';
COMMENT ON COLUMN product_reviews.product_id IS '商品ID';
COMMENT ON COLUMN product_reviews.rating IS '評価';
COMMENT ON COLUMN product_reviews.title IS 'レビュータイトル';
COMMENT ON COLUMN product_reviews.body IS 'レビュー本文';
COMMENT ON COLUMN product_reviews.is_published IS '公開状態';
COMMENT ON COLUMN product_reviews.created_at IS '作成日時';
COMMENT ON COLUMN product_reviews.updated_at IS '更新日時';