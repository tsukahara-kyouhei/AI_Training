# FEAT-001 コンポーネント（Java層）設計書

## 1. 変更コンポーネント一覧

| レイヤー | クラス／ファイル | 変更種別 | 変更概要 |
|---------|----------------|---------|---------|
| Model | `ProductSearchCondition` | 変更 | `tasteIds` フィールド追加 |
| Model | `ProductCategoryFilter` | 変更なし | 既存構造を再利用（カテゴリ一覧向けテイストはそのまま）|
| Service | `ProductListSearchService` | 変更 | `taste` パラメータ受け渡し用オーバーロード追加 |
| Service | `ProductService` | 変更 | `tasteIds` を含む `buildCondition` オーバーロード追加 |
| Repository | `ProductRepository` | 変更 | テキスト正規化処理の追加、キーワードパラメータ拡張、テイストパラメータ追加 |
| Controller | `CatalogController` | 変更 | `taste` リクエストパラメータ追加 |
| Utility | `KeywordNormalizer`（新規） | 新規 | 全角・半角正規化ユーティリティ（`common` パッケージ） |
| Model | `ProductFilterOptionsBundle` | 変更 | `searchTasteOptions` フィールド追加 |
| Service | `ProductFilterOptionService` | 変更 | `searchTasteOptions` のロード処理追加 |

---

## 2. モデル設計

### 2.1 `ProductSearchCondition`

**パッケージ**: `jp.co.skig.officeorder.model.product`

**変更前**

```java
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
    OffsetDateTime saleStartFrom
)
```

**変更後**

```java
public record ProductSearchCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<PriceBand> priceBands,
    List<Long> colorIds,
    List<Integer> tasteIds,             // 追加: キーワード検索画面向けカテゴリ横断テイストID一覧
    ProductCategoryFilter categoryFilter,
    ProductSort sort,
    int page,
    int size,
    OffsetDateTime saleStartFrom
)
```

**設計方針**

- `ProductCategoryFilter` はカテゴリ一覧画面（SRCH003〜005）専用のカテゴリ固有条件として引き続き使用する。
- `tasteIds` はキーワード検索画面（SRCH002）用の**カテゴリ横断テイスト絞込**専用フィールドとして独立して定義する。
- 既存のすべての `ProductSearchCondition` 生成箇所で `tasteIds = List.of()` を渡すよう修正する。

**既存コンストラクタ呼び出し箇所の対応**

| 呼び出しクラス | 変更内容 |
|--------------|---------|
| `ProductService#buildCondition` | `tasteIds` 引数を受け取り、`ProductSearchCondition` に渡す |
| `ProductService#normalize` | `condition.tasteIds()` を null 安全に処理する |
| `ProductListSearchService#searchWithPageCorrection` | ページ補正時のコンストラクタ呼び出しに `tasteIds` を追加 |
| `ProductRepository#findNewestProducts` | `tasteIds = List.of()` を追加 |

### 2.2 `ProductCategoryFilter`

変更なし。

### 2.3 `ProductFilterOptionsBundle`

**パッケージ**: `jp.co.skig.officeorder.model.product`

**変更種別**: 変更

キーワード検索画面（SRCH002）向けのテイスト選択肢を格納するフィールドを追加する。

**変更前**

```java
public record ProductFilterOptionsBundle(
    List<ColorFilterOption> colorOptions,
    List<CategoryFilterOption> deskTopShapeOptions,
    List<CategoryFilterOption> deskTasteOptions,
    List<CategoryFilterOption> chairFunctionOptions,
    List<CategoryFilterOption> chairMaterialOptions,
    List<CategoryFilterOption> chairTasteOptions,
    List<CategoryFilterOption> storageUsageOptions,
    List<CategoryFilterOption> storageTasteOptions
)
```

**変更後**

```java
public record ProductFilterOptionsBundle(
    List<ColorFilterOption> colorOptions,
    List<CategoryFilterOption> deskTopShapeOptions,
    List<CategoryFilterOption> deskTasteOptions,
    List<CategoryFilterOption> chairFunctionOptions,
    List<CategoryFilterOption> chairMaterialOptions,
    List<CategoryFilterOption> chairTasteOptions,
    List<CategoryFilterOption> storageUsageOptions,
    List<CategoryFilterOption> storageTasteOptions,
    List<CategoryFilterOption> searchTasteOptions   // 追加: キーワード検索画面向けテイスト選択肢
)
```

**設計方針**

- テイストマスタは `desk_tastes`・`chair_tastes`・`storage_tastes` で同一テイスト名に同一 ID が保証されているため、`desk_tastes` を代表として `searchTasteOptions` に格納する。
- 既存の `deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` はカテゴリ一覧画面向けとして変更しない。

### 2.4 `ProductFilterOptionService`

**パッケージ**: `jp.co.skig.officeorder.service.product`

**変更種別**: 変更

`loadOptionsBundle()` メソッドで `searchTasteOptions` を追加ロードする。

**変更後の `loadOptionsBundle` 実装**:

```java
public ProductFilterOptionsBundle loadOptionsBundle() {
    return new ProductFilterOptionsBundle(
        productFilterOptionRepository.findActiveColorOptions(),
        productFilterOptionRepository.findActiveDeskTopShapeOptions(),
        productFilterOptionRepository.findActiveDeskTasteOptions(),
        productFilterOptionRepository.findActiveChairFunctionOptions(),
        productFilterOptionRepository.findActiveChairMaterialOptions(),
        productFilterOptionRepository.findActiveChairTasteOptions(),
        productFilterOptionRepository.findActiveStorageUsageOptions(),
        productFilterOptionRepository.findActiveStorageTasteOptions(),
        productFilterOptionRepository.findActiveDeskTasteOptions()  // searchTasteOptions として desk_tastes を使用
    );
}
```

---

## 3. テキスト正規化処理設計

### 3.1 ユーティリティクラス `KeywordNormalizer`

**パッケージ**: `jp.co.skig.officeorder.common`  
**変更種別**: 新規作成

非インスタンス化ユーティリティクラスとして実装する。

**クラス定義**

```java
package jp.co.skig.officeorder.common;

public final class KeywordNormalizer {
    private KeywordNormalizer() {}

    /**
     * 検索キーワードを正規化する。
     * - 全角英数字 → 半角英数字
     * - 半角カタカナ → 全角カタカナ（濁点・半濁点の合成を含む）
     *
     * @param input 入力文字列（null 許容）
     * @return 正規化済み文字列。null または空入力の場合は空文字列を返す
     */
    public static String normalize(String input) { ... }
}
```

### 3.2 正規化仕様

#### (1) 全角英数字 → 半角英数字

| 文字種 | Unicode 範囲 | 変換例 |
|-------|-------------|-------|
| 全角英大文字 | U+FF21〜U+FF3A | `Ａ` → `A` |
| 全角英小文字 | U+FF41〜U+FF5A | `ａ` → `a` |
| 全角数字 | U+FF10〜U+FF19 | `１` → `1` |

変換式: `(char)(c - 0xFEE0)`（全角数字・英字共通）

#### (2) 半角カタカナ → 全角カタカナ

`java.text.Normalizer.normalize(NFKC)` は漢字・ひらがなも変換してしまうため使用しない。  
カタカナのみを対象とした独自変換テーブルを実装する。

| 半角カタカナ | 全角カタカナ |
|------------|-----------|
| ｦ〜ﾟ (U+FF66〜U+FF9F) | ヲ〜゜ (U+30F2〜U+309C) |

濁点（`ﾞ` U+FF9E）および半濁点（`ﾟ` U+FF9F）が後続する場合は前の文字と合成する。

例:
- `ｶﾞ` → `ガ`
- `ﾊﾟ` → `パ`
- `ｵﾌｨｽ` → `オフィス`

#### (3) 対象外文字種

以下は正規化対象外とし、入力のまま通す。

- ひらがな
- 漢字
- 全角記号・全角スペース
- ASCII 記号

### 3.3 正規化の適用方針

| 適用箇所 | 方針 |
|---------|------|
| 検索キーワード（入力値） | `ProductRepository#normalizeKeyword` 内で `KeywordNormalizer.normalize` を呼び出す |
| DB 側カラム値 | アプリケーション層では変換しない。DB データは全角カタカナで統一されていることを前提とする |

> **補足**: PostgreSQL の `ILIKE` 演算子により大文字・小文字は自動的に区別されない。  
> 全角・半角の正規化はキーワード側のみ行うため、DBデータが半角カタカナで登録されている場合は正規化後のキーワード（全角）と一致しない。  
> これを避けるにはデータ自体を全角カタカナに統一する必要がある（データ品質の前提条件）。

---

## 4. `ProductRepository` 設計変更

**パッケージ**: `jp.co.skig.officeorder.repository`

### 4.1 追加・変更メソッド

#### `normalizeKeyword(String)` — 新規追加（private）

```java
/**
 * キーワードを正規化して返す。null または正規化後に空になる場合は null を返す。
 */
private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    String normalized = KeywordNormalizer.normalize(keyword.trim());
    return normalized.isBlank() ? null : normalized;
}
```

#### `toKeywordLike(String)` — 変更（既存メソッドに正規化処理を追加）

```java
/**
 * 部分一致用 LIKE 文字列を返す: %keyword%
 * 正規化後が空の場合は null を返す。
 */
private String toKeywordLike(String keyword) {
    String k = normalizeKeyword(keyword);
    return k == null ? null : "%" + k + "%";
}
```

#### `toKeywordExact(String)` — 新規追加（private）

```java
/**
 * 完全一致用文字列を返す: keyword（正規化済み）
 * 正規化後が空の場合は null を返す。
 */
private String toKeywordExact(String keyword) {
    return normalizeKeyword(keyword);
}
```

#### `toKeywordPrefix(String)` — 新規追加（private）

```java
/**
 * 前方一致用 LIKE 文字列を返す: keyword%
 * 正規化後が空の場合は null を返す。
 */
private String toKeywordPrefix(String keyword) {
    String k = normalizeKeyword(keyword);
    return k == null ? null : k + "%";
}
```

### 4.2 `buildSearchParams` の変更

追加するパラメータ:

| パラメータキー | 型 | 変更種別 | 内容 |
|-------------|---|---------|-----|
| `keywordLike` | `String` | 変更 | 正規化処理を適用（`%normalized_keyword%`） |
| `keywordExact` | `String` | 追加 | 完全一致用正規化キーワード |
| `keywordPrefix` | `String` | 追加 | 前方一致用正規化キーワード（`normalized_keyword%`） |
| `tasteIds` | `List<Integer>` | 追加 | カテゴリ横断テイストIDリスト |
| `hasSearchTasteFilter` | `boolean` | 追加 | カテゴリ横断テイスト絞込フラグ |

追加ロジック:

```java
// テイスト絞込
List<Integer> tasteIds = condition.tasteIds() == null ? List.of() : condition.tasteIds();
boolean hasSearchTasteFilter = !tasteIds.isEmpty();
params.put("tasteIds", tasteIds);
params.put("hasSearchTasteFilter", hasSearchTasteFilter);

// キーワード（正規化済み）
params.put("keywordLike",   toKeywordLike(condition.keyword()));
params.put("keywordExact",  toKeywordExact(condition.keyword()));
params.put("keywordPrefix", toKeywordPrefix(condition.keyword()));
```

---

## 5. `ProductService` 設計変更

**パッケージ**: `jp.co.skig.officeorder.service.product`

### 5.1 既存メソッドの委譲変更

既存の `buildCondition` オーバーロードはすべて `tasteIds = List.of()` を追加し、新しい全引数版に委譲する。

### 5.2 全引数版 `buildCondition` のシグネチャ変更

**変更前**:

```java
public ProductSearchCondition buildCondition(
    String categoryId, String keyword, boolean inStockOnly,
    List<Integer> priceBandIds, List<Long> colorIds,
    String sort, int page, int size, ProductSort defaultSort,
    ProductCategoryFilter categoryFilter, OffsetDateTime saleStartFrom
)
```

**変更後**:

```java
public ProductSearchCondition buildCondition(
    String categoryId, String keyword, boolean inStockOnly,
    List<Integer> priceBandIds, List<Long> colorIds,
    List<Integer> tasteIds,                             // 追加
    String sort, int page, int size, ProductSort defaultSort,
    ProductCategoryFilter categoryFilter, OffsetDateTime saleStartFrom
)
```

### 5.3 `normalize` メソッドの変更

`normalize(ProductSearchCondition)` 内で `tasteIds` を null 安全に処理する:

```java
private ProductSearchCondition normalize(ProductSearchCondition condition) {
    // ...（既存処理）
    return new ProductSearchCondition(
        condition.categoryId(),
        condition.keyword(),
        condition.inStockOnly(),
        condition.priceBands() == null ? List.of() : condition.priceBands(),
        condition.colorIds() == null ? List.of() : condition.colorIds(),
        condition.tasteIds() == null ? List.of() : condition.tasteIds(),  // 追加
        condition.categoryFilter() == null ? ProductCategoryFilter.empty() : condition.categoryFilter().normalize(),
        condition.sort() == null ? ProductSort.RECOMMENDED : condition.sort(),
        page,
        size,
        condition.saleStartFrom()
    );
}
```

---

## 6. `ProductListSearchService` 設計変更

**パッケージ**: `jp.co.skig.officeorder.service.product`

### 6.1 SRCH002 専用の検索条件組み立てメソッド追加

`/products/search`（SRCH002）向けに `taste` パラメータを受け取るメソッドを追加する。  
既存の `buildCondition` を `tasteIds = List.of()` で呼び出している箇所との後方互換を保つ。

**追加メソッドシグネチャ**:

```java
/**
 * キーワード検索結果画面向け検索条件を組み立てる。
 * テイスト絞込（カテゴリ横断）を含む。
 */
public ProductSearchCondition buildSearchCondition(
    String keyword,
    boolean inStockOnly,
    List<Integer> rawPriceBandIds,
    List<String> rawColorKeys,
    List<Integer> rawTasteIds,
    String sort,
    int page,
    int size,
    ProductFilterOptionsBundle optionsBundle
)
```

テイストIDの正規化（null 除去・DBマスタとの突合）:

```java
List<Integer> tasteIds = normalizeTasteIds(rawTasteIds, optionsBundle);
```

`normalizeTasteIds` ロジック:

```java
private List<Integer> normalizeTasteIds(List<Integer> rawIds, ProductFilterOptionsBundle bundle) {
    if (rawIds == null || rawIds.isEmpty()) return List.of();
    // searchTasteOptions（desk_tastes を代表として使用）から有効なテイストIDセットを収集する
    Set<Integer> validIds = bundle.searchTasteOptions().stream()
        .map(CategoryFilterOption::id)
        .collect(Collectors.toSet());
    return rawIds.stream()
        .filter(id -> id != null && validIds.contains(id))
        .distinct()
        .toList();
}
```

### 6.2 `searchWithPageCorrection` の変更

ページ補正時の `ProductSearchCondition` コンストラクタ呼び出しに `tasteIds` フィールドを追加する:

```java
ProductSearchCondition corrected = new ProductSearchCondition(
    condition.categoryId(),
    condition.keyword(),
    condition.inStockOnly(),
    condition.priceBands(),
    condition.colorIds(),
    condition.tasteIds(),    // 追加
    condition.categoryFilter(),
    condition.sort(),
    totalPages,
    condition.size(),
    condition.saleStartFrom()
);
```

---

## 7. `CatalogController` 設計変更

**パッケージ**: `jp.co.skig.officeorder.web`

### 7.1 `searchResults` メソッドの変更

#### リクエストパラメータ追加

| パラメータ名 | 型 | 変更 | 備考 |
|-----------|---|------|-----|
| `q` | `String` | 変更なし | キーワード |
| `inStockOnly` | `boolean` | 変更なし | |
| `priceBand` | `List<Integer>` | 変更なし | |
| `color` | `List<String>` | 変更なし | |
| `taste` | `List<Integer>` | **追加** | テイストID（複数選択可） |
| `sort` | `String` | 変更なし | |
| `page` | `int` | 変更なし | |
| `size` | `int` | 変更なし | |

**変更後のメソッドシグネチャ**:

```java
@GetMapping("/products/search")
public String searchResults(
    @RequestParam(name = "q",           required = false)          String keyword,
    @RequestParam(name = "inStockOnly", defaultValue = "false")    boolean inStockOnly,
    @RequestParam(name = "priceBand",   required = false)          List<Integer> rawPriceBandIds,
    @RequestParam(name = "color",       required = false)          List<String>  rawColorKeys,
    @RequestParam(name = "taste",       required = false)          List<Integer> rawTasteIds,  // 追加
    @RequestParam(name = "sort",        required = false)          String sort,
    @RequestParam(name = "page",        defaultValue = "1")        int page,
    @RequestParam(name = "size",        defaultValue = "15")       int size,
    Model model
)
```

#### 検索条件の組み立て変更

既存の `productListSearchService.buildCondition(...)` 呼び出しを  
新しい `buildSearchCondition(...)` 呼び出しに置き換える:

```java
ProductSearchCondition condition = productListSearchService.buildSearchCondition(
    keyword,
    inStockOnly,
    rawPriceBandIds,
    rawColorKeys,
    rawTasteIds,    // 追加
    sort,
    page,
    size,
    optionsBundle
);
```

#### Model 属性追加

| 属性名 | 型 | 内容 |
|-------|---|-----|
| `selectedTasteIds` | `List<Integer>` | 選択中テイストID一覧（テンプレートで選択状態の描画に使用） |
| `tasteOptions` | `List<CategoryFilterOption>` | テイスト選択肢（マスタデータ） |

`tasteOptions` は `optionsBundle.searchTasteOptions()` を使用する（`ProductFilterOptionsBundle` に追加した専用フィールド）。

```java
model.addAttribute("selectedTasteIds", result.condition().tasteIds());
model.addAttribute("tasteOptions",     optionsBundle.searchTasteOptions());
```
