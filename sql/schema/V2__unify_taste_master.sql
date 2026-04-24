-- ============================================================
-- FEAT-001: テイストマスタ統合
-- 実行タイミング: アプリデプロイと同時
-- ============================================================

-- 1. 統合テイストマスタ テーブル新設
CREATE TABLE tastes (
    taste_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order   INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tastes_active_sort ON tastes (is_active, sort_order);

COMMENT ON TABLE  tastes              IS 'テイスト統合マスタ';
COMMENT ON COLUMN tastes.taste_id    IS 'テイストID';
COMMENT ON COLUMN tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN tastes.sort_order  IS '表示順';
COMMENT ON COLUMN tastes.is_active   IS '有効フラグ';
COMMENT ON COLUMN tastes.created_at  IS '作成日時';
COMMENT ON COLUMN tastes.updated_at  IS '更新日時';

-- 2. 旧3テーブルを UNION して重複排除し tastes へ移行
INSERT INTO tastes (display_name, sort_order, is_active)
SELECT
    display_name,
    MIN(sort_order) AS sort_order,
    BOOL_OR(is_active) AS is_active
FROM (
    SELECT display_name, sort_order, is_active FROM desk_tastes
    UNION ALL
    SELECT display_name, sort_order, is_active FROM chair_tastes
    UNION ALL
    SELECT display_name, sort_order, is_active FROM storage_tastes
) combined
GROUP BY display_name
ORDER BY MIN(sort_order);

-- 3. product_desk_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_desk_attributes pda
SET taste_id = t.taste_id
FROM desk_tastes dt
JOIN tastes t ON t.display_name = dt.display_name
WHERE pda.taste_id = dt.taste_id;

-- 4. product_chair_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_chair_attributes pca
SET taste_id = t.taste_id
FROM chair_tastes ct
JOIN tastes t ON t.display_name = ct.display_name
WHERE pca.taste_id = ct.taste_id;

-- 5. product_storage_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_storage_attributes psa
SET taste_id = t.taste_id
FROM storage_tastes st
JOIN tastes t ON t.display_name = st.display_name
WHERE psa.taste_id = st.taste_id;

-- 6. product_desk_attributes の FK を tastes へ張り替え
ALTER TABLE product_desk_attributes
    DROP CONSTRAINT fk_product_desk_attributes_taste;
ALTER TABLE product_desk_attributes
    ADD CONSTRAINT fk_product_desk_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 7. product_chair_attributes の FK を tastes へ張り替え
ALTER TABLE product_chair_attributes
    DROP CONSTRAINT fk_product_chair_attributes_taste;
ALTER TABLE product_chair_attributes
    ADD CONSTRAINT fk_product_chair_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 8. product_storage_attributes の FK を tastes へ張り替え
ALTER TABLE product_storage_attributes
    DROP CONSTRAINT fk_product_storage_attributes_taste;
ALTER TABLE product_storage_attributes
    ADD CONSTRAINT fk_product_storage_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 9. 旧テーブル DROP（FK 参照がなくなった後に実行）
DROP TABLE desk_tastes;
DROP TABLE chair_tastes;
DROP TABLE storage_tastes;
