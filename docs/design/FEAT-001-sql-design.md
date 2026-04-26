# FEAT-001 SQL / MyBatis 詳細設計書

| 項目         | 内容                                           |
| ------------ | ---------------------------------------------- |
| Issue ID     | FEAT-001                                       |
| 作成日       | 2026-04-27                                     |
| 対象ファイル | `src/main/resources/mappers/ProductMapper.xml` |

---

## 1. 変更概要

| 変更対象            | 種別 | 内容                                                         |
| ------------------- | ---- | ------------------------------------------------------------ |
| `BaseProductWhere`  | 修正 | キーワード検索条件を FR-001/002 要件に合わせて刷新           |
| `TasteSearchFilter` | 新規 | 検索ページ用クロスカテゴリ テイストフィルタ SQL フラグメント |

---

## 2. `BaseProductWhere` フラグメント — キーワード検索部分の修正

### 2-1. 現状

```xml
<if test="keywordLike != null and keywordLike != ''">
    AND (
    p.product_name ILIKE #{keywordLike}
    OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE #{keywordLike}
    )
    )
</if>
```

### 2-2. 変更後

```xml
<if test="keyword != null and keyword != ''">
    AND (
        p.product_name ILIKE #{keywordLike}
        OR p.variation_name ILIKE #{keywordLike}
        OR p.description ILIKE #{keywordLike}
        OR EXISTS (
            SELECT 1
            FROM product_variants pvk
            WHERE pvk.product_id = p.product_id
            AND (
                LOWER(pvk.product_code) = LOWER(#{keyword})
                OR LOWER(pvk.product_code) LIKE LOWER(#{keyword}) || '%'
            )
        )
    )
</if>
```

### 2-3. 変更点の説明

| 変更点                                                    | 対応要件 | 理由                                                              |
| --------------------------------------------------------- | -------- | ----------------------------------------------------------------- |
| `test="keywordLike != null"` → `test="keyword != null"`   | FR-002   | `keyword` （生値）を主スイッチとすることで意図を明確化            |
| `p.variation_name ILIKE #{keywordLike}` 追加              | FR-002   | バリエーション名を部分一致検索対象に追加                          |
| `p.description ILIKE #{keywordLike}` 追加                 | FR-002   | 商品説明文を部分一致検索対象に追加                                |
| `pvk.product_code ILIKE #{keywordLike}` を削除            | FR-001   | 商品コードの中間一致を廃止                                        |
| `LOWER(pvk.product_code) = LOWER(#{keyword})`             | FR-001   | 完全一致（大文字小文字無視）                                      |
| `LOWER(pvk.product_code) LIKE LOWER(#{keyword}) \|\| '%'` | FR-001   | 前方一致（大文字小文字無視）                                      |
| `OR` → `OR EXISTS (...)` の使い分け                       | FR-003   | 商品コードは EXISTS で存在チェック、テキスト列は ILIKE で直接検索 |

---

## 3. `TasteSearchFilter` フラグメント — 新規追加

### 3-1. フラグメント定義

```xml
<sql id="TasteSearchFilter">
    AND (
        1 = 1
        <if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">
            AND (p.category_id != 'desk'
                 OR EXISTS (
                     SELECT 1
                     FROM product_desk_attributes da
                     WHERE da.product_id = p.product_id
                     AND da.taste_id IN
                     <foreach collection="deskTasteIds" item="v"
                              open="(" close=")" separator=",">#{v}</foreach>
                 ))
        </if>
        <if test="chairTasteIds != null and chairTasteIds.size() &gt; 0">
            AND (p.category_id != 'chair'
                 OR EXISTS (
                     SELECT 1
                     FROM product_chair_attributes ca
                     WHERE ca.product_id = p.product_id
                     AND ca.taste_id IN
                     <foreach collection="chairTasteIds" item="v"
                              open="(" close=")" separator=",">#{v}</foreach>
                 ))
        </if>
        <if test="storageTasteIds != null and storageTasteIds.size() &gt; 0">
            AND (p.category_id != 'storage'
                 OR EXISTS (
                     SELECT 1
                     FROM product_storage_attributes sa
                     WHERE sa.product_id = p.product_id
                     AND sa.taste_id IN
                     <foreach collection="storageTasteIds" item="v"
                              open="(" close=")" separator=",">#{v}</foreach>
                 ))
        </if>
    )
</sql>
```

### 3-2. フィルタロジックの説明

各 AND 条件は次の意味を持つ:

```
(カテゴリが「デスク」でない) OR (デスクテイストが一致する)
```

これにより:

- デスクの商品 → `deskTasteIds` に一致するものだけ通過
- チェア・収納の商品 → `deskTasteIds` の制約を受けない（`category_id != 'desk'` が TRUE）

複数カテゴリのテイストを同時指定した場合の挙動:

| deskTaste 指定 | chairTaste 指定 | 結果                                                                      |
| -------------- | --------------- | ------------------------------------------------------------------------- |
| あり           | なし            | デスクはテイスト一致のみ / チェア・収納はすべて通過                       |
| なし           | あり            | デスク・収納はすべて通過 / チェアはテイスト一致のみ                       |
| あり           | あり            | デスクはdeskTaste一致のみ / チェアはchairTaste一致のみ / 収納はすべて通過 |

### 3-3. `BaseProductWhere` への組み込み

```xml
<if test="hasTasteSearchFilter">
    <include refid="TasteSearchFilter"/>
</if>
```

`hasTasteSearchFilter` は `ProductRepository.buildSearchParams()` で設定されるブール値パラメータ。

---

## 4. SQLパラメータ対応表（改訂後）

`buildSearchParams()` から MyBatis へ渡すパラメータのうち、今回追加・変更するもの:

| パラメータ名           | 型              | 設定元                                    | 用途                           |
| ---------------------- | --------------- | ----------------------------------------- | ------------------------------ |
| `keyword`              | `String`        | `condition.keyword()` の trim 結果        | 商品コード完全一致・前方一致   |
| `keywordLike`          | `String`        | `"%" + keyword + "%"`                     | 商品名・バリエーション名・説明 |
| `hasTasteSearchFilter` | `boolean`       | category == null && 任意のtasteIds が非空 | TasteSearchFilter の ON/OFF    |
| `deskTasteIds`         | `List<Integer>` | `filter.deskTasteIds()`                   | デスクテイスト絞り込み         |
| `chairTasteIds`        | `List<Integer>` | `filter.chairTasteIds()`                  | チェアテイスト絞り込み         |
| `storageTasteIds`      | `List<Integer>` | `filter.storageTasteIds()`                | 収納テイスト絞り込み           |

> `deskTasteIds` / `chairTasteIds` / `storageTasteIds` は既存パラメータとして登録済み。
> `keyword` と `hasTasteSearchFilter` が新規追加となる。

---

## 5. `selectTopRankedProducts` — 変更なし

0件時のおすすめ商品取得には既存の `selectTopRankedProducts` をそのまま利用する。

```xml
<select id="selectTopRankedProducts" resultMap="ProductRankedRowMap">
    SELECT r.rank, p.product_id, p.product_name,
           MIN(pv.unit_price) AS min_price,
           MAX(pv.stock_quantity) AS max_stock,
           MIN(pv.product_code) AS product_code
    FROM popular_product_rankings r
    JOIN products p ON p.product_id = r.product_id
    JOIN product_variants pv ON pv.product_id = p.product_id
    WHERE r.ranking_date = (SELECT MAX(ranking_date) FROM popular_product_rankings)
    GROUP BY r.rank, p.product_id, p.product_name
    ORDER BY r.rank ASC
    LIMIT #{limit}
</select>
```

呼び出し側で `limit = 8` を渡す（FR-007）。

---

## 6. インデックス考慮事項

| 対象テーブル・カラム               | 現状                               | 推奨対応                                          |
| ---------------------------------- | ---------------------------------- | ------------------------------------------------- |
| `products.variation_name`          | インデックスなし（ILIKE 検索追加） | 検索頻度が増えたら `pg_trgm` GIN 索引の追加を検討 |
| `products.description`             | インデックスなし（ILIKE 検索追加） | TEXT 型は全文検索が多い場合 `tsvector` も選択肢   |
| `product_variants.product_code`    | 既存インデックスを確認             | `LOWER(product_code)` 式インデックスを追加推奨    |
| `product_desk_attributes.taste_id` | 確認要                             | EXISTS サブクエリ性能次第で追加検討               |

> 今回の実装フェーズでは既存インデックスの範囲内で対応し、性能要件未達時に追加検討とする（NFR 準拠）。
