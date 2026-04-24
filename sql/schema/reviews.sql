-- =============================================================
-- reviews: 商品レビュー
-- =============================================================

CREATE TABLE reviews (
    review_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    BIGINT        NOT NULL REFERENCES members(member_id),
    product_id   BIGINT        NOT NULL REFERENCES products(product_id),
    rating       SMALLINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title        VARCHAR(100),
    body         VARCHAR(1000) NOT NULL,
    is_published BOOLEAN       NOT NULL DEFAULT TRUE,
    is_blocked   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (member_id, product_id)
);

CREATE INDEX idx_reviews_product_created
    ON reviews (product_id, created_at DESC);
