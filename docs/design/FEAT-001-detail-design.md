# FEAT-001 詳細設計書 — 商品検索機能強化

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| 作成日 | 2026-03-15 |
| ステータス | Draft |
| 関連要件 | [FEAT-001-requirements.md](../issues/FEAT-001-requirements.md) |
| 関連基本設計 | [FEAT-001-basic-design.md](FEAT-001-basic-design.md) |

---

## 1. PostgreSQL 正規化関数の追加

### 1.1 新規ファイル

`sql/schema/search-functions.sql`（新規作成）

### 1.2 定義内容

```sql
-- 検索用正規化関数
-- 全角数字・英字（大小）・カタカナを半角に変換し、英字を小文字に統一する
CREATE OR REPLACE FUNCTION normalize_search_text(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
STRICT
AS $$
  SELECT LOWER(
    TRANSLATE(
      TRANSLATE(
        TRANSLATE(input,
          -- 全角数字 → 半角
          '０１２３４５６７８９',
          '0123456789'
        ),
        -- 全角英字（大文字） → 半角大文字
        'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ'
        'ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ',
        'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
        'abcdefghijklmnopqrstuvwxyz'
      ),
      -- 全角英字（小文字） → 半角
      'ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ',
      'abcdefghijklmnopqrstuvwxyz'
    )
  )
$$;
```

> **注意:** PostgreSQL の `TRANSLATE()` は1引数ずつの変換のため、ネストして記述するか、または `regexp_replace` を組み合わせで実装する。  
> 上記は概念的な定義であり、全角カタカナ→半角カタカナの変換（清音・濁音・半濁音の分離が必要）については、実装時に変換テーブルを別途定義すること。

---

## 2. `SearchKeywordNormalizer`（新規クラス）

### 2.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/util/SearchKeywordNormalizer.java`

### 2.2 設計

```
クラス: SearchKeywordNormalizer
種別: final class（インスタンス化不可）
役割: 検索キーワードを正規化する静的ユーティリティ

メソッド:
  + normalize(String keyword): String
      null または空文字の場合は null を返す
      以下の順に変換処理を行う:
        1. 全角数字（０-９ / U+FF10〜U+FF19）→ 半角数字（0-9）
        2. 全角英大文字（Ａ-Ｚ / U+FF21〜U+FF3A）→ 半角英小文字（a-z）
        3. 全角英小文字（ａ-ｚ / U+FF41〜U+FF5A）→ 半角英小文字（a-z）
        4. 全角カタカナ（ア-ン等）→ 半角カタカナ（ｱ-ﾝ等）
           ※濁点・半濁点付き文字は2文字（半角カタカナ + 半角濁点）に分解する
        5. 半角英大文字（A-Z）→ 半角英小文字（a-z）（String#toLowerCase(Locale.ROOT)）
      戻り値: 変換後の文字列（トリム処理は行わない）
```

### 2.3 変換テーブル（全角カタカナ → 半角カタカナ）

| 全角 | 半角 | 全角 | 半角 | 全角 | 半角 |
|-----|------|-----|------|-----|------|
| ア | ｱ | カ | ｶ | サ | ｻ |
| イ | ｲ | キ | ｷ | シ | ｼ |
| ウ | ｳ | ク | ｸ | ス | ｽ |
| エ | ｴ | ケ | ｹ | セ | ｾ |
| オ | ｵ | コ | ｺ | ソ | ｿ |
| ガ | ｶﾞ | ギ | ｷﾞ | … | … |
| ァ | ｧ | ッ | ｯ | ャ | ｬ |
| ィ | ｨ | ュ | ｭ | ヴ | ｳﾞ |
| ゥ | ｩ | ョ | ｮ | ン | ﾝ |
| （その他すべての全角カタカナを網羅すること） | | | | | |

---

## 3. `ProductSearchCondition` の変更

### 3.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`

### 3.2 変更前

```java
record ProductSearchCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<PriceBand> priceBands,
    List<Long> colorIds,
    ProductCategoryFilter categoryFilter,
    ProductSort sort,
    int page,
    int size,
    OffsetDateTime saleStartFrom
)
```

### 3.3 変更後

`tasteNames` フィールドを追加する。

```java
record ProductSearchCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<PriceBand> priceBands,
    List<Long> colorIds,
    ProductCategoryFilter categoryFilter,
    List<String> tasteNames,      // ★追加: 検索結果画面で選択されたテイスト名（正規化済み）
    ProductSort sort,
    int page,
    int size,
    OffsetDateTime saleStartFrom
)
```

- `tasteNames` は未選択の場合 `Collections.emptyList()` を設定する。
- SQL 側では `tasteNames` が空の場合はテイスト絞込を適用しない。

---

## 4. `ProductFilterOptionsBundle` の変更

### 4.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/model/product/ProductFilterOptionsBundle.java`

### 4.2 変更後

`searchTasteOptions` フィールドを追加する。

```java
record ProductFilterOptionsBundle(
    List<ColorFilterOption> colorOptions,
    List<CategoryFilterOption> deskTopShapeOptions,
    List<CategoryFilterOption> deskTasteOptions,
    List<CategoryFilterOption> chairFunctionOptions,
    List<CategoryFilterOption> chairMaterialOptions,
    List<CategoryFilterOption> chairTasteOptions,
    List<CategoryFilterOption> storageUsageOptions,
    List<CategoryFilterOption> storageTasteOptions,
    List<String> searchTasteOptions    // ★追加: 検索結果画面用テイスト名リスト（重複排除済み）
)
```

- `searchTasteOptions` は `List<String>`（テイスト表示名のリスト）とする。
- 値の例: `["ベーシック", "カジュアル", "シンプル", "モダン", "ナチュラル"]`（display_name の昇順）

---

## 5. `ProductListSearchService` の変更

### 5.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`

### 5.2 変更内容

**`buildCondition()` メソッドへの追加処理（検索結果画面用）:**

1. **キーワード正規化の追加**
   - 既存の `keyword` 取得後、`SearchKeywordNormalizer.normalize(keyword)` を呼び出して正規化する。
   - 正規化済みキーワードを `ProductSearchCondition.keyword` にセットする。

2. **テイストパラメータのバインド**
   - リクエストパラメータ `taste` を `List<String>` として受け取る。
   - `tasteNames` として `ProductSearchCondition` にセットする。

### 5.3 処理の詳細

```
buildCondition(リクエストパラメータ) {
    rawKeyword = リクエストから keyword を取得
    normalizedKeyword = SearchKeywordNormalizer.normalize(rawKeyword)  // ★追加

    tasteNames = リクエストから taste[] を取得（複数可）               // ★追加
    tasteNames = tasteNames == null ? emptyList() : tasteNames

    return ProductSearchCondition(
        categoryId: null,                     // 検索はカテゴリ指定なし
        keyword: normalizedKeyword,            // ★正規化済み
        inStockOnly: ...,
        priceBands: ...,
        colorIds: ...,
        categoryFilter: null,                 // 検索はカテゴリフィルタなし
        tasteNames: tasteNames,               // ★追加
        sort: ...,
        page: ...,
        size: ...
    )
}
```

---

## 6. `ProductFilterOptionRepository` の変更

### 6.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/repository/ProductFilterOptionRepository.java`

### 6.2 追加メソッド

```
findAllTasteDisplayNames(): List<String>

  処理:
    1. desk_tastes テーブルから display_name を全件取得
    2. chair_tastes テーブルから display_name を全件取得
    3. storage_tastes テーブルから display_name を全件取得
    4. 3つのリストをマージして重複排除（LinkedHashSet 等で順序を保持）
    5. ソート（display_name 昇順または定義順）して返す

  SQL（mapper に追加）:
    SELECT display_name FROM desk_tastes ORDER BY desk_taste_id
    SELECT display_name FROM chair_tastes ORDER BY chair_taste_id
    SELECT display_name FROM storage_tastes ORDER BY storage_taste_id
```

---

## 7. `ProductFilterOptionService` の変更

### 7.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

### 7.2 追加メソッド

```
loadSearchTasteOptions(): List<String>

  処理:
    1. findAllTasteDisplayNames() をリポジトリから呼び出す
    2. 結果をそのまま返す

  ※ ProductFilterOptionsBundle に searchTasteOptions としてセットして返す
```

---

## 8. `ProductRepository` の変更

### 8.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

### 8.2 変更内容

`search()` メソッドでSQLに渡すパラメータマップへ `tasteNames` を追加する。

```
params.put("tasteNames", condition.tasteNames());   // ★追加
```

---

## 9. `ProductMapper.xml` の変更

### 9.1 ファイルパス

`src/main/resources/mappers/ProductMapper.xml`

### 9.2 キーワード検索 SQL の変更

#### 変更前（既存の BaseProductWhere 内のキーワード条件）

```xml
<if test="keyword != null and keyword != ''">
  AND (
    p.product_name ILIKE #{keywordLike}
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE #{keywordLike}
    )
  )
</if>
```

#### 変更後

```xml
<if test="keyword != null and keyword != ''">
  AND (
    <!-- 商品名・シリーズバリエーション名・説明文: 部分一致（正規化比較） -->
    normalize_search_text(p.product_name)      LIKE LOWER(#{keywordLike})
    OR normalize_search_text(p.variation_name) LIKE LOWER(#{keywordLike})
    OR normalize_search_text(p.description)    LIKE LOWER(#{keywordLike})
    <!-- 商品コード: 完全一致 or 前方一致（正規化比較） -->
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND (
          <choose>
            <when test="productCodeExactMatch">
              normalize_search_text(pvk.product_code) = LOWER(#{keyword})
            </when>
            <otherwise>
              normalize_search_text(pvk.product_code) LIKE LOWER(#{keywordPrefix})
            </otherwise>
          </choose>
        )
    )
  )
</if>
```

#### パラメータ対応表

| パラメータ名 | 設定箇所 | 値の例 |
|-----------|---------|-------|
| `keyword` | `ProductRepository` | 正規化済みキーワード（例: `p0001-c01`） |
| `keywordLike` | `ProductRepository` | `%p0001-c01%`（前後に `%` を付与） |
| `keywordPrefix` | `ProductRepository` | `p001%`（後ろに `%` のみ付与） |
| `productCodeExactMatch` | `ProductRepository` | `keyword.length() >= 9` → `true` |

### 9.3 テイスト絞込フラグメントの追加（SearchTasteFilter）

```xml
<!-- 検索結果画面用テイスト絞込フラグメント -->
<sql id="SearchTasteFilter">
  <if test="tasteNames != null and !tasteNames.isEmpty()">
    AND (
      <!-- デスク: 選択されたテイスト名をデスクテイストマスタで探す -->
      EXISTS (
        SELECT 1
        FROM product_desk_attributes da
        JOIN desk_tastes dt ON dt.desk_taste_id = da.taste_id
        WHERE da.product_id = p.product_id
          AND dt.display_name IN
          <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR
      <!-- チェア: 選択されたテイスト名をチェアテイストマスタで探す -->
      EXISTS (
        SELECT 1
        FROM product_chair_attributes ca
        JOIN chair_tastes ct ON ct.chair_taste_id = ca.taste_id
        WHERE ca.product_id = p.product_id
          AND ct.display_name IN
          <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR
      <!-- 収納: 選択されたテイスト名を収納テイストマスタで探す -->
      EXISTS (
        SELECT 1
        FROM product_storage_attributes sa
        JOIN storage_tastes st ON st.storage_taste_id = sa.taste_id
        WHERE sa.product_id = p.product_id
          AND st.display_name IN
          <foreach item="name" collection="tasteNames" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
    )
  </if>
</sql>
```

### 9.4 `countProducts` / `selectProducts` への `SearchTasteFilter` 適用

`BaseProductWhere` フラグメント内（または `countProducts` / `selectProducts` 内）に以下を追加する。

```xml
<include refid="SearchTasteFilter"/>
```

- カテゴリ指定がある場合（カテゴリ一覧ページ）には `tasteNames` は null となり、フラグメントが無効化されるため既存の動作に影響しない。

---

## 10. `CatalogController` の変更

### 10.1 ファイルパス

`src/main/java/jp/co/skig/officeorder/web/CatalogController.java`

### 10.2 `searchResults()` メソッドの変更点

```
変更前:
  @GetMapping("/products/search")
  searchResults(keyword, inStockOnly, priceBand, color, sort, size, page, model)

変更後:
  @GetMapping("/products/search")
  searchResults(keyword, inStockOnly, priceBand, color,
                taste,            // ★追加: テイスト名（複数可）
                sort, size, page, model)
```

**追加処理:**
1. `productFilterOptionService.loadSearchTasteOptions()` を呼び出し、`searchTasteOptions` をモデルに追加する。
2. `taste` パラメータを `ProductListSearchService.buildCondition()` へ渡す。
3. 選択中のテイスト名リスト `selectedTasteNames` をモデルに追加する（テンプレートで選択状態表示に使用）。

---

## 11. `ProductMapper.java`（追加）

### 11.1 追加メソッド（テイストマスタ取得用）

```java
List<String> selectAllDeskTasteNames();
List<String> selectAllChairTasteNames();
List<String> selectAllStorageTasteNames();
```

対応SQL（`ProductMapper.xml` に追加）:

```xml
<select id="selectAllDeskTasteNames" resultType="string">
  SELECT display_name FROM desk_tastes ORDER BY desk_taste_id
</select>
<select id="selectAllChairTasteNames" resultType="string">
  SELECT display_name FROM chair_tastes ORDER BY chair_taste_id
</select>
<select id="selectAllStorageTasteNames" resultType="string">
  SELECT display_name FROM storage_tastes ORDER BY storage_taste_id
</select>
```

---

## 12. テスト観点

| # | テスト内容 | 確認項目 |
|---|----------|---------|
| T-01 | 全角数字キーワードで検索 | `１２３` → `123` に正規化されて検索される |
| T-02 | 全角英字キーワードで検索 | `ＡＢＣ` → `abc` に正規化されて検索される |
| T-03 | 全角カタカナで検索 | `ナチュラル` → `ﾅﾁｭﾗﾙ` に正規化されて検索される |
| T-04 | 半角英大文字で検索 | `ABC` → `abc` に正規化されて検索される |
| T-05 | 9文字以上の商品コードで検索 | 完全一致で絞り込まれる |
| T-06 | 9文字未満の商品コードで検索 | 前方一致で絞り込まれる |
| T-07 | variation_name を含む商品のキーワード検索 | ヒットする |
| T-08 | description を含む商品のキーワード検索 | ヒットする |
| T-09 | テイスト1件選択で絞り込み | 該当テイストを持つ全カテゴリ商品がヒットする |
| T-10 | テイスト複数選択（OR条件） | いずれかのテイストに合致する商品がヒットする |
| T-11 | テイスト選択なし | テイスト絞込が適用されず全件表示される |
| T-12 | テイスト選択肢の表示 | 5種（ベーシック・カジュアル・シンプル・モダン・ナチュラル）が表示される |
