# FEAT-001 SQL・Mapper 設計書

## 1. 変更対象

| ファイル | 変更箇所 | 変更種別 |
|---------|---------|---------|
| `src/main/resources/mappers/ProductMapper.xml` | フラグメント `BaseProductWhere` | 変更 |
| `src/main/resources/mappers/ProductMapper.xml` | フラグメント `SearchTasteFilter`（新規） | 追加 |
| `jp.co.skig.officeorder.repository.ProductRepository` | `buildSearchParams` | 変更 |
| `jp.co.skig.officeorder.repository.ProductRepository` | `toKeywordLike` / `toKeywordExact` / `toKeywordPrefix` | 変更・追加 |

---

## 2. キーワード検索条件の変更

### 2.1 現状

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

### 2.2 変更後

```xml
<if test="keywordLike != null and keywordLike != ''">
    AND (
        p.product_name     ILIKE #{keywordLike}
        OR p.variation_name ILIKE #{keywordLike}
        OR p.description    ILIKE #{keywordLike}
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
```

### 2.3 変更ポイント解説

| 変更点 | 内容 |
|-------|-----|
| `p.variation_name ILIKE #{keywordLike}` を追加 | シリーズバリエーション名を検索対象に追加（部分一致） |
| `p.description ILIKE #{keywordLike}` を追加 | 説明文を検索対象に追加（部分一致） |
| 商品コードの完全一致を `=` 演算子に変更 | `ILIKE #{keywordLike}`（中間一致）→ `= #{keywordExact}`（完全一致、大文字区別あり） OR `ILIKE #{keywordPrefix}`（前方一致、大文字区別なし） |

> **商品コードの一致方式変更理由**: コードに含まれる一部の数字が別商品コードのコード中間部分に意図せず一致するため、
> 完全一致または前方一致に限定する。

> **`keywordLike` の存在チェックを条件として使用**: `keywordExact` および `keywordPrefix` は
> `keywordLike` が null でない場合に同時に設定されるため、`keywordLike != null` の条件ブロック内で参照する。

---

## 3. テイスト絞込フラグメントの追加

### 3.1 新規フラグメント `SearchTasteFilter`

カテゴリ横断のテイスト絞込を行うフラグメント。  
任意のカテゴリ属性テーブル（デスク・チェア・ストレージ）のいずれかでテイストが一致すれば対象とする。

```xml
<sql id="SearchTasteFilter">
    AND (
        EXISTS (
            SELECT 1
            FROM product_desk_attributes xda
            WHERE xda.product_id = p.product_id
              AND xda.taste_id IN
            <foreach collection="tasteIds" item="tid" open="(" close=")" separator=",">
                #{tid}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_chair_attributes xca
            WHERE xca.product_id = p.product_id
              AND xca.taste_id IN
            <foreach collection="tasteIds" item="tid" open="(" close=")" separator=",">
                #{tid}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_storage_attributes xsa
            WHERE xsa.product_id = p.product_id
              AND xsa.taste_id IN
            <foreach collection="tasteIds" item="tid" open="(" close=")" separator=",">
                #{tid}
            </foreach>
        )
    )
</sql>
```

### 3.2 `BaseProductWhere` へのフラグメント組み込み

```xml
<if test="hasSearchTasteFilter">
    <include refid="SearchTasteFilter"/>
</if>
```

`hasSearchTasteFilter` は `ProductRepository#buildSearchParams` でセットする  
（`!tasteIds.isEmpty()` が true の場合に `true` となる）。

---

## 4. `BaseProductWhere` 変更後全文

変更前後の対比:

### 変更前

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
            p.product_name ILIKE #{keywordLike}
            OR EXISTS (
                SELECT 1
                FROM product_variants pvk
                WHERE pvk.product_id = p.product_id
                  AND pvk.product_code ILIKE #{keywordLike}
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

### 変更後

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
            p.product_name      ILIKE #{keywordLike}
            OR p.variation_name ILIKE #{keywordLike}
            OR p.description    ILIKE #{keywordLike}
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
    <if test="hasDeskFilter">
        <include refid="DeskAttributeFilter"/>
    </if>
    <if test="hasChairFilter">
        <include refid="ChairAttributeFilter"/>
    </if>
    <if test="hasStorageFilter">
        <include refid="StorageAttributeFilter"/>
    </if>
    <if test="hasSearchTasteFilter">
        <include refid="SearchTasteFilter"/>
    </if>
</sql>
```

---

## 5. パラメータマッピング

`ProductRepository#buildSearchParams` が `ProductSearchCondition` から構築する MyBatis パラメータマップの変更点。

### 5.1 追加パラメータ

| パラメータキー | 型 | 設定元 | 内容 |
|-------------|---|-------|-----|
| `keywordExact` | `String` | `toKeywordExact(condition.keyword())` | 完全一致用正規化キーワード（商品コードに `=` 演算子で使用） |
| `keywordPrefix` | `String` | `toKeywordPrefix(condition.keyword())` | 前方一致用正規化キーワード（`keyword%`） |
| `tasteIds` | `List<Integer>` | `condition.tasteIds()` | カテゴリ横断テイストIDリスト |
| `hasSearchTasteFilter` | `boolean` | `!tasteIds.isEmpty()` | 横断テイスト絞込フラグ |

### 5.2 変更パラメータ

| パラメータキー | 変更前 | 変更後 |
|-------------|-------|-------|
| `keywordLike` | `"%" + keyword + "%"` | `"%" + KeywordNormalizer.normalize(keyword).trim() + "%"` |

### 5.3 パラメータ全体一覧（変更後）

| パラメータキー | 型 | 内容 |
|-------------|---|-----|
| `now` | `OffsetDateTime` | 販売期間判定用現在時刻 |
| `saleStartFrom` | `OffsetDateTime` | 新着用販売開始日時下限（null 許容） |
| `categoryId` | `String` | カテゴリID（null 許容） |
| `keywordLike` | `String` | 部分一致用キーワード（`%keyword%`、null 許容） |
| `keywordExact` | `String` | **追加** 完全一致用キーワード（null 許容） |
| `keywordPrefix` | `String` | **追加** 前方一致用キーワード（`keyword%`、null 許容） |
| `inStockOnly` | `boolean` | 在庫ありのみフラグ |
| `colorIds` | `List<Long>` | 絞込カラーIDリスト |
| `priceRanges` | `List<RangeValue>` | 価格レンジリスト |
| `sort` | `String` | ソート順 |
| `hasVariantFilter` | `boolean` | バリアント絞込フラグ |
| `hasDeskFilter` | `boolean` | デスク属性絞込フラグ |
| `hasChairFilter` | `boolean` | チェア属性絞込フラグ |
| `hasStorageFilter` | `boolean` | ストレージ属性絞込フラグ |
| `hasSearchTasteFilter` | `boolean` | **追加** 横断テイスト絞込フラグ |
| `tasteIds` | `List<Integer>` | **追加** 横断テイストIDリスト |
| `deskTopShapeIds` | `List<Integer>` | デスク天板形状IDリスト |
| `deskTasteIds` | `List<Integer>` | デスクテイストIDリスト（カテゴリ一覧専用） |
| `deskWidthRanges` | `List<RangeValue>` | デスク幅レンジリスト |
| `deskDepthRanges` | `List<RangeValue>` | デスク奥行レンジリスト |
| `deskHeightRanges` | `List<RangeValue>` | デスク高さレンジリスト |
| `chairFunctionIds` | `List<Integer>` | チェア機能IDリスト |
| `chairMaterialIds` | `List<Integer>` | チェア素材IDリスト |
| `chairTasteIds` | `List<Integer>` | チェアテイストIDリスト（カテゴリ一覧専用） |
| `storageUsageIds` | `List<Integer>` | ストレージ用途IDリスト |
| `storageTasteIds` | `List<Integer>` | ストレージテイストIDリスト（カテゴリ一覧専用） |
| `limit` | `int` | 取得件数（`selectProducts` 時のみ） |
| `offset` | `long` | 取得オフセット（`selectProducts` 時のみ） |

---

## 6. SQL 動作パターン一覧

### 6.1 キーワード検索

| 入力 | `keywordLike` | `keywordExact` | `keywordPrefix` | SQL 動作 |
|-----|-------------|--------------|----------------|---------|
| `null` / 空 | `null` | `null` | `null` | キーワード条件なし（全件対象） |
| `desk` | `%desk%` | `desk` | `desk%` | 商品名・variation_name・description 中間一致、商品コード `=` 完全一致 or ILIKE 前方一致 |
| `１２３` (全角) | `%123%` | `123` | `123%` | 正規化後 `123` として評価 |
| `ｵﾌｨｽ` (半角カタカナ) | `%オフィス%` | `オフィス` | `オフィス%` | 正規化後 `オフィス` として評価 |

### 6.2 テイスト絞込

| カテゴリ | `hasSearchTasteFilter` | `hasDeskFilter` | 動作 |
|---------|-----------------|----------------|-----|
| 未指定（横断） | `true` | `false` | `SearchTasteFilter` 適用（デスク OR チェア OR ストレージ） |
| `desk` | `true` | true（`deskTasteIds`を利用） | `DeskAttributeFilter` の `taste_id` 条件が適用 |

> **注意**: `/products/search`（SRCH002）は常に `categoryId = null` で呼び出されるため、
> `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` はすべて `false` になる。
> テイスト絞込は `SearchTasteFilter` のみで処理される。
