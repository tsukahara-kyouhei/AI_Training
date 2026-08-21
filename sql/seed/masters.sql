INSERT INTO desk_top_shapes (display_name, sort_order, is_active)
VALUES
  ('丸形', 1, TRUE),
  ('角形', 2, TRUE),
  ('楕円型', 3, TRUE),
  ('L字型', 4, TRUE),
  ('その他', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO desk_tastes (display_name, sort_order, is_active)
VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO chair_functions (display_name, sort_order, is_active)
VALUES
  ('肘付き', 1, TRUE),
  ('肘なし', 2, TRUE),
  ('キャスターあり', 3, TRUE),
  ('キャスターなし', 4, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO chair_materials (display_name, sort_order, is_active)
VALUES
  ('メッシュ', 1, TRUE),
  ('クロス', 2, TRUE),
  ('樹脂', 3, TRUE),
  ('革張り', 4, TRUE),
  ('木製', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO chair_tastes (display_name, sort_order, is_active)
VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO storage_usages (display_name, sort_order, is_active)
VALUES
  ('小物収納', 1, TRUE),
  ('書類収納', 2, TRUE),
  ('オープンタイプ', 3, TRUE),
  ('扉付き', 4, TRUE),
  ('鍵付き', 5, TRUE),
  ('引出し付き', 6, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO storage_tastes (display_name, sort_order, is_active)
VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO colors (color_name, color_code, swatch_type, sort_order, is_active)
VALUES
  ('黒系', '#222222', 'solid', 1, TRUE),
  ('茶系', '#6F4E37', 'solid', 2, TRUE),
  ('ベージュ系', '#DCC8A8', 'solid', 3, TRUE),
  ('グレー系', '#8C9199', 'solid', 4, TRUE),
  ('シルバー系', '#C0C6CC', 'solid', 5, TRUE),
  ('白系', '#FFFFFF', 'solid', 6, TRUE),
  ('青系', '#2F70C9', 'solid', 7, TRUE),
  ('黄系', '#E6C229', 'solid', 8, TRUE),
  ('緑系', '#4EA66D', 'solid', 9, TRUE),
  ('橙系', '#E07A2D', 'solid', 10, TRUE),
  ('赤系', '#CF3B3B', 'solid', 11, TRUE),
  ('ピンク系', '#E79AB8', 'solid', 12, TRUE),
  ('紫系', '#8A6CCF', 'solid', 13, TRUE),
  ('透明系', '#F5F5F5', 'transparent_pattern', 14, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO tax_rates (tax_rate_percent, effective_start_at, effective_end_at, is_active)
VALUES
  (8.00, '2014-04-01 00:00:00+09', '2019-09-30 23:59:59+09', FALSE),
  (10.00, '2019-10-01 00:00:00+09', NULL, TRUE)
ON CONFLICT DO NOTHING;

-- クーポン初期データの投入
INSERT INTO coupons (
    coupon_code, discount_type, discount_value, valid_from, valid_to, min_purchase_amount, usage_limit, is_active
) VALUES
('WELCOME500', 'FIXED', 500, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0, 1, true),
('OFFICE10', 'PERCENT', 10, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 10000, 5, true),
('EXPIRED50', 'PERCENT', 50, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 0, 1, true)
ON CONFLICT (coupon_code) DO NOTHING;
