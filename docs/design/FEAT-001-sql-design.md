# FEAT-001 商品検索機能強化 SQL設計書

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 詳細設計書 |
| 機能ID | FEAT-001 |
| 対象システム | office-order（オフィス家具ECサイト） |
| 対象DB | PostgreSQL 15 |
| 作成日 | 2026-03-24 |
| バージョン | 1.0 |
| 関連設計書 | [FEAT-001-basic-design.md](FEAT-001-basic-design.md) |

---

## 1. 変更対象ファイル

| ファイル | 変更種別 |
|---------|---------|
| `src/main/resources/mappers/ProductMapper.xml` | SQL フラグメント変更・SQL追加 |
| `src/main/java/.../mapper/ProductMapper.java` | Mapper インターフェース メソッド追加 |

---

## 2. 新規 SQL

### 2.1 `selectActiveSearchTasteOptions`（新規追加）

**目的：** 検索結果画面のテイスト絞り込み選択肢を取得する。3カテゴリのテイストマスタを名称で統合し、重複なし・ソート済みの一覧を返す。

**Mapper メソッド：**

```java
List<String> selectActiveSearchTasteOptions();
```

**SQL（ProductMapper.xml）：**

```xml
<select id="selectActiveSearchTasteOptions" resultType="string">
    SELECT DISTINCT display_name
    FROM (
        SELECT display_name FROM desk_tastes    WHERE is_active = TRUE
        UNION ALL
        SELECT display_name FROM chair_tastes   WHERE is_active = TRUE
        UNION ALL
        SELECT display_name FROM storage_tastes WHERE is_active = TRUE
    ) t
    ORDER BY display_name
</select>
```

**備考：**
- `UNION ALL` + 外側の `DISTINCT` で重複排除。`UNION` より性能が良い場合が多い。
- `ORDER BY display_name` で五十音順ソート（PostgreSQL のデフォルトロケール依存）。

---

## 3. 変更 SQL フラグメント

### 3.1 `BaseProductWhere`（変更）

#### 変更概要

| 変更点 | 内容 |
|--------|------|
| キーワード検索条件（商品コード部分） | `ILIKE #{keywordLike}` による中間一致 → `productCodeMatchMode` に応じた完全一致または前方一致へ変更 |
| キーワード検索条件（テキストフィールド） | `product_name` のみ → `product_name`, `variation_name`, `description` の3フィールドに拡張 |
| テイスト横断フィルタ | 新規追加（`hasSearchTasteFilter` が true の場合）|

#### 変更前（現行）

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

#### 変更後

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
            p.product_name    ILIKE #{keywordLike}
            OR p.variation_name ILIKE #{keywordLike}
            OR p.description    ILIKE #{keywordLike}
            OR EXISTS (
                SELECT 1
                FROM product_variants pvk
                WHERE pvk.product_id = p.product_id
                AND (
                    <choose>
                        <when test="productCodeMatchMode == 'exact'">
                            LOWER(pvk.product_code) = #{productCodeExact}
                        </when>
                        <otherwise>
                            LOWER(pvk.product_code) LIKE #{productCodePrefix}
                        </otherwise>
                    </choose>
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

#### 変更点の詳細

| 変更箇所 | 変更前 | 変更後 | 理由 |
|---------|--------|--------|------|
| テキスト検索対象 | `product_name` のみ | `product_name`, `variation_name`, `description` | 要件 3.2 |
| 商品コード一致方式 | `ILIKE #{keywordLike}`（中間一致） | `productCodeMatchMode` による完全一致または前方一致 | 要件 3.1 |
| テイスト横断フィルタ | なし | `hasSearchTasteFilter` で `SearchTasteFilter` を適用 | 要件 3.5 |

---

### 3.2 🆕 `SearchTasteFilter`（新規追加フラグメント）

**目的：** 検索結果画面のテイスト絞り込みを、3カテゴリの属性テーブルに横断して適用する。選択されたテイスト名称に対してカテゴリ別に EXISTS を実行し、OR で結合する。

```xml
<sql id="SearchTasteFilter">
    AND (
        EXISTS (
            SELECT 1
            FROM product_desk_attributes da
            JOIN desk_tastes dt ON dt.taste_id = da.taste_id
            WHERE da.product_id = p.product_id
            AND dt.display_name IN
            <foreach collection="searchTasteNames" item="name"
                     open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_chair_attributes ca
            JOIN chair_tastes ct ON ct.taste_id = ca.taste_id
            WHERE ca.product_id = p.product_id
            AND ct.display_name IN
            <foreach collection="searchTasteNames" item="name"
                     open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_storage_attributes sa
            JOIN storage_tastes st ON st.taste_id = sa.taste_id
            WHERE sa.product_id = p.product_id
            AND st.display_name IN
            <foreach collection="searchTasteNames" item="name"
                     open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
    )
</sql>
```

**設計上の注意：**
- `searchTasteNames` パラメータには必ず1件以上の値が入っている前提（`hasSearchTasteFilter = true` の場合のみ呼ばれる）。
- テイスト名称はアプリ層で有効な選択肢のみに正規化済み（`buildSearchCategoryFilter` を経由）であるため、不正な文字列の混入はない。ただしバインドパラメータを使用するため SQL インジェクションリスクはなし。

---

## 4. `buildSearchParams` へのパラメータ追加一覧

`ProductRepository.buildSearchParams()` が `Map<String, Object>` に追加するパラメータを示す。

### 4.1 キーワード関連パラメータ（変更）

| キー名 | 型 | 設定値 | 変更内容 |
|--------|------|--------|---------|
| `keywordLike` | `String` または `null` | `"%" + normalizedKeyword + "%"` | **変更なし**（名称・用途は同じ。値の生成元がアプリ層で正規化済みになる） |
| `productCodeMatchMode` | `String` | `"exact"` または `"prefix"` | **新規** |
| `productCodeExact` | `String` または `null` | `normalizedKeyword` | **新規**（完全一致用） |
| `productCodePrefix` | `String` または `null` | `normalizedKeyword + "%"` | **新規**（前方一致用） |

`keyword` が null または空白の場合、`keywordLike`, `productCodeExact`, `productCodePrefix` はすべて `null` を設定する（SQL 側の `<if test="keywordLike != null and keywordLike != ''">` で条件ブロックがスキップされる）。

### 4.2 テイスト横断フィルタ関連パラメータ（新規）

| キー名 | 型 | 設定値 |
|--------|------|--------|
| `searchTasteNames` | `List<String>` | `condition.categoryFilter().searchTasteNames()` |
| `hasSearchTasteFilter` | `boolean` | `!searchTasteNames.isEmpty()` |

---

## 5. SQL 実行パターン一覧

検索呼び出し時の SQL 実行パターンを示す。

### パターン A：キーワードなし・テイストなし（全件）

```sql
-- countProducts
SELECT COUNT(*) FROM products p
WHERE p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
  -- keyword, taste 条件なし
;
```

### パターン B：キーワードあり・商品コード完全一致でヒットあり

```sql
-- countProducts（1パス目：exact）
SELECT COUNT(*) FROM products p
WHERE p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
  AND (
      p.product_name    ILIKE '%keyword%'
      OR p.variation_name ILIKE '%keyword%'
      OR p.description    ILIKE '%keyword%'
      OR EXISTS (
          SELECT 1 FROM product_variants pvk
          WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) = 'keyword'   -- 完全一致
      )
  )
;
-- 件数 > 0 → selectProducts を実行（前方一致フォールバックなし）
```

### パターン C：キーワードあり・完全一致ヒットなし → 前方一致フォールバック

```sql
-- countProducts（1パス目：exact）→ 件数 = 0
-- countProducts（2パス目：prefix）
SELECT COUNT(*) FROM products p
WHERE p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
  AND (
      p.product_name    ILIKE '%keyword%'
      OR p.variation_name ILIKE '%keyword%'
      OR p.description    ILIKE '%keyword%'
      OR EXISTS (
          SELECT 1 FROM product_variants pvk
          WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) LIKE 'keyword%'  -- 前方一致
      )
  )
;
-- 件数 > 0 → selectProducts を実行
```

### パターン D：テイスト絞り込みあり

```sql
-- countProducts に SearchTasteFilter が追加される
SELECT COUNT(*) FROM products p
WHERE ...（上記 keyword 条件）...
  AND (
      EXISTS (SELECT 1 FROM product_desk_attributes da
              JOIN desk_tastes dt ON dt.taste_id = da.taste_id
              WHERE da.product_id = p.product_id
              AND dt.display_name IN ('ベーシック', 'モダン'))
      OR EXISTS (SELECT 1 FROM product_chair_attributes ca
              JOIN chair_tastes ct ON ct.taste_id = ca.taste_id
              WHERE ca.product_id = p.product_id
              AND ct.display_name IN ('ベーシック', 'モダン'))
      OR EXISTS (SELECT 1 FROM product_storage_attributes sa
              JOIN storage_tastes st ON st.taste_id = sa.taste_id
              WHERE sa.product_id = p.product_id
              AND st.display_name IN ('ベーシック', 'モダン'))
  )
;
```

---

## 6. インデックス影響分析

| テーブル | 追加クエリ | 既存インデックス | 影響 |
|---------|-----------|----------------|------|
| `product_variants` | `LOWER(pvk.product_code) = ?` | `product_code` は UNIQUE インデックスあり | `LOWER()` 適用でインデックスが活用されない。`product_code` は ASCII のみ想定のため性能影響は軽微（最大1,000件）。必要であれば関数インデックス `CREATE INDEX ON product_variants (LOWER(product_code))` の追加を検討。 |
| `product_desk_attributes` | `WHERE da.product_id = p.product_id` の EXISTS | `product_id` が PK（インデックスあり） | 影響なし |
| `product_chair_attributes` | 同上 | PK あり | 影響なし |
| `product_storage_attributes` | 同上 | PK あり | 影響なし |
| `desk_tastes` / `chair_tastes` / `storage_tastes` | `JOIN` + `display_name IN (...)` | `display_name` にインデックスなし | 各テーブルの行数は数十件程度のため、シーケンシャルスキャンでも問題なし |

**性能目標（参考）：** 本番環境・P95 で TTFB 2秒以内。1,000件規模でのフルスキャンでも十分達成可能と想定。
