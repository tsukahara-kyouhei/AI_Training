\ir test-data/orders.sql

-- Add coupon columns to existing orders table
ALTER TABLE orders
	ADD COLUMN applied_coupon_code VARCHAR(255) NULL,
	ADD COLUMN coupon_discount_amount INTEGER NOT NULL DEFAULT 0;
