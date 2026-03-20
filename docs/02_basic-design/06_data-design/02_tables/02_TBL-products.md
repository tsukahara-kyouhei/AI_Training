# 商品テーブル定義

## 1. products（商品）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 商品ID |
| 2 | product_name | VARCHAR(255) | NOT NULL | - | 商品名 |
| 3 | category_id | VARCHAR(20) | NOT NULL | CHECK: 'desk' / 'chair' / 'storage' | 商品カテゴリID |
| 4 | description | TEXT | NULL | - | 商品紹介文 |
| 5 | assembly_available | BOOLEAN | NOT NULL | DEFAULT FALSE | 組立・設置可否 |
| 6 | assembly_fee | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 組立・設置費 |
| 7 | has_variation | BOOLEAN | NOT NULL | DEFAULT FALSE | シリーズバリエーション有無 |
| 8 | variation_group_id | BIGINT | NULL | has_variation=TRUEの場合必須 | バリエーショングループID |
| 9 | variation_name | VARCHAR(120) | NULL | has_variation=TRUEの場合必須 | バリエーション名 |
| 10 | sale_start_at | TIMESTAMPTZ | NOT NULL | - | 販売開始日時 |
| 11 | sale_end_at | TIMESTAMPTZ | NULL | sale_start_at より後 | 販売終了日時 |
| 12 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 13 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

---

## 2. product_variants（商品バリアント）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_variant_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 商品バリアントID |
| 2 | product_id | BIGINT | NOT NULL | FK → products.product_id (CASCADE) | 商品ID |
| 3 | product_code | VARCHAR(32) | NOT NULL | UNIQUE | 商品コード |
| 4 | color_id | BIGINT | NOT NULL | FK → colors.color_id (RESTRICT) | カラーID |
| 5 | unit_price | NUMERIC(12,0) | NOT NULL | DEFAULT 0, CHECK >= 0 | 販売価格 |
| 6 | stock_quantity | INTEGER | NOT NULL | DEFAULT 0, CHECK >= 0 | 在庫数 |
| 7 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 8 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**UNIQUE制約:** (product_id, color_id)

---

## 3. colors（カラーマスタ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | color_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | カラーID |
| 2 | color_name | VARCHAR(40) | NOT NULL | UNIQUE | カラー名 |
| 3 | color_code | CHAR(7) | NOT NULL | UNIQUE, CHECK: `#RRGGBB`形式 | カラーコード |
| 4 | swatch_type | VARCHAR(32) | NOT NULL | DEFAULT 'solid', CHECK: 'solid' / 'transparent_pattern' | 色見本表示種別 |
| 5 | sort_order | INTEGER | NOT NULL | DEFAULT 0, CHECK >= 0 | 表示順 |
| 6 | is_active | BOOLEAN | NOT NULL | DEFAULT TRUE | 有効フラグ |
| 7 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 8 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

---

## 4. product_desk_attributes（商品デスク属性）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_id | BIGINT | NOT NULL | PK, FK → products.product_id (CASCADE) | 商品ID |
| 2 | top_shape_id | BIGINT | NOT NULL | FK → desk_top_shapes.top_shape_id (RESTRICT) | 天板形状ID |
| 3 | width_mm | INTEGER | NOT NULL | CHECK > 0 | 幅(mm) |
| 4 | depth_mm | INTEGER | NOT NULL | CHECK > 0 | 奥行(mm) |
| 5 | height_mm | INTEGER | NOT NULL | CHECK > 0 | 高さ(mm) |
| 6 | taste_id | BIGINT | NOT NULL | FK → desk_tastes.taste_id (RESTRICT) | テイストID |
| 7 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 8 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

---

## 5. product_chair_attributes（商品チェア属性）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_id | BIGINT | NOT NULL | PK, FK → products.product_id (CASCADE) | 商品ID |
| 2 | function_id | BIGINT | NOT NULL | FK → chair_functions.function_id (RESTRICT) | 機能ID |
| 3 | material_id | BIGINT | NOT NULL | FK → chair_materials.material_id (RESTRICT) | 素材ID |
| 4 | taste_id | BIGINT | NOT NULL | FK → chair_tastes.taste_id (RESTRICT) | テイストID |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

---

## 6. product_storage_attributes（商品収納家具属性）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | product_id | BIGINT | NOT NULL | PK, FK → products.product_id (CASCADE) | 商品ID |
| 2 | usage_id | BIGINT | NOT NULL | FK → storage_usages.usage_id (RESTRICT) | 用途ID |
| 3 | taste_id | BIGINT | NOT NULL | FK → storage_tastes.taste_id (RESTRICT) | テイストID |
| 4 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 5 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |
