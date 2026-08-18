CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title VARCHAR(100),
    content TEXT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_reviews_member_product UNIQUE (member_id, product_id),
    CONSTRAINT fk_product_reviews_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_published
    ON product_reviews (product_id, is_published, created_at DESC);