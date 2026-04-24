# FEAT-001 SQL設計書（ProductMapper.xml）

| 項目 | 内容 |
|------|------|
| Issue ID | FEAT-001 |
| ドキュメント種別 | SQL設計書 |
| 作成日 | 2026-04-24 |

---

## 1. 変更概要

| 区分 | 対象 | 内容 |
|------|------|------|
| 変更 | `<sql id="BaseProductWhere">` | キーワード条件変更（検索対象拡張・正規化・商品コード一致方式変更）、テイスト絞込条件追加 |
| 変更 | `<sql id="DeskAttributeFilter">` | `deskTasteIds` → `tasteIds` に変更 |
| 変更 | `<sql id="ChairAttributeFilter">` | `chairTasteIds` → `tasteIds` に変更 |
| 変更 | `<sql id="StorageAttributeFilter">` | `storageTasteIds` → `tasteIds` に変更 |
| 新規 | `<sql id="TasteFilter">` | カテゴリ横断テイスト絞込フラグメント |
| 削除 | `<select id="selectActiveDeskTasteOptions">` | 旧テーブル参照 |
| 削除 | `<select id="selectActiveChairTasteOptions">` | 旧テーブル参照 |
| 削除 | `<select id="selectActiveStorageTasteOptions">` | 旧テーブル参照 |
| 新規 | `<select id="selectActiveTasteOptions">` | 統合テーブル参照 |

---

## 2. TRANSLATE 式について

DB側の全角→半角正規化には PostgreSQL の `TRANSLATE` 関数を用いる。  
以下の定数文字列をすべての対象カラムに適用する。

| 引数 | 値 | 文字数 |
|------|---|--------|
| `from` | `ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ０１２３４５６７８９` | 62文字 |
| `to` | `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789` | 62文字 |

`ILIKE` により大文字小文字を不区別にする（Java側でキーワードの小文字化も行うが、DB側でも冗長に吸収できる）。

---

## 3. `BaseProductWhere` フラグメント（変更後全文）

```xml
<sql id="BaseProductWhere">
    p.sale_start_at &lt;= #{now}
    AND (p.sale_end_at IS NULL OR p.sale_end_at &gt; #{now})
    <if test="saleStartFrom != null">
        AND p.sale_start_at &gt;= #{saleStartFrom}
    </if>
    <if test="categoryId != null and categoryId != ''">
        AND p.category_id = #{categoryId}
    </if>
    <if test="keywordLike != null and keywordLike != ''">
        AND (
            TRANSLATE(p.product_name,
                'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ０１２３４５６７８９',
                'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
            ) ILIKE #{keywordLike}
            OR TRANSLATE(p.variation_name,
                'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ０１２３４５６７８９',
                'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
            ) ILIKE #{keywordLike}
            OR TRANSLATE(p.description,
                'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ０１２３４５６７８９',
                'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
            ) ILIKE #{keywordLike}
            OR EXISTS (
                SELECT 1
                FROM product_variants pvk
                WHERE pvk.product_id = p.product_id
                AND (
                    pvk.product_code = #{keywordExact}
                    OR pvk.product_code ILIKE #{keywordPrefix}
                )
            )
        )
    </if>
    <if test="hasVariantFilter">
        AND EXISTS (
            SELECT 1
            FROM product_variants pvf
            WHERE pvf.product_id = p.product_id
            <include refid="VariantFilterPvf"/>
        )
    </if>
    <if test="hasSearchTasteFilter">
        <include refid="TasteFilter"/>
    </if>
    <if test="hasDeskFilter">
        <include refid="DeskAttributeFilter"/>
    </if>
    <if test="hasChairFilter">
        <include refid="ChairAttributeFilter"/>
    </if>
    <if test="hasStorageFilter">
        <include refid="StorageAttributeFilter"/>
    </if>
</sql>
```

### パラメータ対応表

| パラメータ名 | 型 | 内容 | 変更 |
|------------|----|----|------|
| `now` | `OffsetDateTime` | 現在日時 | 変更なし |
| `saleStartFrom` | `OffsetDateTime` | 新着判定起点日時 | 変更なし |
| `categoryId` | `String` | カテゴリID | 変更なし |
| `keywordLike` | `String` | 正規化済み部分一致パターン（`%abc%`） | **変更**（正規化適用後の値） |
| `keywordExact` | `String` | 正規化済み商品コード完全一致用 | **新規** |
| `keywordPrefix` | `String` | 正規化済み商品コード前方一致パターン（`abc%`） | **新規** |
| `hasVariantFilter` | `boolean` | バリアントフィルタ有無 | 変更なし |
| `hasSearchTasteFilter` | `boolean` | 横断テイストフィルタ有無 | **新規** |
| `hasDeskFilter` | `boolean` | デスクフィルタ有無 | 変更なし |
| `hasChairFilter` | `boolean` | チェアフィルタ有無 | 変更なし |
| `hasStorageFilter` | `boolean` | 収納家具フィルタ有無 | 変更なし |

---

## 4. `TasteFilter` フラグメント（新規）

カテゴリ横断テイスト絞込。検索結果画面（`/products/search`）でのみ使用する。

```xml
<sql id="TasteFilter">
    AND (
        EXISTS (
            SELECT 1
            FROM product_desk_attributes da
            WHERE da.product_id = p.product_id
            AND da.taste_id IN
            <foreach collection="searchTasteIds" item="v" open="(" close=")" separator=",">
                #{v}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_chair_attributes ca
            WHERE ca.product_id = p.product_id
            AND ca.taste_id IN
            <foreach collection="searchTasteIds" item="v" open="(" close=")" separator=",">
                #{v}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_storage_attributes sa
            WHERE sa.product_id = p.product_id
            AND sa.taste_id IN
            <foreach collection="searchTasteIds" item="v" open="(" close=")" separator=",">
                #{v}
            </foreach>
        )
    )
</sql>
```

| パラメータ名 | 型 | 内容 |
|------------|----|----|
| `searchTasteIds` | `List<Long>` | 検索結果画面で選択されたテイストID一覧 |

---

## 5. `DeskAttributeFilter` フラグメント（変更）

`deskTasteIds` → `tasteIds` に変更する。

```xml
<sql id="DeskAttributeFilter">
    AND EXISTS (
        SELECT 1
        FROM product_desk_attributes da
        WHERE da.product_id = p.product_id
        <if test="deskTopShapeIds != null and deskTopShapeIds.size() &gt; 0">
            AND da.top_shape_id IN
            <foreach collection="deskTopShapeIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
        <if test="tasteIds != null and tasteIds.size() &gt; 0">
            AND da.taste_id IN
            <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
        <if test="deskWidthRanges != null and deskWidthRanges.size() &gt; 0">
            AND (
            <foreach collection="deskWidthRanges" item="range" separator=" OR ">
                <choose>
                    <when test="range.minInclusive == null">da.width_mm &lt;= #{range.maxInclusive}</when>
                    <when test="range.maxInclusive == null">da.width_mm &gt;= #{range.minInclusive}</when>
                    <otherwise>(da.width_mm &gt;= #{range.minInclusive} AND da.width_mm &lt;= #{range.maxInclusive})</otherwise>
                </choose>
            </foreach>
            )
        </if>
        <if test="deskDepthRanges != null and deskDepthRanges.size() &gt; 0">
            AND (
            <foreach collection="deskDepthRanges" item="range" separator=" OR ">
                <choose>
                    <when test="range.minInclusive == null">da.depth_mm &lt;= #{range.maxInclusive}</when>
                    <when test="range.maxInclusive == null">da.depth_mm &gt;= #{range.minInclusive}</when>
                    <otherwise>(da.depth_mm &gt;= #{range.minInclusive} AND da.depth_mm &lt;= #{range.maxInclusive})</otherwise>
                </choose>
            </foreach>
            )
        </if>
        <if test="deskHeightRanges != null and deskHeightRanges.size() &gt; 0">
            AND (
            <foreach collection="deskHeightRanges" item="range" separator=" OR ">
                <choose>
                    <when test="range.minInclusive == null">da.height_mm &lt;= #{range.maxInclusive}</when>
                    <when test="range.maxInclusive == null">da.height_mm &gt;= #{range.minInclusive}</when>
                    <otherwise>(da.height_mm &gt;= #{range.minInclusive} AND da.height_mm &lt;= #{range.maxInclusive})</otherwise>
                </choose>
            </foreach>
            )
        </if>
    )
</sql>
```

---

## 6. `ChairAttributeFilter` フラグメント（変更）

`chairTasteIds` → `tasteIds` に変更する。

```xml
<sql id="ChairAttributeFilter">
    AND EXISTS (
        SELECT 1
        FROM product_chair_attributes ca
        WHERE ca.product_id = p.product_id
        <if test="chairFunctionIds != null and chairFunctionIds.size() &gt; 0">
            AND ca.function_id IN
            <foreach collection="chairFunctionIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
        <if test="chairMaterialIds != null and chairMaterialIds.size() &gt; 0">
            AND ca.material_id IN
            <foreach collection="chairMaterialIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
        <if test="tasteIds != null and tasteIds.size() &gt; 0">
            AND ca.taste_id IN
            <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
    )
</sql>
```

---

## 7. `StorageAttributeFilter` フラグメント（変更）

`storageTasteIds` → `tasteIds` に変更する。

```xml
<sql id="StorageAttributeFilter">
    AND EXISTS (
        SELECT 1
        FROM product_storage_attributes sa
        WHERE sa.product_id = p.product_id
        <if test="storageUsageIds != null and storageUsageIds.size() &gt; 0">
            AND sa.usage_id IN
            <foreach collection="storageUsageIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
        <if test="tasteIds != null and tasteIds.size() &gt; 0">
            AND sa.taste_id IN
            <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
                #{value}
            </foreach>
        </if>
    )
</sql>
```

---

## 8. `selectActiveTasteOptions`（新規）

統合テイストマスタから有効な選択肢を取得する。

```xml
<select id="selectActiveTasteOptions" resultMap="ProductFilterOptionRowMap">
    SELECT
        taste_id    AS option_id,
        display_name
    FROM tastes
    WHERE is_active = TRUE
    ORDER BY sort_order ASC, taste_id ASC
</select>
```

---

## 9. 削除クエリ一覧

以下の3クエリを `ProductMapper.xml` から削除する。

```xml
<!-- 削除 -->
<select id="selectActiveDeskTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM desk_tastes ...
</select>

<!-- 削除 -->
<select id="selectActiveChairTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM chair_tastes ...
</select>

<!-- 削除 -->
<select id="selectActiveStorageTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM storage_tastes ...
</select>
```

---

## 10. パラメータ渡し整理（`ProductRepository.buildSearchParams` の params map）

変更前後の params map キーの差分をまとめる。

| キー | 変更前 | 変更後 | 備考 |
|------|--------|--------|------|
| `keywordLike` | `%keyword%`（正規化なし） | `%normalizedKeyword%` | Java 側で正規化済み |
| `keywordExact` | なし | `normalizedKeyword` | 新規 |
| `keywordPrefix` | なし | `normalizedKeyword%` | 新規 |
| `deskTasteIds` | `List<Integer>` | **削除** | |
| `chairTasteIds` | `List<Integer>` | **削除** | |
| `storageTasteIds` | `List<Integer>` | **削除** | |
| `tasteIds` | なし | `List<Long>`（`filter.tasteIds()`） | カテゴリ一覧ページ用 |
| `searchTasteIds` | なし | `List<Long>`（`condition.tasteIds()`） | 検索結果画面用 |
| `hasSearchTasteFilter` | なし | `boolean` | 新規 |
