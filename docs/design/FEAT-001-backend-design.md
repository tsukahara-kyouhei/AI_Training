# FEAT-001 — バックエンド詳細設計

> 作成日: 2026-03-16  
> 関連設計概要: [FEAT-001-design-overview.md](FEAT-001-design-overview.md)

---

## 1. ProductRepository.java

**ファイル:** `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`

### 1-1. 変更メソッド一覧

| メソッド | 変更種別 | 対応 FR |
|---------|---------|---------|
| `buildSearchParams()` | 変更 | FR-01, FR-02, FR-03, FR-04 |
| `toKeywordLike()` | 変更（ロジック追加なし・引数の意味を明確化） | — |
| `toKeywordPrefix()` | **新規追加** | FR-01 |
| `normalizeKeyword()` | **新規追加** | FR-03 |

---

### 1-2. 新規メソッド: `normalizeKeyword()`

```java
import java.text.Normalizer;

/**
 * 入力キーワードを NFKC 正規化する。
 * 全角英数字・記号を半角に、半角カタカナを全角に変換する。
 * null / 空白のみの場合は null を返す。
 */
private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return null;
    }
    return Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKC);
}
```

---

### 1-3. 新規メソッド: `toKeywordPrefix()`

商品コード（`product_code`）の前方一致検索用。

```java
/**
 * 正規化済みキーワードを product_code の前方一致 LIKE パターンに変換する。
 * "keyword" → "keyword%"
 * normalizedKeyword が null の場合は null を返す。
 */
private String toKeywordPrefix(String normalizedKeyword) {
    if (normalizedKeyword == null) {
        return null;
    }
    return normalizedKeyword + "%";
}
```

---

### 1-4. 既存メソッド: `toKeywordLike()` の変更

現行の引数 `keyword`（生の入力値）を受け取っていたが、正規化済みの値を受け取るように変更する。
メソッドのロジック自体（`%〜%` への変換）は変更しない。

```java
// 変更前
private String toKeywordLike(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return null;
    }
    return "%" + keyword.trim() + "%";
}

// 変更後（引数の意味変更 — 正規化済みキーワードを受け取る）
private String toKeywordLike(String normalizedKeyword) {
    if (normalizedKeyword == null) {
        return null;
    }
    return "%" + normalizedKeyword + "%";  // trim 不要（normalizeKeyword 側で実施済み）
}
```

---

### 1-5. `buildSearchParams()` の変更

変更前後の差分のみ示す。

```java
// ▼ 変更前
params.put("keywordLike", toKeywordLike(condition.keyword()));

// ▼ 変更後
String normalized = normalizeKeyword(condition.keyword());    // ← 追加
params.put("keywordLike",   toKeywordLike(normalized));       // 既存キーを継続利用
params.put("keywordPrefix", toKeywordPrefix(normalized));     // ← 追加
```

`hasSearchTasteFilter` フラグの追加（テイスト絞込用）:

```java
// ▼ 追加箇所: hasDeskFilter などを計算している付近に追記
ProductCategoryFilter cf = condition.categoryFilter();

// 検索結果画面（categoryId=null）でテイストが選択されている場合に true
boolean hasSearchTasteFilter = condition.categoryId() == null
        && cf != null
        && (!isNullOrEmpty(cf.deskTasteIds())
            || !isNullOrEmpty(cf.chairTasteIds())
            || !isNullOrEmpty(cf.storageTasteIds()));
params.put("hasSearchTasteFilter", hasSearchTasteFilter);
```

> **注意:** `isNullOrEmpty()` は `list == null || list.isEmpty()` を判定するプライベートヘルパーメソッド。既存コードで同等のロジックが使われている場合はそれを流用すること。

---

### 1-6. 変更前後の params キー一覧（差分）

| キー | 変更 |
|-----|------|
| `keywordLike` | 値が正規化済みキーワードを使うように変更 |
| `keywordPrefix` | **新規追加** (`normalizedKeyword + "%"`) |
| `hasSearchTasteFilter` | **新規追加** (`boolean`) |

その他のキーは変更なし。

---

## 2. ProductMapper.xml

**ファイル:** `src/main/resources/mappers/ProductMapper.xml`

### 2-1. 変更箇所一覧

| 変更箇所 | 内容 |
|---------|------|
| `BaseProductWhere` フラグメント | キーワード条件の変更（FR-01, FR-02） |
| `SearchTasteFilter` フラグメント | **新規追加**（FR-04） |
| `BaseProductWhere` への include 追加 | `SearchTasteFilter` の呼び出し追加（FR-04） |

---

### 2-2. `BaseProductWhere` — キーワード条件の変更

```xml
<!-- ▼ 変更前 -->
<if test="keywordLike != null">
  AND (
    p.product_name ILIKE #{keywordLike}
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE #{keywordLike}
    )
  )
</if>

<!-- ▼ 変更後 -->
<if test="keywordLike != null">
  AND (
    p.product_name    ILIKE #{keywordLike}
    OR p.variation_name ILIKE #{keywordLike}
    OR p.description    ILIKE #{keywordLike}
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE #{keywordPrefix}
    )
  )
</if>
```

**ポイント:**
- `variation_name`・`description` を `OR` で追加（部分一致 `keywordLike`）
- `product_code` は `keywordLike` → `keywordPrefix` に変更（前方一致）
- `keywordLike != null` のガード条件はそのまま流用（`keywordPrefix` も同時に null でなくなる）

---

### 2-3. 新規 SQL フラグメント: `SearchTasteFilter`

`BaseProductWhere` の末尾（既存の `StorageAttributeFilter` の include の後）に追加する。

```xml
<sql id="SearchTasteFilter">
  <if test="hasSearchTasteFilter">
    AND (
      <trim prefixOverrides="OR">
        <if test="deskTasteIds != null and deskTasteIds.size() > 0">
          EXISTS (
            SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id
              AND da.taste_id IN
              <foreach item="id" collection="deskTasteIds" open="(" separator="," close=")">
                #{id}
              </foreach>
          )
        </if>
        <if test="chairTasteIds != null and chairTasteIds.size() > 0">
          OR EXISTS (
            SELECT 1 FROM product_chair_attributes ca
            WHERE ca.product_id = p.product_id
              AND ca.taste_id IN
              <foreach item="id" collection="chairTasteIds" open="(" separator="," close=")">
                #{id}
              </foreach>
          )
        </if>
        <if test="storageTasteIds != null and storageTasteIds.size() > 0">
          OR EXISTS (
            SELECT 1 FROM product_storage_attributes sa
            WHERE sa.product_id = p.product_id
              AND sa.taste_id IN
              <foreach item="id" collection="storageTasteIds" open="(" separator="," close=")">
                #{id}
              </foreach>
          )
        </if>
      </trim>
    )
  </if>
</sql>
```

**設計意図:**
- `<trim prefixOverrides="OR">` により、最初の条件の先頭 `OR` を MyBatis が自動削除する
- `hasSearchTasteFilter = true` のとき、3 リストのうち少なくとも 1 つは非空であることが保証されるため、`AND ()` が空になることはない
- `hasSearchTasteFilter = false`（カテゴリ一覧ページや taste 未選択時）は条件全体がスキップされる

---

### 2-4. `BaseProductWhere` への `SearchTasteFilter` の追加

```xml
<sql id="BaseProductWhere">
  <!-- ...既存 SQL... -->
  <if test="hasVariantFilter"> AND EXISTS (...) </if>
  <if test="hasDeskFilter">    <include refid="DeskAttributeFilter"/>     </if>
  <if test="hasChairFilter">   <include refid="ChairAttributeFilter"/>    </if>
  <if test="hasStorageFilter"> <include refid="StorageAttributeFilter"/>  </if>
  <include refid="SearchTasteFilter"/>  <!-- ← 末尾に追加（自身の if でゲート） -->
</sql>
```

---

## 3. ProductFilterOptionService.java

**ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`

### 3-1. 追加メソッド一覧

| メソッド | 役割 |
|---------|------|
| `allTasteDisplayNames(ProductFilterOptionsBundle)` | 全カテゴリのテイスト表示名の重複除去リストを返す |
| `resolveTasteFilter(List<String>, ProductFilterOptionsBundle)` | 表示名リストから各カテゴリの taste_id リストを解決し `ProductCategoryFilter` で返す |

---

### 3-2. `allTasteDisplayNames()` — テイスト選択肢取得

```java
/**
 * 全カテゴリ（デスク・チェア・収納）のテイスト display_name を
 * 重複除去した順序保持リストで返す。
 * 各テーブルの sort_order 順を尊重しつつ、先行するテーブルの順序を優先する。
 */
public List<String> allTasteDisplayNames(ProductFilterOptionsBundle bundle) {
    // 挿入順を保持しつつ重複排除するため LinkedHashSet を使用
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    for (CategoryFilterOption opt : bundle.deskTasteOptions()) {
        seen.add(opt.label());
    }
    for (CategoryFilterOption opt : bundle.chairTasteOptions()) {
        seen.add(opt.label());
    }
    for (CategoryFilterOption opt : bundle.storageTasteOptions()) {
        seen.add(opt.label());
    }
    return new ArrayList<>(seen);
}
```

**ソート順の方針:**
- 3 テーブルの並びを desk → chair → storage の順で処理し、初出現時の順序を維持する
- 全テーブルで同名のテイスト（例: 「ナチュラル」）は desk_tastes の sort_order が基準になる
- テーブル間でのソート順の統一が必要な場合は、マスタデータ整備で対応する

---

### 3-3. `resolveTasteFilter()` — 表示名 → taste_id 変換

```java
/**
 * 選択されたテイスト表示名リストを、各カテゴリの taste_id リストに変換し
 * ProductCategoryFilter として返す。
 * taste フィールド以外（天板形状・サイズ等）はすべて空リストとする。
 *
 * @param displayNames URL パラメーター "taste" の値リスト
 * @param bundle       事前取得済みのフィルターオプション一式
 */
public ProductCategoryFilter resolveTasteFilter(
        List<String> displayNames,
        ProductFilterOptionsBundle bundle) {

    if (displayNames == null || displayNames.isEmpty()) {
        return ProductCategoryFilter.empty();
    }

    Set<String> nameSet = new HashSet<>(displayNames);

    List<Integer> deskTasteIds = bundle.deskTasteOptions().stream()
            .filter(opt -> nameSet.contains(opt.label()))
            .map(CategoryFilterOption::id)
            .toList();

    List<Integer> chairTasteIds = bundle.chairTasteOptions().stream()
            .filter(opt -> nameSet.contains(opt.label()))
            .map(CategoryFilterOption::id)
            .toList();

    List<Integer> storageTasteIds = bundle.storageTasteOptions().stream()
            .filter(opt -> nameSet.contains(opt.label()))
            .map(CategoryFilterOption::id)
            .toList();

    // taste フィールド以外は空リストで生成
    return new ProductCategoryFilter(
            List.of(),        // deskTopShapeIds
            List.of(),        // deskWidthBandIds
            List.of(),        // deskDepthBandIds
            List.of(),        // deskHeightBandIds
            deskTasteIds,     // deskTasteIds
            List.of(),        // chairFunctionIds
            List.of(),        // chairMaterialIds
            chairTasteIds,    // chairTasteIds
            List.of(),        // storageUsageIds
            storageTasteIds   // storageTasteIds
    );
}
```

**注意:** `ProductCategoryFilter` のコンストラクター引数の順序は既存コードに合わせること。フィールド順は以下の通り:
```
deskTopShapeIds, deskWidthBandIds, deskDepthBandIds, deskHeightBandIds, deskTasteIds,
chairFunctionIds, chairMaterialIds, chairTasteIds,
storageUsageIds, storageTasteIds
```

---

## 4. ProductListSearchService.java

**ファイル:** `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`

### 4-1. `buildCondition()` のシグネチャ変更

検索結果画面用に呼ばれる `buildCondition` オーバーロード（`categoryFilter` を受け取らない版）に `tasteNames` パラメーターを追加する。

```java
// ▼ 変更前
public ProductListSearchResult buildCondition(
        String categoryId,
        String keyword,
        boolean inStockOnly,
        List<Integer> rawPriceBandIds,
        List<String> rawColorKeys,
        ProductSort sort,
        int page,
        int size,
        ProductFilterOptionsBundle optionsBundle) { ... }

// ▼ 変更後
public ProductListSearchResult buildCondition(
        String categoryId,
        String keyword,
        boolean inStockOnly,
        List<Integer> rawPriceBandIds,
        List<String> rawColorKeys,
        List<String> rawTasteNames,      // ← 追加
        ProductSort sort,
        int page,
        int size,
        ProductFilterOptionsBundle optionsBundle) { ... }
```

### 4-2. メソッド本体の変更

```java
// ▼ 変更前
ProductCategoryFilter categoryFilter = ProductCategoryFilter.empty();

// ▼ 変更後
ProductCategoryFilter categoryFilter =
        productFilterOptionService.resolveTasteFilter(rawTasteNames, optionsBundle);
```

変更箇所はこの 1 箇所のみ。`ProductService.buildCondition()` への委譲呼び出しは変更不要（引数として渡す `categoryFilter` が変わるだけ）。

---

## 5. CatalogController.java

**ファイル:** `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`

### 5-1. `searchResults()` のシグネチャ変更

```java
// ▼ 変更後（追加分のみ記載）
@GetMapping("/products/search")
public String searchResults(
        @RequestParam(name = "q", required = false) String keyword,
        @RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
        @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
        @RequestParam(name = "color", required = false) List<String> rawColorKeys,
        @RequestParam(name = "taste", required = false) List<String> rawTasteNames,  // ← 追加
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "15") int size,
        Model model) { ... }
```

### 5-2. メソッド本体の変更

```java
// ① buildCondition 呼び出しに rawTasteNames を追加
var result = productListSearchService.buildCondition(
        null,
        keyword,
        inStockOnly,
        rawPriceBandIds,
        rawColorKeys,
        rawTasteNames,    // ← 追加
        ProductSort.fromValue(sort, ProductSort.RECOMMENDED),
        page,
        size,
        optionsBundle);

// ② テイスト選択肢と選択済み値をモデルに追加（既存の model.addAttribute の後に追記）
List<String> tasteOptions = productFilterOptionService.allTasteDisplayNames(optionsBundle);
List<String> selectedTasteNames = rawTasteNames != null ? rawTasteNames : List.of();
model.addAttribute("tasteOptions",       tasteOptions);
model.addAttribute("selectedTasteNames", selectedTasteNames);
```

### 5-3. 変更しないもの

- `applyProductListModel()` ヘルパーメソッド — 変更なし
- `model.addAttribute("keyword", ...)` — 変更なし（既存のまま）
- 他のエンドポイント（`desks()`, `chairs()`, `storages()` 等）— 変更なし

---

## 6. 設計上の考慮事項・注意点

### 6-1. `keywordLike` と `keywordPrefix` の null ガード

`normalizeKeyword()` が `null` を返す場合（キーワード未入力）は、`toKeywordLike()` も `toKeywordPrefix()` も `null` を返す。XML 側の `<if test="keywordLike != null">` により、キーワード条件ブロック全体がスキップされる。`keywordPrefix` が `null` の場合でも XML の `#{keywordPrefix}` はこのブロック内のみに登場するため、問題は生じない。

### 6-2. `hasSearchTasteFilter` と既存フラグの関係

| フラグ | `categoryId=null` かつ taste 選択時 | 説明 |
|-------|-------------------------------------|------|
| `hasDeskFilter` | `false` | カテゴリ横断のため不発動 |
| `hasChairFilter` | `false` | 同上 |
| `hasStorageFilter` | `false` | 同上 |
| `hasSearchTasteFilter` | `true` | 新規フラグが担当 |

既存の `DeskAttributeFilter` 等は `AND EXISTS` であるため、`hasSearchTasteFilter` の `SearchTasteFilter` とは別途独立して動作する。検索結果画面では前者は常に `false` なので競合しない。

### 6-3. `resolveTasteFilter()` のセキュリティ考慮

`rawTasteNames` は URL パラメーターから来るユーザー入力であるが、`resolveTasteFilter()` 内で `bundle.xxx.stream().filter(display_name が一致するもの)` とホワイトリスト照合しているため、存在しないテイスト名が来ても対応するIDが見つからずに空リストになる。SQL インジェクションのリスクはない（MyBatis の PreparedStatement バインドを使用）。

### 6-4. NFKC 正規化の副作用

`java.text.Normalizer.normalize(text, NFKC)` はカーニングなどの互換文字（Ⅲ→III 等）も展開する。商品名や説明文に数字ローマ数字等が含まれる場合、意図した変換になることを確認すること。基本的な英数字・カタカナの変換においては副作用は生じない。
