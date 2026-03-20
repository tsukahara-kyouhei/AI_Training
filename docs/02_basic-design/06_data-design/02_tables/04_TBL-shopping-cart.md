# カートテーブル定義

> **注意:** カートはゲスト・会員で管理方法が異なる。  
> - **ゲスト:** Cookieにシリアライズして保持（`CartCookieStore`）  
> - **会員:** DBの `shopping_cart` / `cart_lines` テーブルで管理

## 1. shopping_cart（ショッピングカート）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | cart_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | カートID |
| 2 | member_id | BIGINT | NOT NULL | UNIQUE, FK → members.member_id | 会員ID |
| 3 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 4 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**制約:** 1会員につき1カート（UNIQUE: member_id）

---

## 2. cart_lines（カートライン）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | cart_line_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | カートラインID |
| 2 | cart_id | BIGINT | NOT NULL | FK → shopping_cart.cart_id (CASCADE) | カートID |
| 3 | product_variant_id | BIGINT | NOT NULL | FK → product_variants.product_variant_id | 商品バリアントID |
| 4 | quantity | INTEGER | NOT NULL | CHECK: 1〜99 | 数量 |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**UNIQUE制約:** (cart_id, product_variant_id)

---

## 3. ゲストカートのCookieスキーマ

ゲストカートはCookieにJSON形式でシリアライズして保持する。

```json
[
  {
    "productVariantId": 123,
    "quantity": 2
  },
  ...
]
```

| フィールド | 型 | 説明 |
|---|---|---|
| productVariantId | Long | 商品バリアントID |
| quantity | Integer | 数量（1〜99） |
