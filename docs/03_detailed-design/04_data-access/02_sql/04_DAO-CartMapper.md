# CartMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/CartMapper.xml`  
対象テーブル: `product_variants`, `products`, `colors`, `tax_rates`

カートデータ自体はDBに保存せず `HttpSession` で管理するため、CartMapperの役割は **カート表示に必要な商品スナップショット取得** と **税率取得** のみ。

---

## 2. selectCartProductSnapshots（カート商品スナップショット）

カートに入っている `productVariantId` のリストに対し、最新の価格・在庫・商品名を一括取得する。

```sql
SELECT
  pv.product_variant_id,
  p.product_id,
  p.product_name,
  pv.product_code,
  c.color_name,
  pv.unit_price,
  pv.stock_quantity,
  p.assembly_available,
  p.assembly_fee
FROM product_variants pv
JOIN products p ON p.product_id = pv.product_id
JOIN colors   c ON c.color_id   = pv.color_id
WHERE pv.product_variant_id IN
  <foreach collection="productVariantIds" item="id" open="(" close=")" separator=",">
    #{id}
  </foreach>
  AND p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
```

**用途:** カート一覧画面・確認画面での商品情報表示、在庫チェック。

---

## 3. selectCurrentTaxRatePercent（現在税率）

```sql
SELECT tax_rate_percent
FROM tax_rates
WHERE is_active = TRUE
  AND effective_start_at <= #{now}
  AND (effective_end_at IS NULL OR effective_end_at > #{now})
ORDER BY effective_start_at DESC
LIMIT 1
```
