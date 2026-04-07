# ProductMapper SQL定義書

## 1. 概要

XML: `src/main/resources/mappers/ProductMapper.xml`  
対象テーブル: `products`, `product_variants`, `product_desk_attributes`, `product_chair_attributes`, `product_storage_attributes`, `product_popular_rankings`, `recommended_related_products`, `product_variation_groups`, `colors`, `tax_rates`

---

## 2. countProducts / selectProducts（商品一覧・件数取得）

商品一覧ページの動的WHERE検索。`Map<String,Object>` に検索パラメータをセットして呼び出す。

**主なWHERE条件（`<if>` タグで動的付与）:**

| パラメータキー | 型 | 説明 |
|---|---|---|
| `keywordLike` | String | 商品名・説明文・バリエーション名の部分一致ILIKE検索（`'%keyword%'` 形式）。`translate()` で全角→半角正規化して比較 |
| `keywordPrefix` | String | 商品コードの前方一致ILIKE検索（`'keyword%'` 形式）。EXISTS副問合せで `product_variants.product_code` を検索。`translate()` で正規化 |
| `categoryId` | String | カテゴリID（desk/chair/storage） |
| `inStockOnly` | boolean | 在庫あり絞り込み |
| `colorIds` | List | カラーID `IN` |
| `priceRanges` | List\<RangeValue\> | 価格帯（`minInclusive`/`maxInclusive`） |
| `deskTopShapeIds` | List | デスク天板形状ID（desk専用） |
| `deskTasteIds` | List | デスクテイストID |
| `deskWidthRanges` | List\<RangeValue\> | デスク幅範囲（mm） |
| `chairFunctionIds` | List | チェア機能ID（chair専用） |
| `chairMaterialIds` | List | チェア素材ID |
| `storageTasteIds` | List | 収納棚テイストID（storage専用） |
| `hasTasteFilter` | boolean | テイストフィルター有無フラグ（検索結果画面でカテゴリ横断テイスト絞込み時に使用） |
| `limit` / `offset` | int | ページング |

**キーワード検索WHERE条件（変更後）:**

```xml
<if test="keywordLike != null and keywordLike != ''">
    AND (
        translate(p.product_name,
            <include refid="NormalizeFrom"/>, <include refid="NormalizeTo"/>
        ) ILIKE #{keywordLike} ESCAPE '\'
        OR translate(p.description,
            <include refid="NormalizeFrom"/>, <include refid="NormalizeTo"/>
        ) ILIKE #{keywordLike} ESCAPE '\'
        OR (p.variation_name IS NOT NULL
            AND translate(p.variation_name,
                <include refid="NormalizeFrom"/>, <include refid="NormalizeTo"/>
            ) ILIKE #{keywordLike} ESCAPE '\')
        OR EXISTS (
            SELECT 1 FROM product_variants pvk
            WHERE pvk.product_id = p.product_id
            AND translate(pvk.product_code,
                <include refid="NormalizeFrom"/>, <include refid="NormalizeTo"/>
            ) ILIKE #{keywordPrefix} ESCAPE '\'
        )
    )
</if>
```

- `NormalizeFrom` / `NormalizeTo`: `<sql>` fragment で定義する変換テーブル文字列リテラル。英数字は全角→半角、カタカナ清音＋長音は半角→全角の双方向変換を1つの `translate()` で実施（例: `<sql id="NormalizeFrom">'ＡＢＣ...Ｚａｂｃ...ｚ０１２...９ｱｲｳ...ﾝｰ'</sql>`）
- 4条件はOR結合。`variation_name` は NULL の場合スキップ
- 商品コードは前方一致（`keywordPrefix`）、それ以外は部分一致（`keywordLike`）
- 大文字小文字の同一視は `ILIKE` で対応
- 各 ILIKE 句に `ESCAPE '\'` を付与し、特殊文字（`%` `_` `\`）のエスケープに対応

**テイストフィルターWHERE条件（検索結果画面用、新規追加）:**

```xml
<if test="hasTasteFilter">
    AND
    <trim prefix="(" suffix=")" prefixOverrides="OR">
        <if test="deskTasteIds != null and !deskTasteIds.isEmpty()">
            OR EXISTS (SELECT 1 FROM product_desk_attributes da
                    WHERE da.product_id = p.product_id
                    AND da.taste_id IN
                    <foreach item="id" collection="deskTasteIds" open="(" separator="," close=")">#{id}</foreach>)
        </if>
        <if test="chairTasteIds != null and !chairTasteIds.isEmpty()">
            OR EXISTS (SELECT 1 FROM product_chair_attributes ca
                       WHERE ca.product_id = p.product_id
                       AND ca.taste_id IN
                       <foreach item="id" collection="chairTasteIds" open="(" separator="," close=")">#{id}</foreach>)
        </if>
        <if test="storageTasteIds != null and !storageTasteIds.isEmpty()">
            OR EXISTS (SELECT 1 FROM product_storage_attributes sa
                       WHERE sa.product_id = p.product_id
                       AND sa.taste_id IN
                       <foreach item="id" collection="storageTasteIds" open="(" separator="," close=")">#{id}</foreach>)
        </if>
    </trim>
</if>
```

- `<trim prefixOverrides="OR">` により、最初に出現する `OR` を除去し、先頭の `OR EXISTS` が構文エラーにならない
- 未選択カテゴリのテイストはOR条件に含めない
- テイストが1つでも選択されている場合、該当テイスト属性を持つ商品のみに絞り込まれる

**取得カラム（ProductListMapperRow）:**
`product_id`, `product_name`, `min_price`（バリアント最安値）, `max_stock`（最大在庫）, `product_code`

---

## 3. selectTopRankedProducts（ランキング上位商品）

```sql
SELECT
  pr.rank,
  p.product_id,
  p.product_name,
  MIN(pv.unit_price) AS min_price,
  MAX(pv.stock_quantity) AS max_stock,
  MIN(pv.product_code) AS product_code
FROM product_popular_rankings pr
JOIN products p ON p.product_id = pr.product_id
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE pr.ranking_date = (
    SELECT MAX(ranking_date) FROM product_popular_rankings
)
GROUP BY pr.rank, p.product_id, p.product_name
ORDER BY pr.rank ASC
LIMIT #{limit}
```

---

## 4. selectProductDetail（商品詳細ヘッダ）

```sql
SELECT
  p.product_id, p.product_name, p.description, p.category_id,
  p.assembly_available, p.assembly_fee,
  (SELECT COUNT(*) > 1 FROM products p2
   WHERE p2.variation_group_id = p.variation_group_id) AS has_variation,
  p.variation_group_id, pvg.variation_name
FROM products p
LEFT JOIN product_variation_groups pvg ON pvg.variation_group_id = p.variation_group_id
WHERE p.product_id = #{productId}
  AND p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
```

---

## 5. selectProductVariants（カラーバリアント一覧）

```sql
SELECT
  pv.product_variant_id, pv.product_code,
  c.color_id, c.color_name, c.color_code,
  pv.unit_price, pv.stock_quantity
FROM product_variants pv
JOIN colors c ON c.color_id = pv.color_id
WHERE pv.product_id = #{productId}
ORDER BY c.sort_order
```

---

## 6. selectCurrentTaxRatePercent（現在税率）

```sql
SELECT tax_rate_percent
FROM tax_rates
WHERE is_active = TRUE
  AND effective_start_at <= #{now}
  AND (effective_end_at IS NULL OR effective_end_at > #{now})
ORDER BY effective_start_at DESC
LIMIT 1
```
