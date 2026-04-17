# FEAT-001 商品検索機能強化 詳細設計書

作成日: 2026-04-17  
最終更新: 2026-04-17  
対象要件: `docs/issues/FEAT-001-requirements.md`

---

## 1. 概要

### 1-1. 変更目的

本書は FEAT-001「商品検索機能強化」の実装に必要なコンポーネント設計を定義する。  
変更の主眼は①検索対象フィールドの拡張・一致方式の見直し、②複数キーワードの AND 検索、③全角/半角正規化、④テイスト絞込フィルタの追加の 4 点である。

### 1-2. 変更コンポーネント一覧

| レイヤー | クラス / ファイル | 変更区分 |
|---------|-----------------|---------|
| Common | `NormalizationUtils` | **新規** |
| Model | `ProductSearchCondition` | **変更**（フィールド追加） |
| Mapper interface | `ProductMapper` | **変更**（メソッド追加） |
| Mapper row | `KeywordSearchParam` | **新規** |
| Mapper XML | `ProductMapper.xml` | **変更**（SQL 修正・追加） |
| Repository | `ProductRepository` | **変更**（パラメータ生成ロジック修正） |
| Repository | `ProductFilterOptionRepository` | **変更**（メソッド追加） |
| Service | `ProductService` | **変更**（正規化呼び出し追加、引数追加） |
| Service | `ProductListSearchService` | **変更**（引数追加、taste 正規化） |
| Service | `ProductFilterOptionService` | **変更**（メソッド追加） |
| Controller | `CatalogController` | **変更**（パラメータ・モデル属性追加） |
| Template | `product-list-search-results.html` | **変更**（テイストフィルタ UI 追加） |

---

## 2. アーキテクチャ変更概要

### 2-1. 既存の呼び出しフロー（キーワード検索）

```
CatalogController.searchResults()
  └─ ProductListSearchService.buildCondition()
       └─ ProductService.buildCondition()
            └─ ProductSearchCondition（keyword: String）
                 └─ ProductService.search()
                      └─ ProductRepository.search()
                           └─ buildSearchParams() → params["keywordLike"] = "%keyword%"
                                └─ ProductMapper.selectProducts(params) / countProducts(params)
```

### 2-2. 変更後の呼び出しフロー

```
CatalogController.searchResults()
  ├─ ProductFilterOptionService.loadUnifiedTasteDisplayNames()   ← NEW
  └─ ProductListSearchService.buildCondition()                   ← 引数に tasteDisplayNames 追加
       └─ ProductFilterOptionService.normalizeTasteDisplayNames() ← NEW
       └─ ProductService.buildCondition()
            ├─ NormalizationUtils.normalizeForSearch(keyword)    ← NEW
            └─ ProductSearchCondition（keyword: String, tasteDisplayNames: List<String>）← フィールド追加
                 └─ ProductService.search()
                      └─ ProductRepository.search()
                           └─ buildSearchParams()
                                ├─ buildKeywords(keyword)        ← NEW（空白分割、KeywordSearchParam リスト）
                                ├─ params["keywords"] = [...]    ← 変更
                                └─ params["tasteDisplayNames"]   ← NEW
                                     └─ ProductMapper.selectProducts / countProducts
```

---

## 3. 新規クラス設計

### 3-1. `NormalizationUtils`

**配置パッケージ:** `jp.co.skig.officeorder.common`  
**種別:** final class（インスタンス化不可、static メソッドのみ）

#### 責務

検索キーワードの全角/半角正規化を行う共通ユーティリティ。  
変換はサービス層から呼び出し、DB カラム値は変換しない。

#### 主要メソッド

```
normalizeForSearch(String text) : String
```

入力に対して以下の順で変換を適用する。

| 処理順 | 変換内容 | 例 |
|:----:|---------|-----|
| 1 | 半角カタカナ（濁点・半濁点の合成を含む）→ 全角カタカナ | `ﾃﾞｽｸ` → `デスク` |
| 2 | 全角数字 → 半角 | `１２３` → `123` |
| 3 | 全角英大文字 → 半角 | `ＡＢＣ` → `ABC` |
| 4 | 全角英小文字 → 半角 | `ａｂｃ` → `abc` |
| 5 | 全角ハイフン（`－` U+FF0D）→ 半角 `-` | `DSK－001` → `DSK-001` |
| 6 | 全角括弧 `（）` → 半角 `()` | `（3段）` → `(3段)` |

**null / 空文字列:** 入力のまま返す（変換しない）。

#### 半角カタカナ変換の実装方針

半角カタカナには濁点（`ﾞ` U+FF9E）・半濁点（`ﾟ` U+FF9F）が続く 2 文字の組み合わせが存在する（例: `ﾃﾞ` → `デ`）。  
実装では文字列をインデックスループで走査し、現在文字と次文字の組み合わせを先読みして合成カタカナへ変換する。  
単独の半角カタカナはそのまま全角カタカナへ変換する（JIS 規格の対応表に基づく 1:1 マッピングテーブルを使用）。

#### テスト対象

- `NormalizationUtilsTest`（ユニットテスト）で全変換パターンを網羅する。  
  詳細は `FEAT-001-test-design.md` を参照。

---

### 3-2. `KeywordSearchParam`

**配置パッケージ:** `jp.co.skig.officeorder.mapper.row`  
**種別:** record

#### 責務

MyBatis の `<foreach>` に渡すキーワード 1 件分のLIKE パターンを保持する。

```java
public record KeywordSearchParam(
    String keywordLike,   // 部分一致パターン: "%{normalized_keyword}%"
    String codeLike       // 前方一致パターン: "{normalized_keyword}%"
) {}
```

`ProductRepository.buildKeywords()` で生成し、params map の `"keywords"` キーに `List<KeywordSearchParam>` として格納する。

---

## 4. モデル変更設計

### 4-1. `ProductSearchCondition`

**変更種別:** レコードフィールド追加（末尾に追加）

```java
// 変更前（末尾が saleStartFrom）
public record ProductSearchCondition(
    ...
    OffsetDateTime saleStartFrom
)

// 変更後（tasteDisplayNames を追加）
public record ProductSearchCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<PriceBand> priceBands,
    List<Long> colorIds,
    ProductCategoryFilter categoryFilter,
    ProductSort sort,
    int page,
    int size,
    OffsetDateTime saleStartFrom,
    List<String> tasteDisplayNames   // NEW: カテゴリ横断テイスト表示名リスト
)
```

**影響箇所（`ProductSearchCondition` のインスタンス生成箇所）:**

| クラス | メソッド | 対応内容 |
|-------|---------|---------|
| `ProductService` | `buildCondition()` | 最後の引数に `tasteDisplayNames` を追加 |
| `ProductService` | `normalize()` | `condition.tasteDisplayNames()` をそのまま引き継ぐ |
| `ProductListSearchService` | `searchWithPageCorrection()` | `corrected` 生成時に `condition.tasteDisplayNames()` を引き継ぐ |

---

## 5. Controller 層設計

### 5-1. `CatalogController.searchResults()`

#### 変更内容

**追加リクエストパラメータ:**

```java
@RequestParam(name = "taste", required = false) List<String> rawTasteDisplayNames
```

**追加処理:**

```
// テイスト統合候補一覧をロード
List<String> tasteFilterOptions = productFilterOptionService.loadUnifiedTasteDisplayNames();
```

**`buildCondition()` 呼び出し変更:**  
`buildCondition()` に `rawTasteDisplayNames` と `tasteFilterOptions` を渡す（後述の `ProductListSearchService` 変更を参照）。

**追加モデル属性:**

| 属性名 | 型 | 内容 |
|-------|-----|------|
| `tasteFilterOptions` | `List<String>` | テイスト絞込チェックボックスの選択肢（display_name の一覧） |
| `selectedTasteDisplayNames` | `List<String>` | 現在選択中のテイスト display_name リスト |

**ソート/ページネーション form への追加:**  
ソート選択 form・ページネーションリンクに `taste` パラメータを引き継ぐ（template 側の修正と連動）。

---

## 6. Service 層設計

### 6-1. `ProductService`

#### 変更箇所①: キーワード正規化の追加

`buildCondition()` のキーワード前処理を以下のように変更する。

```java
// 変更前
String trimmedKeyword = keyword == null ? null : keyword.trim();

// 変更後
String trimmedKeyword = keyword == null ? null
    : NormalizationUtils.normalizeForSearch(keyword.trim());
```

これにより `ProductSearchCondition.keyword` には正規化済みキーワードが格納される。

#### 変更箇所②: `buildCondition()` に `tasteDisplayNames` を追加

最終的なオーバーロード（`saleStartFrom` を含む形）に `List<String> tasteDisplayNames` 引数を追加し、`ProductSearchCondition` 生成時に渡す。

既存のオーバーロードからは `null` を渡すデフォルト引数としてチェーンする。

#### 変更箇所③: `normalize()` に `tasteDisplayNames` を引き継ぐ

```java
private ProductSearchCondition normalize(ProductSearchCondition condition) {
    ...
    return new ProductSearchCondition(
        ...
        condition.saleStartFrom(),
        condition.tasteDisplayNames() == null ? List.of() : condition.tasteDisplayNames()  // NEW
    );
}
```

---

### 6-2. `ProductListSearchService`

#### `buildCondition()`（カテゴリ固有条件あり版）への変更

**追加引数:**
```
List<String> rawTasteDisplayNames
List<String> allowedTasteDisplayNames  // ProductFilterOptionService から渡す
```

**追加処理:**
```
List<String> selectedTasteDisplayNames =
    productFilterOptionService.normalizeTasteDisplayNames(rawTasteDisplayNames, allowedTasteDisplayNames);
```

**`ProductService.buildCondition()` 呼び出しに `selectedTasteDisplayNames` を追加。**

#### `searchWithPageCorrection()` の変更

ページ補正時に生成する `corrected` condition に `tasteDisplayNames` を引き継ぐ。

---

### 6-3. `ProductFilterOptionService`

#### 新規メソッド①: `loadUnifiedTasteDisplayNames()`

**戻り値:** `List<String>`  
**処理:** `productFilterOptionRepository.findUnifiedTasteDisplayNames()` を呼び出し、統合テイスト display_name 一覧を返す。

#### 新規メソッド②: `normalizeTasteDisplayNames()`

**引数:**
- `List<String> rawTasteDisplayNames` ― controller から受け取った生の値  
- `List<String> allowedDisplayNames` ― DB から取得した有効な display_name 一覧

**処理:** `rawTasteDisplayNames` の各要素が `allowedDisplayNames` に含まれるもののみを通過させる。  
**目的:** 不正な入力値（SQL インジェクションの可能性、存在しないテイスト名）を除去する。

```java
public List<String> normalizeTasteDisplayNames(
        List<String> rawTasteDisplayNames,
        List<String> allowedDisplayNames) {
    if (rawTasteDisplayNames == null || rawTasteDisplayNames.isEmpty()) {
        return List.of();
    }
    Set<String> allowedSet = new HashSet<>(allowedDisplayNames);
    return rawTasteDisplayNames.stream()
        .filter(name -> name != null && allowedSet.contains(name))
        .distinct()
        .toList();
}
```

> **セキュリティ上の注意:** テイスト display_name は URL パラメータ経由で渡されるため、DB から取得した許可リストによるホワイトリストフィルタリングを必ず実施すること。IN 句に渡す前に `normalizeTasteDisplayNames()` を経由させる。

---

## 7. Repository 層設計

### 7-1. `ProductRepository`

#### 変更箇所①: `buildKeywords()` の新規追加（private メソッド）

正規化済みキーワード文字列を空白（半角・全角）で分割し、`List<KeywordSearchParam>` を返す。

```
buildKeywords(String normalizedKeyword) : List<KeywordSearchParam>
```

| ステップ | 内容 |
|:----:|------|
| 1 | `normalizedKeyword` が null または空白 → `List.of()` を返す |
| 2 | 正規表現 `[\s\u3000]+`（半角スペース・全角スペース）で分割 |
| 3 | 空要素を除去し、各トークンについて `KeywordSearchParam` を生成 |
| 4 | `keywordLike = "%" + token + "%"`,  `codeLike = token + "%"` |
| 5 | `List<KeywordSearchParam>` を返す |

**例:** `"ナチュラル デスク"` → `[{keywordLike:"%ナチュラル%", codeLike:"ナチュラル%"}, {keywordLike:"%デスク%", codeLike:"デスク%"}]`

#### 変更箇所②: `buildSearchParams()` の修正

| 変更前 | 変更後 |
|-------|-------|
| `params.put("keywordLike", toKeywordLike(condition.keyword()))` | `params.put("keywords", buildKeywords(condition.keyword()))` |
| ― | `params.put("tasteDisplayNames", tasteDisplayNames)` |
| ― | `params.put("hasTasteFilter", !tasteDisplayNames.isEmpty())` |

tasteDisplayNames の取得:
```java
List<String> tasteDisplayNames = condition.tasteDisplayNames() == null
    ? List.of() : condition.tasteDisplayNames();
```

#### 変更箇所③: `toKeywordLike()` の廃止・`toCodeLike()` の非追加

`toKeywordLike()` / `toCodeLike()` は `buildKeywords()` に統合するため個別メソッドとしては不要になる（`toKeywordLike()` は削除または `@Deprecated` 化）。

---

### 7-2. `ProductFilterOptionRepository`

#### 新規メソッド: `findUnifiedTasteDisplayNames()`

**戻り値:** `List<String>`  
**処理:** `productMapper.selectUnifiedTasteDisplayNames()` を呼び出し、重複除去・ソート済みの display_name 一覧を返す。

---

## 8. Mapper 設計

### 8-1. `ProductMapper` インターフェース

追加メソッド:

```java
/**
 * カテゴリ横断で統合されたテイスト表示名一覧を取得する。
 * desk_tastes / chair_tastes / storage_tastes を UNION して重複排除・ソート済みで返す。
 */
List<String> selectUnifiedTasteDisplayNames();
```

---

### 8-2. `ProductMapper.xml`

#### 変更箇所①: `BaseProductWhere` のキーワード条件修正

**変更前:**
```xml
<if test="keywordLike != null and keywordLike != ''">
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

**変更後:**
```xml
<if test="keywords != null and keywords.size() > 0">
    AND
    <foreach collection="keywords" item="kw" separator=" AND ">
        (
            p.product_name ILIKE #{kw.keywordLike}
            OR (p.variation_name IS NOT NULL AND p.variation_name ILIKE #{kw.keywordLike})
            OR p.description ILIKE #{kw.keywordLike}
            OR EXISTS (
                SELECT 1 FROM product_variants pvk
                WHERE pvk.product_id = p.product_id
                AND pvk.product_code ILIKE #{kw.codeLike}
            )
        )
    </foreach>
</if>
```

**ポイント:**
- `keywords` が空リスト（`q` 未指定）の場合は条件追加なし → テイスト単独検索が可能
- `kw.keywordLike` は `%keyword%`（4 フィールドは部分一致）
- `kw.codeLike` は `keyword%`（product_code のみ前方一致）
- `variation_name IS NOT NULL` を明示して ILIKE の NULL 誤評価を排除
- 各キーワードは `AND` で結合（複数キーワードはすべて一致が必要）

---

#### 変更箇所②: `BaseProductWhere` へのテイストフィルタ追加

`BaseProductWhere` の末尾（`hasStorageFilter` ブロックの後）に以下を追加する。

```xml
<if test="hasTasteFilter">
    AND (
        EXISTS (
            SELECT 1
            FROM product_desk_attributes da
            JOIN desk_tastes dt ON da.taste_id = dt.taste_id
            WHERE da.product_id = p.product_id
              AND dt.display_name IN
            <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_chair_attributes ca
            JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
            WHERE ca.product_id = p.product_id
              AND ct.display_name IN
            <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
        OR EXISTS (
            SELECT 1
            FROM product_storage_attributes sa
            JOIN storage_tastes st ON sa.taste_id = st.taste_id
            WHERE sa.product_id = p.product_id
              AND st.display_name IN
            <foreach collection="tasteDisplayNames" item="name" open="(" close=")" separator=",">
                #{name}
            </foreach>
        )
    )
</if>
```

---

#### 変更箇所③: `selectUnifiedTasteDisplayNames` クエリの追加

```xml
<select id="selectUnifiedTasteDisplayNames" resultType="string">
    SELECT display_name
    FROM (
        SELECT display_name, sort_order FROM desk_tastes    WHERE is_active = TRUE
        UNION ALL
        SELECT display_name, sort_order FROM chair_tastes   WHERE is_active = TRUE
        UNION ALL
        SELECT display_name, sort_order FROM storage_tastes WHERE is_active = TRUE
    ) combined
    GROUP BY display_name
    ORDER BY MIN(sort_order) ASC, display_name ASC
</select>
```

**設計意図:**
- `UNION ALL` + `GROUP BY` で重複排除を行う（`UNION` より効率的）
- `MIN(sort_order)` を優先し、同値の場合は `display_name` 昇順
- `resultType="string"` で `List<String>` に直接マッピング

---

## 9. テンプレート設計

### 9-1. `product-list-search-results.html` の変更箇所

#### 変更①: サイドバーフォームへのテイストフィルタ追加

価格帯・カラーの次に以下のブロックを挿入する。

```html
<!-- テイストフィルタ（カラーフィルタの後に追加） -->
<div class="field" th:if="${tasteFilterOptions != null and !#lists.isEmpty(tasteFilterOptions)}">
    <label>テイスト</label>
    <div>
        <label class="check-item"
               th:each="tasteName : ${tasteFilterOptions}">
            <input type="checkbox"
                   name="taste"
                   th:value="${tasteName}"
                   th:checked="${selectedTasteDisplayNames != null
                                 and selectedTasteDisplayNames.contains(tasteName)}">
            <span th:text="${tasteName}">ナチュラル</span>
        </label>
    </div>
</div>
```

#### 変更②: ソート選択フォームへの `taste` 引き継ぎ（desktop ツールバー内）

既存の `<input type="hidden" name="color" ...>` の後に追加する。

```html
<input type="hidden" name="taste"
       th:each="t : ${selectedTasteDisplayNames}" th:value="${t}">
```

#### 変更③: ページネーションリンクへの `taste` パラメータ追加

各ページリンク（前へ / ページ番号 / 次へ）の `th:href` に `,taste=${selectedTasteDisplayNames}` を追加する。

例（前へリンク）:
```html
th:href="@{/products/search(q=${keyword},page=${currentPage - 1},size=${pageSize},
    sort=${sortValue},inStockOnly=${inStockOnly ? 'true' : null},
    priceBand=${selectedPriceBandIds},color=${selectedColorKeys},
    taste=${selectedTasteDisplayNames})}"
```

#### 変更④: リセットリンクへの対応

現状 `@{/products/search(q=${keyword})}` のみでテイスト条件もリセット済みのため、変更不要。

#### 変更⑤: モバイルモーダル

現状モバイル向けモーダルは簡易版と明記されているため、本対応スコープでは `TODO: テイストフィルタ（PC版と同様）` コメントを挿入するにとどめ、実装は後続対応とする（要件定義書スコープ外）。

---

## 10. インデックス設計

### 10-1. 追加インデックス一覧

| テーブル | カラム | インデックス種別 | 理由 |
|---------|--------|---------------|------|
| `products` | `product_name` | `pg_trgm` GIN | ILIKE 高速化 |
| `products` | `variation_name` | `pg_trgm` GIN | ILIKE 高速化（新規検索対象） |
| `products` | `description` | `pg_trgm` GIN | ILIKE 高速化（`TEXT` 型のため特に重要） |
| `product_variants` | `product_code` | B-tree（既存 UNIQUE 制約） | 前方一致は B-tree で対応可 |

### 10-2. 適用 DDL

以下を `sql/schema/` 配下の新規ファイル（例: `search-indexes.sql`）または既存の `products.sql` への追記として管理する。

```sql
-- pg_trgm 拡張の有効化（初回のみ、インフラ担当者と事前確認）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 商品名 trigram インデックス
CREATE INDEX IF NOT EXISTS idx_products_product_name_trgm
    ON products USING GIN (product_name gin_trgm_ops);

-- バリエーション名 trigram インデックス（NULL 値は自動スキップ）
CREATE INDEX IF NOT EXISTS idx_products_variation_name_trgm
    ON products USING GIN (variation_name gin_trgm_ops);

-- 説明文 trigram インデックス（TEXT 型のため最優先）
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING GIN (description gin_trgm_ops);
```

> **未決事項 No.1 依存:** `pg_trgm` 拡張の適用可否はインフラ担当者との合意が必要。  
> 適用しない場合は全件 ILIKE スキャンとなるが、最大 1,000 件のスコープでは許容範囲内（要件定義書 §5 参照）。

---

## 11. 既存設計書への影響

### 11-1. CRUD マトリックス差分

`docs/design/crud-matrix.md` の機能 **05「商品一覧（キーワード検索）」** 行を以下のとおり更新する。

**商品マスタドメイン表（§3）の変更:**

| No. | 機能 / 画面 | desk_tastes | chair_tastes | storage_tastes | 変更 |
|----:|------------|:-----------:|:------------:|:--------------:|:----:|
| 05 | 商品一覧（検索） | **R** | **R** | **R** | **新規 R 追加** |

変更前はすべて `−`。テイストフィルタ選択肢の取得（`selectUnifiedTasteDisplayNames`）、およびテイストフィルタ適用時の EXISTS サブクエリで各テーブルを参照するため `R` となる。

### 11-2. その他の設計書

| 設計書 | 変更要否 | 理由 |
|-------|---------|------|
| `er-diagram.md` | **変更なし** | テーブル追加・変更なし |
| `screen-list.md` | **変更なし** | 画面 URL・テンプレート変更なし |
| `screen-flow-diagram.md` | **変更なし** | 画面遷移の追加・変更なし |

---

## 12. 実装上の注意事項

### 12-1. セキュリティ

- `tasteDisplayNames` は URL クエリパラメータ経由のユーザー入力。MyBatis の `#{...}` 記法（プリペアドステートメント）を使用するため SQL インジェクションリスクはないが、`ProductFilterOptionService.normalizeTasteDisplayNames()` によるホワイトリストフィルタリングを必ず実施すること。
- Mapper XML の `IN` 句も `#{name}` によるバインドパラメータを使用しているため安全。

### 12-2. `variation_name` の NULL 考慮

- `variation_name IS NOT NULL AND variation_name ILIKE ...` の明示が必要（PostgreSQL では `NULL ILIKE '...'` は NULL を返すため実害はないが、インデックス利用上も明示することが望ましい）。

### 12-3. 空キーワード + テイストのみ検索

- `keywords` が空リストの場合、キーワード条件ブロックは生成されず、テイストフィルタ・在庫フィルタ等のみが適用される。  
- `CatalogController.searchResults()` は `q` が未指定でも `taste` のみで検索を実行できる。

### 12-4. `ProductSearchCondition` の後方互換性

`ProductSearchCondition` は Java record であり、フィールド追加はコンパイルエラーを引き起こす。全インスタンス生成箇所を必ず修正すること（§4-1 参照）。

---

## 13. 未決事項

（要件定義書 §8 を引き継ぐ）

| No. | 項目 | 確認先 | 本設計への影響 |
|----:|------|-------|-------------|
| 1 | `pg_trgm` 拡張の適用可否 | インフラ担当 / DBA | §10 インデックス設計の適用可否 |
| 2 | `display_name` のカテゴリ間統一性 | DB シードデータ確認 | テイストフィルタ選択肢の重複除去結果に影響 |
| 3 | カタカナ正規化方向（全角統一）とデータ整合性 | データ設計担当 | `NormalizationUtils` の変換方向の前提 |
| 4 | `variation_name` 追加によるヒット増加の懸念 | PO / 商品担当 | 要件変更の可能性あり |
