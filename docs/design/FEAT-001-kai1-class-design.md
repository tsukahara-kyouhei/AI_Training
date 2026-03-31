# FEAT-001 改修1: テイストマスタ統合 クラス詳細設計書

> 要件定義書: `docs/issues/FEAT-001-requirements_kai1.md`
> 作成日: 2026-03-31

対象ファイル一覧:

| # | レイヤ | ファイルパス |
|---|--------|------------|
| 1 | Mapper IF | `src/main/java/.../mapper/ProductMapper.java` |
| 2 | Model | `src/main/java/.../model/product/ProductCategoryFilter.java` |
| 3 | Model | `src/main/java/.../model/product/ProductFilterOptionsBundle.java` |
| 4 | Repository | `src/main/java/.../repository/ProductFilterOptionRepository.java` |
| 5 | Repository | `src/main/java/.../repository/ProductRepository.java` |
| 6 | Service | `src/main/java/.../service/product/ProductFilterOptionService.java` |
| 7 | Controller | `src/main/java/.../web/CatalogController.java` |
| 8 | Template | `src/main/resources/templates/pages/product-list-category-desk.html` |
| 9 | Template | `src/main/resources/templates/pages/product-list-category-chair.html` |
| 10 | Template | `src/main/resources/templates/pages/product-list-category-storage.html` |

---

## 1. `ProductMapper.java`（Mapper インターフェース）

### 変更内容

| メソッド | 種別 | 変更内容 |
|---------|------|---------|
| `selectActiveDeskTasteOptions()` | **削除** | `desk_tastes` テーブル廃止に伴い削除 |
| `selectActiveChairTasteOptions()` | **削除** | `chair_tastes` テーブル廃止に伴い削除 |
| `selectActiveStorageTasteOptions()` | **削除** | `storage_tastes` テーブル廃止に伴い削除 |
| `selectActiveTasteOptions()` | **新規追加** | `tastes` テーブルから有効テイスト選択肢取得 |
| `selectUnifiedSearchTasteOptions()` | 変更なし | SQL のみ変更（インターフェース シグネチャ変更なし） |

### 変更後シグネチャ

```java
// 削除
// List<ProductFilterOptionMapperRow> selectActiveDeskTasteOptions();
// List<ProductFilterOptionMapperRow> selectActiveChairTasteOptions();
// List<ProductFilterOptionMapperRow> selectActiveStorageTasteOptions();

// 追加
/** カテゴリ横断テイスト選択肢（統合）を取得する。 */
List<ProductFilterOptionMapperRow> selectActiveTasteOptions();

// 変更なし（シグネチャのみ）
/** 検索結果画面用の統合テイスト選択肢を取得する。 */
List<ProductFilterOptionMapperRow> selectUnifiedSearchTasteOptions();
```

---

## 2. `ProductCategoryFilter.java`（Model）

### 変更内容

`deskTasteIds`・`chairTasteIds`・`storageTasteIds` の 3 フィールドを廃止し、
単一の `tasteIds` フィールドへ集約する。

### 変更前後のフィールド構成

| フィールド名 | 型 | 変更前 | 変更後 |
|------------|-----|--------|--------|
| `deskTopShapeIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `deskWidthBandIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `deskDepthBandIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `deskHeightBandIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `deskTasteIds` | `List<Integer>` | あり | **削除** |
| `chairFunctionIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `chairMaterialIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `chairTasteIds` | `List<Integer>` | あり | **削除** |
| `storageUsageIds` | `List<Integer>` | 現状維持 | 現状維持 |
| `storageTasteIds` | `List<Integer>` | あり | **削除** |
| **`tasteIds`** | **`List<Integer>`** | なし | **追加（末尾）** |

### 変更後のクラス定義

```java
public record ProductCategoryFilter(
        List<Integer> deskTopShapeIds,
        List<Integer> deskWidthBandIds,
        List<Integer> deskDepthBandIds,
        List<Integer> deskHeightBandIds,
        List<Integer> chairFunctionIds,
        List<Integer> chairMaterialIds,
        List<Integer> storageUsageIds,
        List<Integer> tasteIds           // 新規統合フィールド（末尾）
) {

    public static ProductCategoryFilter empty() {
        return new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    public ProductCategoryFilter normalize() {
        return new ProductCategoryFilter(
                normalizeList(deskTopShapeIds),
                normalizeList(deskWidthBandIds),
                normalizeList(deskDepthBandIds),
                normalizeList(deskHeightBandIds),
                normalizeList(chairFunctionIds),
                normalizeList(chairMaterialIds),
                normalizeList(storageUsageIds),
                normalizeList(tasteIds)
        );
    }

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

    // normalizeList() は変更なし
}
```

---

## 3. `ProductFilterOptionsBundle.java`（Model）

### 変更内容

`deskTasteOptions`・`chairTasteOptions`・`storageTasteOptions` の 3 フィールドを廃止し、
単一の `tasteOptions` フィールドへ集約する。

### 変更前後のフィールド構成

| フィールド名 | 型 | 変更前 | 変更後 |
|------------|-----|--------|--------|
| `colorOptions` | `List<ColorFilterOption>` | 現状維持 | 現状維持 |
| `deskTopShapeOptions` | `List<CategoryFilterOption>` | 現状維持 | 現状維持 |
| `deskTasteOptions` | `List<CategoryFilterOption>` | あり | **削除** |
| `chairFunctionOptions` | `List<CategoryFilterOption>` | 現状維持 | 現状維持 |
| `chairMaterialOptions` | `List<CategoryFilterOption>` | 現状維持 | 現状維持 |
| `chairTasteOptions` | `List<CategoryFilterOption>` | あり | **削除** |
| `storageUsageOptions` | `List<CategoryFilterOption>` | 現状維持 | 現状維持 |
| `storageTasteOptions` | `List<CategoryFilterOption>` | あり | **削除** |
| **`tasteOptions`** | **`List<CategoryFilterOption>`** | なし | **追加（末尾）** |

### 変更後のクラス定義

```java
public record ProductFilterOptionsBundle(
        List<ColorFilterOption> colorOptions,
        List<CategoryFilterOption> deskTopShapeOptions,
        List<CategoryFilterOption> chairFunctionOptions,
        List<CategoryFilterOption> chairMaterialOptions,
        List<CategoryFilterOption> storageUsageOptions,
        List<CategoryFilterOption> tasteOptions          // 新規統合フィールド（末尾）
) {
    public ProductFilterOptionsBundle {
        colorOptions        = immutableOrEmpty(colorOptions);
        deskTopShapeOptions = immutableOrEmpty(deskTopShapeOptions);
        chairFunctionOptions= immutableOrEmpty(chairFunctionOptions);
        chairMaterialOptions= immutableOrEmpty(chairMaterialOptions);
        storageUsageOptions = immutableOrEmpty(storageUsageOptions);
        tasteOptions        = immutableOrEmpty(tasteOptions);
    }

    // immutableOrEmpty() は変更なし
}
```

---

## 4. `ProductFilterOptionRepository.java`（Repository）

### 変更内容

| メソッド | 種別 | 変更内容 |
|---------|------|---------|
| `findActiveDeskTasteOptions()` | **削除** | `selectActiveDeskTasteOptions()` 廃止に伴い削除 |
| `findActiveChairTasteOptions()` | **削除** | `selectActiveChairTasteOptions()` 廃止に伴い削除 |
| `findActiveStorageTasteOptions()` | **削除** | `selectActiveStorageTasteOptions()` 廃止に伴い削除 |
| `findActiveTasteOptions()` | **新規追加** | `selectActiveTasteOptions()` に委譲 |
| `findUnifiedSearchTasteOptions()` | 変更なし | SQL のみ変更（メソッド実装変更なし） |

### 追加メソッド

```java
/**
 * カテゴリ一覧画面用のテイスト選択肢を取得する（統合テーブル参照）。
 *
 * @return テイスト選択肢のリスト（taste_id, display_name）
 */
public List<CategoryFilterOption> findActiveTasteOptions() {
    return productMapper.selectActiveTasteOptions().stream()
            .map(this::toCategoryFilterOption)
            .filter(option -> option != null)
            .toList();
}
```

---

## 5. `ProductFilterOptionService.java`（Service）

### 5.1 `loadOptionsBundle()` の変更

旧テーブル 3 系統のテイスト取得を廃止し、統合 `findActiveTasteOptions()` に差し替える。

```java
// 変更前
public ProductFilterOptionsBundle loadOptionsBundle() {
    return new ProductFilterOptionsBundle(
            productFilterOptionRepository.findActiveColorOptions(),
            productFilterOptionRepository.findActiveDeskTopShapeOptions(),
            productFilterOptionRepository.findActiveDeskTasteOptions(),     // 削除
            productFilterOptionRepository.findActiveChairFunctionOptions(),
            productFilterOptionRepository.findActiveChairMaterialOptions(),
            productFilterOptionRepository.findActiveChairTasteOptions(),    // 削除
            productFilterOptionRepository.findActiveStorageUsageOptions(),
            productFilterOptionRepository.findActiveStorageTasteOptions()   // 削除
    );
}

// 変更後
public ProductFilterOptionsBundle loadOptionsBundle() {
    return new ProductFilterOptionsBundle(
            productFilterOptionRepository.findActiveColorOptions(),
            productFilterOptionRepository.findActiveDeskTopShapeOptions(),
            productFilterOptionRepository.findActiveChairFunctionOptions(),
            productFilterOptionRepository.findActiveChairMaterialOptions(),
            productFilterOptionRepository.findActiveStorageUsageOptions(),
            productFilterOptionRepository.findActiveTasteOptions()          // 追加
    );
}
```

### 5.2 `buildDeskFilter()` シグネチャの変更

`rawDeskTasteIds` → `rawTasteIds` に変更。
`ProductCategoryFilter` コンストラクタ引数の taste 引数も `tasteIds` に対応。

```java
// 変更前（5引数版）
public ProductCategoryFilter buildDeskFilter(
        List<Integer> rawDeskTopShapeIds,
        List<Integer> rawDeskWidthBandIds,
        List<Integer> rawDeskDepthBandIds,
        List<Integer> rawDeskHeightBandIds,
        List<Integer> rawDeskTasteIds) { ... }

// 変更後（5引数版）
public ProductCategoryFilter buildDeskFilter(
        List<Integer> rawDeskTopShapeIds,
        List<Integer> rawDeskWidthBandIds,
        List<Integer> rawDeskDepthBandIds,
        List<Integer> rawDeskHeightBandIds,
        List<Integer> rawTasteIds) { ... }
```

```java
// 変更前（6引数版・内部実装）
public ProductCategoryFilter buildDeskFilter(
        List<Integer> rawDeskTopShapeIds,
        List<Integer> rawDeskWidthBandIds,
        List<Integer> rawDeskDepthBandIds,
        List<Integer> rawDeskHeightBandIds,
        List<Integer> rawDeskTasteIds,
        ProductFilterOptionsBundle optionsBundle) {

    List<Integer> allowedDeskTopShapeIds = extractOptionIds(optionsBundle.deskTopShapeOptions());
    List<Integer> allowedDeskTasteIds    = extractOptionIds(optionsBundle.deskTasteOptions());  // 変更前
    return new ProductCategoryFilter(
            normalizeIntegerOptions(rawDeskTopShapeIds,     allowedDeskTopShapeIds),
            normalizeIntegerOptions(rawDeskWidthBandIds,    ALLOWED_DESK_WIDTH_BAND_IDS),
            normalizeIntegerOptions(rawDeskDepthBandIds,    ALLOWED_DESK_DEPTH_BAND_IDS),
            normalizeIntegerOptions(rawDeskHeightBandIds,   ALLOWED_DESK_HEIGHT_BAND_IDS),
            normalizeIntegerOptions(rawDeskTasteIds,        allowedDeskTasteIds),               // 変更前
            List.of(), List.of(), List.of(), List.of(), List.of()
    );
}

// 変更後（6引数版・内部実装）
public ProductCategoryFilter buildDeskFilter(
        List<Integer> rawDeskTopShapeIds,
        List<Integer> rawDeskWidthBandIds,
        List<Integer> rawDeskDepthBandIds,
        List<Integer> rawDeskHeightBandIds,
        List<Integer> rawTasteIds,
        ProductFilterOptionsBundle optionsBundle) {

    List<Integer> allowedDeskTopShapeIds = extractOptionIds(optionsBundle.deskTopShapeOptions());
    List<Integer> allowedTasteIds        = extractOptionIds(optionsBundle.tasteOptions());      // 変更後
    return new ProductCategoryFilter(
            normalizeIntegerOptions(rawDeskTopShapeIds,   allowedDeskTopShapeIds),
            normalizeIntegerOptions(rawDeskWidthBandIds,  ALLOWED_DESK_WIDTH_BAND_IDS),
            normalizeIntegerOptions(rawDeskDepthBandIds,  ALLOWED_DESK_DEPTH_BAND_IDS),
            normalizeIntegerOptions(rawDeskHeightBandIds, ALLOWED_DESK_HEIGHT_BAND_IDS),
            List.of(),                                                                          // chairFunctionIds
            List.of(),                                                                          // chairMaterialIds
            List.of(),                                                                          // storageUsageIds
            normalizeIntegerOptions(rawTasteIds, allowedTasteIds)                              // tasteIds（末尾）
    );
}
```

### 5.3 `buildChairFilter()` シグネチャの変更

`rawChairTasteIds` → `rawTasteIds` に変更。

```java
// 変更前（4引数版）
public ProductCategoryFilter buildChairFilter(
        List<Integer> rawChairFunctionIds,
        List<Integer> rawChairMaterialIds,
        List<Integer> rawChairTasteIds) { ... }

// 変更後（4引数版）
public ProductCategoryFilter buildChairFilter(
        List<Integer> rawChairFunctionIds,
        List<Integer> rawChairMaterialIds,
        List<Integer> rawTasteIds) { ... }
```

```java
// 変更後（5引数版・内部実装）
public ProductCategoryFilter buildChairFilter(
        List<Integer> rawChairFunctionIds,
        List<Integer> rawChairMaterialIds,
        List<Integer> rawTasteIds,
        ProductFilterOptionsBundle optionsBundle) {

    List<Integer> allowedChairFunctionIds  = extractOptionIds(optionsBundle.chairFunctionOptions());
    List<Integer> allowedChairMaterialIds  = extractOptionIds(optionsBundle.chairMaterialOptions());
    List<Integer> allowedTasteIds          = extractOptionIds(optionsBundle.tasteOptions());
    return new ProductCategoryFilter(
            List.of(), List.of(), List.of(), List.of(),                                        // desk 系
            normalizeIntegerOptions(rawChairFunctionIds,  allowedChairFunctionIds),
            normalizeIntegerOptions(rawChairMaterialIds,  allowedChairMaterialIds),
            List.of(),                                                                          // storageUsageIds
            normalizeIntegerOptions(rawTasteIds, allowedTasteIds)                              // tasteIds（末尾）
    );
}
```

### 5.4 `buildStorageFilter()` シグネチャの変更

`rawStorageTasteIds` → `rawTasteIds` に変更。

```java
// 変更前（3引数版）
public ProductCategoryFilter buildStorageFilter(
        List<Integer> rawStorageUsageIds,
        List<Integer> rawStorageTasteIds) { ... }

// 変更後（3引数版）
public ProductCategoryFilter buildStorageFilter(
        List<Integer> rawStorageUsageIds,
        List<Integer> rawTasteIds) { ... }
```

```java
// 変更後（4引数版・内部実装）
public ProductCategoryFilter buildStorageFilter(
        List<Integer> rawStorageUsageIds,
        List<Integer> rawTasteIds,
        ProductFilterOptionsBundle optionsBundle) {

    List<Integer> allowedStorageUsageIds = extractOptionIds(optionsBundle.storageUsageOptions());
    List<Integer> allowedTasteIds        = extractOptionIds(optionsBundle.tasteOptions());
    return new ProductCategoryFilter(
            List.of(), List.of(), List.of(), List.of(),                                        // desk 系
            List.of(), List.of(),                                                               // chair 系
            normalizeIntegerOptions(rawStorageUsageIds, allowedStorageUsageIds),
            normalizeIntegerOptions(rawTasteIds, allowedTasteIds)                              // tasteIds（末尾）
    );
}
```

### 5.5 taste ショートハンドメソッドの変更

```java
// 削除
// public List<CategoryFilterOption> deskTasteOptions()    { ... }
// public List<CategoryFilterOption> chairTasteOptions()   { ... }
// public List<CategoryFilterOption> storageTasteOptions() { ... }

// 追加
/** テイスト選択肢（統合）を返すショートハンド。 */
public List<CategoryFilterOption> tasteOptions(ProductFilterOptionsBundle optionsBundle) {
    return optionsBundle.tasteOptions();
}
```

> **注意:** `loadUnifiedSearchTasteOptions()` と `normalizeSearchTasteNames()` は変更なし。

---

## 6. `ProductRepository.java`（Repository）

### `buildSearchParams()` の変更

#### 変更前

```java
// カテゴリ別テイスト ID の取得・設定
List<Integer> deskTasteIds    = filter.deskTasteIds();
List<Integer> chairTasteIds   = filter.chairTasteIds();
List<Integer> storageTasteIds = filter.storageTasteIds();

boolean hasDeskFilter    = !deskTopShapeIds.isEmpty() || ... || !deskTasteIds.isEmpty();
boolean hasChairFilter   = !chairFunctionIds.isEmpty() || !chairMaterialIds.isEmpty()
                             || !chairTasteIds.isEmpty();
boolean hasStorageFilter = !storageUsageIds.isEmpty() || !storageTasteIds.isEmpty();

params.put("deskTasteIds",    deskTasteIds);
params.put("chairTasteIds",   chairTasteIds);
params.put("storageTasteIds", storageTasteIds);
```

#### 変更後

```java
// 統合テイスト ID の取得・設定
List<Integer> tasteIds = filter.tasteIds();

boolean hasDeskFilter    = !deskTopShapeIds.isEmpty() || ... || !tasteIds.isEmpty();
boolean hasChairFilter   = !chairFunctionIds.isEmpty() || !chairMaterialIds.isEmpty()
                             || !tasteIds.isEmpty();
boolean hasStorageFilter = !storageUsageIds.isEmpty() || !tasteIds.isEmpty();

params.put("tasteIds", tasteIds);
```

> **設計補足:**
> `tasteIds` は 3 カテゴリのフィルタ全てが共有する。
> `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` の条件に `!tasteIds.isEmpty()` を追加することで、
> テイストのみ選択した場合でも各属性テーブルのフィルタが有効になる。
> カテゴリ一覧画面では `categoryId` がフィルタされているため、他カテゴリの商品には影響しない。

---

## 7. `CatalogController.java`（Controller）

### 7.1 `desks()` メソッドの変更

```java
// 変更前
@RequestParam(name = "deskTaste", required = false) List<Integer> rawDeskTasteIds,

ProductCategoryFilter deskFilter = productFilterOptionService.buildDeskFilter(
        rawDeskTopShapeIds, rawDeskWidthBandIds, rawDeskDepthBandIds, rawDeskHeightBandIds,
        rawDeskTasteIds, optionsBundle);

model.addAttribute("deskTasteIds",    deskFilter.deskTasteIds());
model.addAttribute("deskTasteOptions", optionsBundle.deskTasteOptions());

// 変更後
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds,

ProductCategoryFilter deskFilter = productFilterOptionService.buildDeskFilter(
        rawDeskTopShapeIds, rawDeskWidthBandIds, rawDeskDepthBandIds, rawDeskHeightBandIds,
        rawTasteIds, optionsBundle);

model.addAttribute("tasteIds",    deskFilter.tasteIds());
model.addAttribute("tasteOptions", optionsBundle.tasteOptions());
```

### 7.2 `chairs()` メソッドの変更

```java
// 変更前
@RequestParam(name = "chairTaste", required = false) List<Integer> rawChairTasteIds,

ProductCategoryFilter chairFilter = productFilterOptionService.buildChairFilter(
        rawChairFunctionIds, rawChairMaterialIds, rawChairTasteIds, optionsBundle);

model.addAttribute("chairTasteIds",    chairFilter.chairTasteIds());
model.addAttribute("chairTasteOptions", optionsBundle.chairTasteOptions());

// 変更後
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds,

ProductCategoryFilter chairFilter = productFilterOptionService.buildChairFilter(
        rawChairFunctionIds, rawChairMaterialIds, rawTasteIds, optionsBundle);

model.addAttribute("tasteIds",    chairFilter.tasteIds());
model.addAttribute("tasteOptions", optionsBundle.tasteOptions());
```

### 7.3 `storages()` メソッドの変更

```java
// 変更前
@RequestParam(name = "storageTaste", required = false) List<Integer> rawStorageTasteIds,

ProductCategoryFilter storageFilter = productFilterOptionService.buildStorageFilter(
        rawStorageUsageIds, rawStorageTasteIds, optionsBundle);

model.addAttribute("storageTasteIds",    storageFilter.storageTasteIds());
model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions());

// 変更後
@RequestParam(name = "taste", required = false) List<Integer> rawTasteIds,

ProductCategoryFilter storageFilter = productFilterOptionService.buildStorageFilter(
        rawStorageUsageIds, rawTasteIds, optionsBundle);

model.addAttribute("tasteIds",    storageFilter.tasteIds());
model.addAttribute("tasteOptions", optionsBundle.tasteOptions());
```

> **注意:** `searchResults()` メソッドは変更なし（`taste` パラメータで `List<String>` を受け取る設計は維持）。

---

## 8. テンプレート変更

### 8.1 `product-list-category-desk.html`

| 変更対象 | 変更前 | 変更後 |
|---------|--------|--------|
| テイストオプション繰り返し | `th:each="option : ${deskTasteOptions}"` | `th:each="option : ${tasteOptions}"` |
| input の name 属性 | `name="deskTaste"` | `name="taste"` |
| チェック状態判定 | `th:checked="${deskTasteIds != null and deskTasteIds.contains(option.id)}"` | `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"` |
| ページネーションリンク | `&deskTaste=...` を含むリンク | `&taste=...` に変更（存在する場合） |

### 8.2 `product-list-category-chair.html`

| 変更対象 | 変更前 | 変更後 |
|---------|--------|--------|
| テイストオプション繰り返し | `th:each="option : ${chairTasteOptions}"` | `th:each="option : ${tasteOptions}"` |
| input の name 属性 | `name="chairTaste"` | `name="taste"` |
| チェック状態判定 | `th:checked="${chairTasteIds != null and chairTasteIds.contains(option.id)}"` | `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"` |
| ページネーションリンク | `&chairTaste=...` を含むリンク | `&taste=...` に変更（存在する場合） |

### 8.3 `product-list-category-storage.html`

| 変更対象 | 変更前 | 変更後 |
|---------|--------|--------|
| テイストオプション繰り返し | `th:each="option : ${storageTasteOptions}"` | `th:each="option : ${tasteOptions}"` |
| input の name 属性 | `name="storageTaste"` | `name="taste"` |
| チェック状態判定 | `th:checked="${storageTasteIds != null and storageTasteIds.contains(option.id)}"` | `th:checked="${tasteIds != null and tasteIds.contains(option.id)}"` |
| ページネーションリンク | `&storageTaste=...` を含むリンク | `&taste=...` に変更（存在する場合） |

---

## 9. 変更なしクラス

| クラス | 理由 |
|--------|------|
| `ProductSearchCondition` | `tasteNames (List<String>)` フィールドは変更なし |
| `ProductListSearchService` | `buildCondition()` シグネチャ変更なし |
| `ProductRepository` - `search()` / `count()` | `buildSearchParams()` の内部変更のみ |
| `product-list-search-results.html` | 検索結果画面のテイスト UI は変更なし |

---

## 10. クラス間の依存関係（変更後）

```
CatalogController.desks() / chairs() / storages()
  │  @RequestParam(name = "taste") List<Integer> rawTasteIds
  │
  ├─ ProductFilterOptionService.loadOptionsBundle()
  │     └─ ProductFilterOptionRepository.findActiveTasteOptions()
  │           └─ ProductMapper.selectActiveTasteOptions()
  │                 └─ SELECT FROM tastes WHERE is_active = TRUE
  │
  ├─ ProductFilterOptionService.buildDeskFilter/buildChairFilter/buildStorageFilter(rawTasteIds)
  │     → normalizeIntegerOptions(rawTasteIds, allowedTasteIds)
  │     → ProductCategoryFilter(tasteIds=[...])
  │
  └─ ProductListSearchService.buildCondition(categoryFilter)
        └─ ProductRepository.buildSearchParams()
              params.put("tasteIds", tasteIds)
              hasDeskFilter = ... || !tasteIds.isEmpty()
              hasChairFilter = ... || !tasteIds.isEmpty()
              hasStorageFilter = ... || !tasteIds.isEmpty()
                └─ ProductMapper.xml
                     DeskAttributeFilter: AND da.taste_id IN #{tasteIds}
                     ChairAttributeFilter: AND ca.taste_id IN #{tasteIds}
                     StorageAttributeFilter: AND sa.taste_id IN #{tasteIds}
```

---

## 11. テスト変更方針

### 11.1 既存テストの更新

| テストファイル | 変更内容 |
|-------------|---------|
| `ProductFilterOptionServiceTest.java` | `loadOptionsBundle()` の検証で `deskTasteOptions` 等の取得を `tasteOptions` に変更。`buildDeskFilter()` 等の引数名変更に追従 |
| `ProductFilterOptionServiceNormalizeTest.java` | テイスト系 normalize 処理の検証は `tasteIds` に統一 |

### 11.2 新規テスト追加（推奨）

| テストファイル | 検証内容 |
|-------------|---------|
| `ProductCategoryFilterTest.java` | `tasteIds` フィールドの `normalize()` / `isEmpty()` 動作確認 |
| `ProductRepositoryTasteIdsTest.java` | `buildSearchParams()` が `tasteIds` を正しく `params` に設定し、`hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` が期待通りになるか検証 |
