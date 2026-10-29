CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('fixed_amount', 'percentage')),
    discount_value NUMERIC(16,4) NOT NULL CHECK (discount_value >= 0),
    max_discount_amount NUMERIC(12,0) CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    min_purchase_amount NUMERIC(12,0) NOT NULL DEFAULT 0 CHECK (min_purchase_amount >= 0),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    target_conditions JSONB NOT NULL,
    max_usage_total BIGINT CHECK (max_usage_total IS NULL OR max_usage_total >= 0),
    max_usage_per_user INTEGER CHECK (max_usage_per_user IS NULL OR max_usage_per_user >= 0),
    used_count_total BIGINT NOT NULL DEFAULT 0 CHECK (used_count_total >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_coupons_valid_period CHECK (valid_to >= valid_from),
    CONSTRAINT chk_coupons_fixed_discount_integer
        CHECK (discount_type <> 'fixed_amount' OR discount_value = TRUNC(discount_value)),
    CONSTRAINT chk_coupons_percentage_range
        CHECK (discount_type <> 'percentage' OR discount_value <= 100),
    CONSTRAINT chk_coupons_max_discount_type
        CHECK (max_discount_amount IS NULL OR discount_type = 'percentage'),
    CONSTRAINT chk_coupons_target_conditions_object
        CHECK (jsonb_typeof(target_conditions) = 'object'),
    CONSTRAINT chk_coupons_target_condition_keys
        CHECK (
            jsonb_typeof(target_conditions -> 'allowed_category_ids') IS NOT DISTINCT FROM 'array'
            AND jsonb_typeof(target_conditions -> 'allowed_product_ids') IS NOT DISTINCT FROM 'array'
            AND jsonb_typeof(target_conditions -> 'target_user_type') IS NOT DISTINCT FROM 'string'
            AND COALESCE(target_conditions ->> 'target_user_type' IN ('all', 'new'), FALSE)
        ),
    CONSTRAINT chk_coupons_target_condition_unknown_keys
        CHECK (
            target_conditions - ARRAY[
                'allowed_category_ids',
                'allowed_product_ids',
                'target_user_type'
            ]::text[] = '{}'::jsonb
        )
);

CREATE INDEX IF NOT EXISTS idx_coupons_active_period
    ON coupons (is_active, valid_from, valid_to);

CREATE TABLE IF NOT EXISTS user_coupon_usages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_coupon_usages_member_coupon UNIQUE (member_id, coupon_id),
    CONSTRAINT fk_user_coupon_usages_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_coupon_usages_coupon FOREIGN KEY (coupon_id)
        REFERENCES coupons (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_user_coupon_usages_coupon
    ON user_coupon_usages (coupon_id);

CREATE OR REPLACE FUNCTION validate_coupon_target_conditions()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    item JSONB;
    product_id_value BIGINT;
BEGIN
    IF jsonb_typeof(NEW.target_conditions) IS DISTINCT FROM 'object'
       OR jsonb_typeof(NEW.target_conditions -> 'allowed_category_ids') IS DISTINCT FROM 'array'
       OR jsonb_typeof(NEW.target_conditions -> 'allowed_product_ids') IS DISTINCT FROM 'array'
       OR jsonb_typeof(NEW.target_conditions -> 'target_user_type') IS DISTINCT FROM 'string'
       OR NOT COALESCE(NEW.target_conditions ->> 'target_user_type' IN ('all', 'new'), FALSE)
       OR NEW.target_conditions - ARRAY[
            'allowed_category_ids',
            'allowed_product_ids',
            'target_user_type'
       ]::text[] <> '{}'::jsonb THEN
        RAISE EXCEPTION 'Invalid coupon target_conditions' USING ERRCODE = '23514';
    END IF;

    FOR item IN
        SELECT value FROM jsonb_array_elements(NEW.target_conditions -> 'allowed_category_ids') AS entry(value)
    LOOP
        IF jsonb_typeof(item) <> 'string' OR item #>> '{}' NOT IN ('desk', 'chair', 'storage') THEN
            RAISE EXCEPTION 'Invalid coupon category id' USING ERRCODE = '23514';
        END IF;
    END LOOP;

    FOR item IN
        SELECT value FROM jsonb_array_elements(NEW.target_conditions -> 'allowed_product_ids') AS entry(value)
    LOOP
        IF jsonb_typeof(item) <> 'number' OR item::text !~ '^[1-9][0-9]*$' THEN
            RAISE EXCEPTION 'Invalid coupon product id' USING ERRCODE = '23514';
        END IF;
        product_id_value := (item #>> '{}')::BIGINT;
        IF NOT EXISTS (SELECT 1 FROM products WHERE product_id = product_id_value) THEN
            RAISE EXCEPTION 'Coupon target product does not exist' USING ERRCODE = '23503';
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_coupons_validate_target_conditions ON coupons;
CREATE TRIGGER trg_coupons_validate_target_conditions
    BEFORE INSERT OR UPDATE OF target_conditions ON coupons
    FOR EACH ROW EXECUTE FUNCTION validate_coupon_target_conditions();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_applied_coupon'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_applied_coupon FOREIGN KEY (applied_coupon_id)
            REFERENCES coupons (id) ON DELETE RESTRICT;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION prevent_coupon_physical_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Coupons must be deactivated instead of deleted';
END;
$$;

DROP TRIGGER IF EXISTS trg_coupons_prevent_delete ON coupons;
CREATE TRIGGER trg_coupons_prevent_delete
    BEFORE DELETE ON coupons
    FOR EACH ROW EXECUTE FUNCTION prevent_coupon_physical_delete();
