# FEAT-001 商品検索機能強化 クラス詳細設計書

対象ファイル（レイヤ別）：

| レイヤ | ファイル |
|--------|---------|
| Controller | `src/main/java/.../web/CatalogController.java` |
| Service | `src/main/java/.../service/product/ProductListSearchService.java` |
| Service | `src/main/java/.../service/product/ProductFilterOptionService.java` |
| Model | `src/main/java/.../model/product/ProductSearchCondition.java` |
| Repository | `src/main/java/.../repository/ProductRepository.java` |
| Repository | `src/main/java/.../repository/ProductFilterOptionRepository.java` |

---

## 1. `ProductSearchCondition`（Model）

### 変更内容

Java `record` に `tasteNames` フィールドを追加する。

| フィールド名 | 型 | 区分 | 内容 |
|------------|-----|------|------|
| `categoryId` | `String` | 現状維持 | カテゴリ ID |
| `keyword` | `String` | 現状維持（格納値が正規化済みに変わる） | 検索キーワード |
| `inStockOnly` | `boolean` | 現状維持 | 在庫ありのみ |
| `priceBands` | `List<PriceBand>` | 現状維持 | 価格帯 |
| `colorIds` | `List<Long>` | 現状維持 | カラー ID リスト |
| `categoryFilter` | `ProductCategoryFilter` | 現状維持 | カテゴリ固有絞り込み |
| `sort` | `ProductSort` | 現状維持 | 並び順 |
| `page` | `int` | 現状維持 | ページ番号 |
| `size` | `int` | 現状維持 | ページサイズ |
| `saleStartFrom` | `OffsetDateTime` | 現状維持 | 新着判定下限 |
| **`tasteNames`** | **`List<String>`** | **新規追加** | **検索結果画面のテイスト絞り込み（display_name のリスト）** |

#### 設計判断

- `tasteNames` は `ProductCategoryFilter`（カテゴリ固有フィルタ）ではなく `ProductSearchCondition` 直下に置く。
  理由：検索結果画面のテイスト絞り込みはカテゴリ横断であり、カテゴリ固有のものではないため。
- `keyword` フィールド自体の型は変更しないが、格納される値は `ProductListSearchService` で正規化済みの文字列となる。

---

## 2. `CatalogController`（Controller）

### 変更対象メソッド：`searchResults()`

#### 追加するリクエストパラメータ

| パラメータ名 | 型 | アノテーション |
|------------|-----|--------------|
| `taste` | `List<String>` | `@RequestParam(value = "taste", required = false)` |

#### 変更後の処理フロー

```java
@GetMapping("/products/search")
public String searchResults(
    @RequestParam(value = "q", required = false) String rawKeyword,
    @RequestParam(value = "inStockOnly", defaultValue = "false") boolean inStockOnly,
    @RequestParam(value = "priceBand", required = false) List<Integer> rawPriceBandIds,
    @RequestParam(value = "color", required = false) List<String> rawColorKeys,
    @RequestParam(value = "taste", required = false) List<String> rawTasteNames, // 新規追加
    @RequestParam(value = "sort", required = false) String rawSort,
    @RequestParam(value = "page", defaultValue = "1") int page,
    @RequestParam(value = "size", defaultValue = "20") int size,
    Model model) {

    // 統合テイスト選択肢を取得（新規）
    List<String> tasteOptions = productFilterOptionService.loadUnifiedSearchTasteOptions();

    // テイスト絞り込み条件を正規化（新規）
    List<String> tasteNames = productFilterOptionService.normalizeSearchTasteNames(rawTasteNames, tasteOptions);

    // 既存の条件組み立て（keyword の正規化は Service 内で実施）
    ProductSearchCondition condition = productListSearchService.buildCondition(
        null, rawKeyword, inStockOnly, rawPriceBandIds, rawColorKeys,
        null, rawSort, page, size, tasteNames // tasteNames を追加引数として渡す
    );

    // 以降は既存処理
    // ...

    // テイスト選択肢・選択状態を Model に追加（新規）
    model.addAttribute("searchTasteOptions", tasteOptions);
    model.addAttribute("selectedTasteNames", tasteNames);

    return "pages/product-list-search-results";
}
```

> **注意：** `buildCondition()` のシグネチャ変更は後述の `ProductListSearchService` 設計を参照。

---

## 3. `ProductListSearchService`（Service）

### 3.1 新規メソッド：`normalizeSearchKeyword()`

全角・半角正規化を行うユーティリティメソッド。

```java
/**
 * 検索キーワードを全角へ正規化する（半角数字・英字・カタカナ → 全角）。
 * null または空文字の場合は null を返す。
 */
private String normalizeSearchKeyword(String rawKeyword) {
    if (rawKeyword == null || rawKeyword.isBlank()) {
        return null;
    }
    return TextNormalizer.toFullWidth(rawKeyword.strip());
}
```

### 3.2 変更メソッド：`buildCondition()`

- キーワードを `normalizeSearchKeyword()` で正規化してから `ProductSearchCondition` に格納する。
- `tasteNames` パラメータを受け取り `ProductSearchCondition` に設定する。

```java
public ProductSearchCondition buildCondition(
    String categoryId,
    String rawKeyword,
    boolean inStockOnly,
    List<Integer> rawPriceBandIds,
    List<String> rawColorKeys,
    // ... 既存パラメータ ...
    List<String> tasteNames  // 新規追加
) {
    String normalizedKeyword = normalizeSearchKeyword(rawKeyword);
    // ... 既存の正規化処理 ...

    return new ProductSearchCondition(
        categoryId,
        normalizedKeyword,  // 正規化済み keyword
        inStockOnly,
        priceBands,
        colorIds,
        categoryFilter,
        sort,
        correctedPage,
        size,
        saleStartFrom,
        tasteNames          // 新規追加
    );
}
```

### 3.3 新規ユーティリティクラス：`TextNormalizer`

`ProductListSearchService` からの呼び出しを受ける静的ユーティリティクラス。

**配置パス（案）：** `src/main/java/.../util/TextNormalizer.java`

```java
/**
 * 全角・半角テキスト正規化ユーティリティ。
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /**
     * 入力文字列の半角数字・英字・カタカナを全角へ変換する。
     */
    public static String toFullWidth(String input) {
        // 実装方式は未決事項 #1 の確定後に決定する。
        // 候補：
        //   A. ICU4J UCharacterIterator / Normalizer2 を利用
        //   B. 文字コードの算術変換（数字・英字は +0xFEE0、カタカナは個別マッピング）
        throw new UnsupportedOperationException("実装は未決事項 #1 の決定後");
    }
}
```

---

## 4. `ProductFilterOptionService`（Service）

### 4.1 新規メソッド：`loadUnifiedSearchTasteOptions()`

3 カテゴリのテイストマスタを名称統合した選択肢リストを返す。

```java
/**
 * 検索結果画面用の統合テイスト選択肢（display_name のリスト）を返す。
 * 3 カテゴリのマスタを名称で統合・重複排除する。
 */
public List<String> loadUnifiedSearchTasteOptions() {
    return productFilterOptionRepository.findUnifiedSearchTasteOptions();
}
```

### 4.2 新規メソッド：`normalizeSearchTasteNames()`

画面入力（生値）をホワイトリスト（有効な display_name 一覧）と照合して正規化する。

```java
/**
 * 生入力のテイスト名リストをホワイトリスト照合により正規化する。
 *
 * @param rawNames    リクエストパラメータで受け取ったテイスト名リスト（null 可）
 * @param validNames  loadUnifiedSearchTasteOptions() が返す有効な名称リスト
 * @return ホワイトリストに存在する名称のみを含むリスト。null または空の場合は空リスト。
 */
public List<String> normalizeSearchTasteNames(List<String> rawNames, List<String> validNames) {
    if (rawNames == null || rawNames.isEmpty()) {
        return List.of();
    }
    Set<String> validSet = new HashSet<>(validNames);
    return rawNames.stream()
        .filter(validSet::contains)
        .distinct()
        .toList();
}
```

---

## 5. `ProductFilterOptionRepository`（Repository）

### 新規メソッド：`findUnifiedSearchTasteOptions()`

```java
/**
 * 検索結果画面用の統合テイスト選択肢を取得する。
 * 3 カテゴリのマスタを UNION し、display_name で重複排除・sort_order 昇順に並べる。
 *
 * @return テイスト display_name のリスト
 */
public List<String> findUnifiedSearchTasteOptions() {
    return productMapper.selectUnifiedSearchTasteOptions()
        .stream()
        .map(ProductFilterOptionMapperRow::displayName)
        .toList();
}
```

- `productMapper.selectUnifiedSearchTasteOptions()` の SQL は SQL 設計書 §5 を参照。
- 既存の `ProductFilterOptionMapperRow` を再利用するが、`option_id` は使用しない（`displayName` のみ取得）。

---

## 6. `ProductRepository`（Repository）

### 変更対象メソッド：`buildSearchParams()`

#### キーワード関連パラメータの変更

```java
// 変更前
String keywordLike = keyword != null ? "%" + keyword + "%" : null;
params.put("keywordLike", keywordLike);

// 変更後（keyword はすでに正規化済みの前提）
if (keyword != null) {
    params.put("keywordLike",   "%" + keyword + "%");  // 部分一致用
    params.put("keywordExact",  keyword);               // 完全一致用（新規）
    params.put("keywordPrefix", keyword + "%");         // 前方一致用（新規）
} else {
    params.put("keywordLike",   null);
    params.put("keywordExact",  null);
    params.put("keywordPrefix", null);
}
```

#### テイスト絞り込みパラメータの追加

```java
List<String> tasteNames = condition.tasteNames();
boolean hasSearchTasteFilter = tasteNames != null && !tasteNames.isEmpty();
params.put("searchTasteNames",    tasteNames);
params.put("hasSearchTasteFilter", hasSearchTasteFilter);
```

---

## 7. クラス間の依存関係と呼び出し順序（検索結果画面）

```
CatalogController.searchResults()
  │
  ├─ ProductFilterOptionService.loadUnifiedSearchTasteOptions()
  │     └─ ProductFilterOptionRepository.findUnifiedSearchTasteOptions()
  │           └─ ProductMapper.selectUnifiedSearchTasteOptions()
  │
  ├─ ProductFilterOptionService.normalizeSearchTasteNames()
  │     （ホワイトリスト照合のみ、DB アクセスなし）
  │
  └─ ProductListSearchService.buildCondition()
        │  normalizeSearchKeyword() で keyword を全角変換
        └─ ProductRepository.search() / count()
              │  buildSearchParams() でパラメータ変換
              └─ ProductMapper.selectProducts() / countProducts()
```

---

## 8. 変更不要なクラス

| クラス | 理由 |
|--------|------|
| `ProductCategoryFilter` | 検索結果用テイストは横断フィルタのため、カテゴリ固有フィルタには含めない |
| カテゴリ一覧系 Service / Repository | 今回のスコープ外（カテゴリ一覧画面は対象外）|
| `ProductListPage` / `ProductListMapperRow` 等 | 検索結果の表示仕様には変更なし |
