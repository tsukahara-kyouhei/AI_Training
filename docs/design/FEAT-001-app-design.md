# FEAT-001 アプリケーション詳細設計書

| 項目 | 内容 |
|------|------|
| Issue ID | FEAT-001 |
| ドキュメント種別 | アプリケーション詳細設計書 |
| 作成日 | 2026-04-24 |

---

## 1. 変更概要

| 区分 | クラス | パッケージ | 変更種別 |
|------|--------|-----------|---------|
| 新規 | `SearchKeywordNormalizer` | `service/product` | 新規作成 |
| 変更 | `ProductSearchCondition` | `model/product` | フィールド追加 |
| 変更 | `ProductCategoryFilter` | `model/product` | フィールド統合 |
| 変更 | `ProductFilterOptionsBundle` | `model/product` | フィールド統合 |
| 変更 | `ProductFilterOptionRepository` | `repository` | メソッド変更 |
| 変更 | `ProductFilterOptionService` | `service/product` | メソッド変更 |
| 変更 | `ProductRepository` | `repository` | `buildSearchParams` 変更 |
| 変更 | `ProductService` | `service/product` | `buildCondition` シグネチャ変更 |
| 変更 | `ProductListSearchService` | `service/product` | `buildCondition` 変更 |
| 変更 | `ProductMapper` | `mapper` | メソッド変更 |
| 変更 | `CatalogController` | `web` | エンドポイント変更 |

---

## 2. 新規クラス：`SearchKeywordNormalizer`

**パッケージ**: `jp.co.skig.officeorder.service.product`  
**役割**: 検索キーワードの正規化（全角英数字→半角変換、英字小文字化）をカプセル化する。  
**アノテーション**: `@Component`

### フィールド

```java
// 全角英大文字・小文字・数字（変換元）
private static final String FULLWIDTH_CHARS =
    "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ" +
    "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ" +
    "０１２３４５６７８９";

// 半角英大文字・小文字・数字（変換先）
private static final String HALFWIDTH_CHARS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
    "abcdefghijklmnopqrstuvwxyz" +
    "0123456789";
```

### メソッド

#### `normalize(String keyword): String`

キーワードを正規化して返す。

| ステップ | 処理 |
|---------|------|
| 1 | `null` または空白のみの場合は `null` を返す |
| 2 | `keyword.strip()` でトリム |
| 3 | 全角英大文字・全角英小文字・全角数字を対応する半角文字に1対1変換（`FULLWIDTH_CHARS` → `HALFWIDTH_CHARS` の文字対応で `String.translate` 相当の処理） |
| 4 | `toLowerCase(Locale.ROOT)` で英字を小文字に統一 |

**実装イメージ**:
```java
public String normalize(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return null;
    }
    String trimmed = keyword.strip();
    StringBuilder sb = new StringBuilder(trimmed.length());
    for (int i = 0; i < trimmed.length(); i++) {
        char c = trimmed.charAt(i);
        int idx = FULLWIDTH_CHARS.indexOf(c);
        sb.append(idx >= 0 ? HALFWIDTH_CHARS.charAt(idx) : c);
    }
    return sb.toString().toLowerCase(Locale.ROOT);
}
```

#### `toLikePattern(String normalizedKeyword): String`

正規化済みキーワードを部分一致用 LIKE パターンに変換する。

```java
public String toLikePattern(String normalizedKeyword) {
    if (normalizedKeyword == null) return null;
    return "%" + normalizedKeyword + "%";
}
```

#### `toPrefixPattern(String normalizedKeyword): String`

正規化済みキーワードを前方一致用 LIKE パターンに変換する。

```java
public String toPrefixPattern(String normalizedKeyword) {
    if (normalizedKeyword == null) return null;
    return normalizedKeyword + "%";
}
```

---

## 3. `ProductSearchCondition`（変更）

`tasteIds` フィールドを追加する。

### 変更後のレコード定義

```java
public record ProductSearchCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<PriceBand> priceBands,
    List<Long> colorIds,
    List<Long> tasteIds,            // 追加 ★
    ProductCategoryFilter categoryFilter,
    ProductSort sort,
    int page,
    int size,
    OffsetDateTime saleStartFrom
) {}
```

> `tasteIds` は検索結果画面（カテゴリ横断）でのテイスト絞込に使用する。
> カテゴリ一覧ページでは `ProductCategoryFilter.tasteIds` を使用し、`ProductSearchCondition.tasteIds` は `List.of()` を渡す。

### コンストラクタ呼び出し箇所の修正

以下の箇所で `new ProductSearchCondition(...)` を直接呼び出しているため、`tasteIds` 引数を追加する。

| クラス | メソッド | 渡す値 |
|--------|---------|--------|
| `ProductService` | `buildCondition(...)` 最終形 | 引数で受け取った `tasteIds` |
| `ProductListSearchService` | `searchWithPageCorrection()` ページ補正時 | `condition.tasteIds()` |
| `ProductRepository` | `findNewestProducts()` | `List.of()` |

---

## 4. `ProductCategoryFilter`（変更）

`deskTasteIds` / `chairTasteIds` / `storageTasteIds` を削除し、`tasteIds: List<Long>` に統合する。

### 変更後のレコード定義

```java
public record ProductCategoryFilter(
    List<Integer> deskTopShapeIds,
    List<Integer> deskWidthBandIds,
    List<Integer> deskDepthBandIds,
    List<Integer> deskHeightBandIds,
    List<Integer> chairFunctionIds,
    List<Integer> chairMaterialIds,
    List<Integer> storageUsageIds,
    List<Long>    tasteIds           // 旧 deskTasteIds / chairTasteIds / storageTasteIds を統合 ★
) {}
```

### `empty()` 変更後

```java
public static ProductCategoryFilter empty() {
    return new ProductCategoryFilter(
        List.of(), List.of(), List.of(), List.of(),
        List.of(), List.of(), List.of(),
        List.of()  // tasteIds
    );
}
```

### `isEmpty()` 変更後

```java
public boolean isEmpty() {
    return deskTopShapeIds.isEmpty()
        && deskWidthBandIds.isEmpty()
        && deskDepthBandIds.isEmpty()
        && deskHeightBandIds.isEmpty()
        && chairFunctionIds.isEmpty()
        && chairMaterialIds.isEmpty()
        && storageUsageIds.isEmpty()
        && tasteIds.isEmpty();
}
```

### `normalize()` 変更後

```java
public ProductCategoryFilter normalize() {
    return new ProductCategoryFilter(
        normalizeIntList(deskTopShapeIds),
        normalizeIntList(deskWidthBandIds),
        normalizeIntList(deskDepthBandIds),
        normalizeIntList(deskHeightBandIds),
        normalizeIntList(chairFunctionIds),
        normalizeIntList(chairMaterialIds),
        normalizeIntList(storageUsageIds),
        normalizeLongList(tasteIds)   // ★
    );
}

// 既存の normalizeList(List<Integer>) はそのまま流用
// Long 版を追加
private static List<Long> normalizeLongList(List<Long> values) {
    if (values == null || values.isEmpty()) return List.of();
    return values.stream()
        .filter(v -> v != null)
        .distinct()
        .toList();
}
```

---

## 5. `ProductFilterOptionsBundle`（変更）

`deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` を削除し、
`tasteOptions: List<CategoryFilterOption>` に統合する。

### 変更後のレコード定義

```java
public record ProductFilterOptionsBundle(
    List<ColorFilterOption>    colorOptions,
    List<CategoryFilterOption> deskTopShapeOptions,
    List<CategoryFilterOption> deskFunctionOptions,   // chairFunctionOptions は chair 専用なので残す
    List<CategoryFilterOption> chairFunctionOptions,
    List<CategoryFilterOption> chairMaterialOptions,
    List<CategoryFilterOption> storageUsageOptions,
    List<CategoryFilterOption> tasteOptions            // 旧 deskTasteOptions / chairTasteOptions / storageTasteOptions を統合 ★
) {}
```

> コンパクト コンストラクタ内の `immutableOrEmpty` 呼び出しも同様に `tasteOptions` に統合する。

---

## 6. `ProductFilterOptionRepository`（変更）

### 削除メソッド

- `findActiveDeskTasteOptions()`
- `findActiveChairTasteOptions()`
- `findActiveStorageTasteOptions()`

### 追加メソッド

```java
/**
 * 有効なテイスト候補一覧を統合マスタから取得する。
 *
 * @return テイスト候補
 */
public List<CategoryFilterOption> findActiveTasteOptions() {
    return productMapper.selectActiveTasteOptions().stream()
        .map(this::toCategoryFilterOption)
        .filter(option -> option != null)
        .toList();
}
```

---

## 7. `ProductFilterOptionService`（変更）

### `loadOptionsBundle()` の変更

```java
public ProductFilterOptionsBundle loadOptionsBundle() {
    return new ProductFilterOptionsBundle(
        productFilterOptionRepository.findActiveColorOptions(),
        productFilterOptionRepository.findActiveDeskTopShapeOptions(),
        productFilterOptionRepository.findActiveChairFunctionOptions(),
        productFilterOptionRepository.findActiveChairMaterialOptions(),
        productFilterOptionRepository.findActiveStorageUsageOptions(),
        productFilterOptionRepository.findActiveTasteOptions()  // ★ 統合メソッドへ変更
    );
}
```

### `buildDeskFilter()` の変更

引数から `rawDeskTasteIds` を `rawTasteIds` に変更し、`tasteIds: List<Long>` として返す。

```java
public ProductCategoryFilter buildDeskFilter(
    List<Integer> rawDeskTopShapeIds,
    List<Integer> rawDeskWidthBandIds,
    List<Integer> rawDeskDepthBandIds,
    List<Integer> rawDeskHeightBandIds,
    List<Integer> rawTasteIds,            // 引数名変更 ★
    ProductFilterOptionsBundle optionsBundle
) {
    List<Integer> allowedDeskTopShapeIds = extractOptionIds(optionsBundle.deskTopShapeOptions());
    List<Integer> allowedTasteIds        = extractOptionIds(optionsBundle.tasteOptions());  // ★
    return new ProductCategoryFilter(
        normalizeIntegerOptions(rawDeskTopShapeIds, allowedDeskTopShapeIds),
        normalizeIntegerOptions(rawDeskWidthBandIds, ALLOWED_DESK_WIDTH_BAND_IDS),
        normalizeIntegerOptions(rawDeskDepthBandIds, ALLOWED_DESK_DEPTH_BAND_IDS),
        normalizeIntegerOptions(rawDeskHeightBandIds, ALLOWED_DESK_HEIGHT_BAND_IDS),
        List.of(),   // chairFunctionIds
        List.of(),   // chairMaterialIds
        List.of(),   // storageUsageIds
        toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))  // tasteIds ★
    );
}
```

### `buildChairFilter()` の変更

```java
public ProductCategoryFilter buildChairFilter(
    List<Integer> rawChairFunctionIds,
    List<Integer> rawChairMaterialIds,
    List<Integer> rawTasteIds,            // 引数名変更 ★
    ProductFilterOptionsBundle optionsBundle
) {
    List<Integer> allowedChairFunctionIds  = extractOptionIds(optionsBundle.chairFunctionOptions());
    List<Integer> allowedChairMaterialIds  = extractOptionIds(optionsBundle.chairMaterialOptions());
    List<Integer> allowedTasteIds          = extractOptionIds(optionsBundle.tasteOptions());  // ★
    return new ProductCategoryFilter(
        List.of(), List.of(), List.of(), List.of(),  // desk系
        normalizeIntegerOptions(rawChairFunctionIds, allowedChairFunctionIds),
        normalizeIntegerOptions(rawChairMaterialIds, allowedChairMaterialIds),
        List.of(),   // storageUsageIds
        toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))  // tasteIds ★
    );
}
```

### `buildStorageFilter()` の変更

```java
public ProductCategoryFilter buildStorageFilter(
    List<Integer> rawStorageUsageIds,
    List<Integer> rawTasteIds,            // 引数名変更 ★
    ProductFilterOptionsBundle optionsBundle
) {
    List<Integer> allowedStorageUsageIds = extractOptionIds(optionsBundle.storageUsageOptions());
    List<Integer> allowedTasteIds        = extractOptionIds(optionsBundle.tasteOptions());  // ★
    return new ProductCategoryFilter(
        List.of(), List.of(), List.of(), List.of(),  // desk系
        List.of(), List.of(),                         // chair系
        normalizeIntegerOptions(rawStorageUsageIds, allowedStorageUsageIds),
        toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))  // tasteIds ★
    );
}
```

### 追加：`normalizeTasteIds()`（検索結果画面用）

```java
/**
 * 検索結果画面（カテゴリ横断）のテイスト絞込ID一覧を正規化する。
 *
 * @param rawTasteIds 画面から渡された生値
 * @param optionsBundle 使用する候補群
 * @return 正規化済みテイストID一覧（Long）
 */
public List<Long> normalizeTasteIds(List<Integer> rawTasteIds,
                                    ProductFilterOptionsBundle optionsBundle) {
    if (rawTasteIds == null || rawTasteIds.isEmpty()) return List.of();
    List<Integer> allowedIds = extractOptionIds(optionsBundle.tasteOptions());
    return toLongList(normalizeIntegerOptions(rawTasteIds, allowedIds));
}
```

### 追加：`resolveSelectedTasteIds()`（画面再表示用）

```java
/**
 * 選択済み tasteIds を画面表示順のまま返す。
 *
 * @param tasteIds 選択済みID
 * @param optionsBundle 使用する候補群
 * @return 表示順に整えた選択済みID
 */
public List<Long> resolveSelectedTasteIds(List<Long> tasteIds,
                                          ProductFilterOptionsBundle optionsBundle) {
    if (tasteIds == null || tasteIds.isEmpty()) return List.of();
    Set<Long> selected = new HashSet<>(tasteIds);
    return optionsBundle.tasteOptions().stream()
        .map(opt -> (long) opt.id())
        .filter(selected::contains)
        .toList();
}
```

### 削除メソッド

- `deskTasteOptions()`
- `chairTasteOptions()`
- `storageTasteOptions()`

### 追加メソッド

```java
/** テイスト候補（統合）を返す。 */
public List<CategoryFilterOption> tasteOptions() {
    return loadOptionsBundle().tasteOptions();
}
```

### private ヘルパー追加：`toLongList()`

```java
private List<Long> toLongList(List<Integer> values) {
    if (values == null || values.isEmpty()) return List.of();
    return values.stream().map(Long::valueOf).toList();
}
```

---

## 8. `ProductRepository`（変更）

### `buildSearchParams()` の変更

変更前と変更後の差分のみ記載する。

**削除するコード**:
```java
List<Integer> deskTasteIds    = filter.deskTasteIds();
List<Integer> chairTasteIds   = filter.chairTasteIds();
List<Integer> storageTasteIds = filter.storageTasteIds();
// ...
boolean hasDeskFilter    = ... || !deskTasteIds.isEmpty() || ...
boolean hasChairFilter   = ... || !chairTasteIds.isEmpty();
boolean hasStorageFilter = ... || !storageTasteIds.isEmpty();
// ...
params.put("deskTasteIds",    deskTasteIds);
params.put("chairTasteIds",   chairTasteIds);
params.put("storageTasteIds", storageTasteIds);
```

**追加するコード**:
```java
List<Long> tasteIds       = filter.tasteIds();       // ★ 統合フィールド
List<Long> searchTasteIds = condition.tasteIds() == null
                            ? List.of() : condition.tasteIds(); // ★ 検索結果画面用

boolean hasDeskFilter = category == ProductCategory.DESK
    && (!deskTopShapeIds.isEmpty()
        || !tasteIds.isEmpty()           // ★ (旧 deskTasteIds → tasteIds)
        || !deskWidthRanges.isEmpty()
        || !deskDepthRanges.isEmpty()
        || !deskHeightRanges.isEmpty());

boolean hasChairFilter = category == ProductCategory.CHAIR
    && (!chairFunctionIds.isEmpty()
        || !chairMaterialIds.isEmpty()
        || !tasteIds.isEmpty());         // ★ (旧 chairTasteIds → tasteIds)

boolean hasStorageFilter = category == ProductCategory.STORAGE
    && (!storageUsageIds.isEmpty()
        || !tasteIds.isEmpty());         // ★ (旧 storageTasteIds → tasteIds)

boolean hasSearchTasteFilter = !searchTasteIds.isEmpty(); // ★ 新規

// params.put
params.put("tasteIds",             tasteIds);             // ★
params.put("searchTasteIds",       searchTasteIds);       // ★
params.put("hasSearchTasteFilter", hasSearchTasteFilter); // ★
```

**変更するキーワード関連コード**:

現行の `toKeywordLike(condition.keyword())` は 1 パラメータで `%keyword%` を生成しているが、
本改修で `SearchKeywordNormalizer` での正規化と3種パラメータへの分解が必要になる。

`buildSearchParams` に `SearchKeywordNormalizer` を DI して以下を生成する：

```java
// SearchKeywordNormalizer normalizer を DI or newで保持
String normalizedKeyword = normalizer.normalize(condition.keyword());
String keywordLike       = normalizer.toLikePattern(normalizedKeyword);   // %abc%
String keywordExact      = normalizedKeyword;                              // abc
String keywordPrefix     = normalizer.toPrefixPattern(normalizedKeyword); // abc%

params.put("keywordLike",   keywordLike);   // ★ 旧 keywordLike から正規化版に変更
params.put("keywordExact",  keywordExact);  // ★ 新規
params.put("keywordPrefix", keywordPrefix); // ★ 新規
```

> `ProductRepository` のコンストラクタに `SearchKeywordNormalizer` を追加して DI する。

---

## 9. `ProductService`（変更）

### `buildCondition()` シグネチャ変更（最終形）

`tasteIds: List<Long>` を引数に追加する。既存の 2-引数、3-引数の overload には `tasteIds = List.of()` を渡して委譲する。

```java
// 最終形（全引数）
public ProductSearchCondition buildCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<Integer> priceBandIds,
    List<Long> colorIds,
    String sort,
    int page,
    int size,
    ProductSort defaultSort,
    ProductCategoryFilter categoryFilter,
    List<Long> tasteIds,           // ★ 追加
    OffsetDateTime saleStartFrom
) {
    // ... 正規化後に new ProductSearchCondition(..., tasteIds, ...) を生成
}
```

既存の短縮 overload は `tasteIds = List.of()` で最終形に委譲し、シグネチャ互換を保つ。

---

## 10. `ProductListSearchService`（変更）

### `buildCondition()` の変更

検索結果画面用の `tasteIds: List<Long>` を引数に追加し、`productService.buildCondition()` に渡す。

```java
public ProductSearchCondition buildCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<Integer> rawPriceBandIds,
    List<String> rawColorKeys,
    List<Long> tasteIds,                // ★ 追加
    String sort,
    int page,
    int size,
    ProductSort defaultSort,
    ProductFilterOptionsBundle optionsBundle
) {
    // ...（既存処理の後）
    return productService.buildCondition(
        categoryId, keyword, inStockOnly,
        selectedPriceBandIds, colorIds, sort, page, size, defaultSort,
        ProductCategoryFilter.empty(),
        tasteIds,      // ★
        null
    );
}
```

既存の카테고리フィルタあり版 overload も同様に `tasteIds` を追加する。

### `searchWithPageCorrection()` の変更

ページ補正時の `new ProductSearchCondition(...)` に `condition.tasteIds()` を追加する。

```java
ProductSearchCondition corrected = new ProductSearchCondition(
    condition.categoryId(),
    condition.keyword(),
    condition.inStockOnly(),
    condition.priceBands(),
    condition.colorIds(),
    condition.tasteIds(),     // ★ 追加
    condition.categoryFilter(),
    condition.sort(),
    totalPages,
    condition.size(),
    condition.saleStartFrom()
);
```

---

## 11. `ProductMapper`（変更）

### 削除メソッド

```java
List<ProductFilterOptionMapperRow> selectActiveDeskTasteOptions();
List<ProductFilterOptionMapperRow> selectActiveChairTasteOptions();
List<ProductFilterOptionMapperRow> selectActiveStorageTasteOptions();
```

### 追加メソッド

```java
/**
 * テイスト統合マスタから有効なテイスト選択肢を取得する。
 */
List<ProductFilterOptionMapperRow> selectActiveTasteOptions();
```

---

## 12. `CatalogController`（変更）

### `searchResults()` の変更

```java
@GetMapping("/products/search")
public String searchResults(
    @RequestParam(name = "q",           required = false) String keyword,
    @RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
    @RequestParam(name = "priceBand",   required = false) List<Integer> rawPriceBandIds,
    @RequestParam(name = "color",       required = false) List<String>  rawColorKeys,
    @RequestParam(name = "taste",       required = false) List<Integer> rawTasteIds,  // ★ 追加
    @RequestParam(name = "sort",        required = false) String sort,
    @RequestParam(name = "page",        defaultValue = "1")  int page,
    @RequestParam(name = "size",        defaultValue = "15") int size,
    Model model
) {
    ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
    List<Long> tasteIds = productFilterOptionService.normalizeTasteIds(rawTasteIds, optionsBundle); // ★

    ProductSearchCondition condition = productListSearchService.buildCondition(
        null, keyword, inStockOnly, rawPriceBandIds, rawColorKeys,
        tasteIds,      // ★
        sort, page, size, ProductSort.RECOMMENDED, optionsBundle
    );
    ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
    applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);

    model.addAttribute("keyword", result.condition().keyword() == null ? "" : result.condition().keyword());

    // ★ 追加
    List<Long> selectedTasteIds = productFilterOptionService
        .resolveSelectedTasteIds(result.condition().tasteIds(), optionsBundle);
    model.addAttribute("selectedTasteIds", selectedTasteIds);
    model.addAttribute("tasteOptions", optionsBundle.tasteOptions());

    return "pages/product-list-search-results";
}
```

### `desks()` の変更

```java
// 変更前: @RequestParam(name = "deskTaste", ...) List<Integer> rawDeskTasteIds
// 変更後:
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds  // ★ パラメータ名変更

// buildDeskFilter の呼び出し変更
ProductCategoryFilter deskFilter = productFilterOptionService.buildDeskFilter(
    rawDeskTopShapeIds, rawDeskWidthBandIds, rawDeskDepthBandIds,
    rawDeskHeightBandIds, rawTasteIds,   // ★
    optionsBundle
);

// model.addAttribute 変更
// 変更前: model.addAttribute("deskTasteIds",    deskFilter.deskTasteIds());
// 変更前: model.addAttribute("deskTasteOptions", optionsBundle.deskTasteOptions());
// 変更後:
model.addAttribute("tasteIds",    deskFilter.tasteIds());       // ★
model.addAttribute("tasteOptions", optionsBundle.tasteOptions()); // ★
```

### `chairs()` の変更

```java
// 変更前: @RequestParam(name = "chairTaste", ...) List<Integer> rawChairTasteIds
// 変更後:
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds  // ★

// buildChairFilter 変更
ProductCategoryFilter chairFilter = productFilterOptionService.buildChairFilter(
    rawChairFunctionIds, rawChairMaterialIds, rawTasteIds, optionsBundle  // ★
);

// model.addAttribute 変更
// 変更前: model.addAttribute("chairTasteIds",    chairFilter.chairTasteIds());
// 変更前: model.addAttribute("chairTasteOptions", optionsBundle.chairTasteOptions());
// 変更後:
model.addAttribute("tasteIds",    chairFilter.tasteIds());       // ★
model.addAttribute("tasteOptions", optionsBundle.tasteOptions()); // ★
```

### `storages()` の変更

```java
// 変更前: @RequestParam(name = "storageTaste", ...) List<Integer> rawStorageTasteIds
// 変更後:
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds  // ★

// buildStorageFilter 変更
ProductCategoryFilter storageFilter = productFilterOptionService.buildStorageFilter(
    rawStorageUsageIds, rawTasteIds, optionsBundle  // ★
);

// model.addAttribute 変更
// 変更前: model.addAttribute("storageTasteIds",    storageFilter.storageTasteIds());
// 変更前: model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions());
// 変更後:
model.addAttribute("tasteIds",    storageFilter.tasteIds());       // ★
model.addAttribute("tasteOptions", optionsBundle.tasteOptions());   // ★
```

> **URL パラメータ変更の影響**  
> カテゴリ一覧ページのテイスト絞込 URL パラメータが変わる（例: `?deskTaste=1` → `?taste=1`）。
> 既存ブックマークとの互換性は本 Issue のスコープ外。

---

## 13. テンプレート（Thymeleaf）変更

### `fragments/layout/header.html`

```html
<!-- 変更前 -->
<input type="search" name="q" placeholder="商品名・商品コード" aria-label="商品名・商品コードで検索">

<!-- 変更後 -->
<input type="search" name="q" placeholder="商品名・商品コードなど" aria-label="商品名・商品コードなどで検索">
```

### `product-list-search-results.html`

#### 絞込フォームへのテイスト追加（カラーフィルタの直後）

```html
<hr>
<div class="field">
    <label>テイスト</label>
    <div class="checklist">
        <label class="check-item" th:each="option : ${tasteOptions}">
            <input type="checkbox"
                   name="taste"
                   th:value="${option.id}"
                   th:checked="${selectedTasteIds != null and selectedTasteIds.contains(option.id)}">
            <span th:text="${option.label}">ベーシック</span>
        </label>
    </div>
</div>
```

#### hidden フィールド（並び順・表示件数フォームへの追加）

ソート・表示件数変更フォームに `taste` の hidden を追加する：

```html
<input type="hidden" name="taste" th:each="id : ${selectedTasteIds}" th:value="${id}">
```

#### ページネーションリンクへの追加

全ページリンクの `th:href` に `taste=${selectedTasteIds}` を追加する（既存の `color`・`priceBand` と同形式）：

```html
th:href="@{/products/search(q=${keyword},page=${...},size=${pageSize},sort=${sortValue},
           inStockOnly=${inStockOnly ? 'true' : null},
           priceBand=${selectedPriceBandIds},
           color=${selectedColorKeys},
           taste=${selectedTasteIds})}"
```

### `product-list-category-desk.html` / `chair` / `storage`

各カテゴリ一覧テンプレートで以下を変更する（デスクを例に記載）。

```html
<!-- 変更前 -->
<input type="checkbox" name="deskTaste" th:value="${option.id}"
       th:checked="${deskTasteIds != null and deskTasteIds.contains(option.id)}">
... th:each="option : ${deskTasteOptions}" ...

<!-- 変更後 -->
<input type="checkbox" name="taste" th:value="${option.id}"
       th:checked="${tasteIds != null and tasteIds.contains(option.id)}">
... th:each="option : ${tasteOptions}" ...
```

ページネーションリンクも同様に `deskTaste` → `taste` に変更する。
