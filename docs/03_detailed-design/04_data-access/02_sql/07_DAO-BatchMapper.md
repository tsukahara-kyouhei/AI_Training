# BatchMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/BatchMapper.xml`  
対象テーブル: `orders`, `order_items`, `product_popular_rankings`, `recommended_related_products`

---

## 2. selectPopularRankingCandidates（ランキング候補集計）

指定期間（`sinceAt` ～ `asOf`）の受注明細を商品単位で集計する。

```sql
SELECT
  oi.product_code,
  pv.product_id,
  SUM(oi.quantity) AS sold_quantity
FROM order_items oi
JOIN product_variants pv
  ON pv.product_code = oi.product_code
JOIN orders o ON o.order_id = oi.order_id
WHERE o.order_datetime >= #{sinceAt}
  AND o.order_datetime <  #{asOf}
  AND o.order_status NOT IN ('cancelled')
GROUP BY oi.product_code, pv.product_id
ORDER BY sold_quantity DESC
```

---

## 3. deletePopularRankingsByDate / insertPopularRanking（ランキング保存）

```sql
-- 既存データ削除
DELETE FROM product_popular_rankings
WHERE ranking_date = #{rankingDate}

-- 1件ずつ登録（Java側でループ）
INSERT INTO product_popular_rankings (
  ranking_date, rank, product_id, sold_quantity_1m, created_at
) VALUES (
  #{rankingDate}, #{rank}, #{productId}, #{soldQuantity1m}, CURRENT_TIMESTAMP
)
```

---

## 4. selectOrderProductOccurrences（共起ペア抽出）

同一注文内で一緒に購入された商品ペアを抽出する（協調フィルタリング用）。

```sql
SELECT
  a.product_id AS source_product_id,
  b.product_id AS related_product_id,
  COUNT(*) AS co_occurrence_count
FROM order_items oia
JOIN product_variants pva ON pva.product_code = oia.product_code
JOIN order_items oib ON oib.order_id = oia.order_id
  AND oib.product_code != oia.product_code
JOIN product_variants pvb ON pvb.product_code = oib.product_code
JOIN orders o ON o.order_id = oia.order_id
WHERE o.order_datetime >= #{sinceAt}
  AND o.order_status NOT IN ('cancelled')
GROUP BY a.product_id, b.product_id
```

> 戻り値 `BatchOrderProductOccurrenceMapperRow` は `(sourceProductId, relatedProductId, coOccurrenceCount)` を保持する。

---

## 5. deleteRecommendedRelatedByDate / insertRecommendedRelated（レコメンド保存）

```sql
-- 既存データ削除
DELETE FROM recommended_related_products
WHERE recommendation_date = #{recommendationDate}

-- 1件ずつ登録（Java側でループ）
INSERT INTO recommended_related_products (
  recommendation_date, source_product_id, rank,
  recommended_product_id, score, created_at
) VALUES (
  #{recommendationDate}, #{sourceProductId}, #{rank},
  #{recommendedProductId}, #{score}, CURRENT_TIMESTAMP
)
```
