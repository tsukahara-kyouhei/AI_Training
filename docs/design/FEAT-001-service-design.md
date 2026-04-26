# FEAT-001 サービス詳細設計書

| 項目       | 内容                                                                         |
| ---------- | ---------------------------------------------------------------------------- |
| Issue ID   | FEAT-001                                                                     |
| 作成日     | 2026-04-27                                                                   |
| 対象クラス | `ProductFilterOptionService` / `ProductListSearchService` / `ProductService` |

---

## 1. 対象クラスと変更概要

| クラス                       | 変更種別 | 内容                                     |
| ---------------------------- | -------- | ---------------------------------------- |
| `ProductFilterOptionService` | 修正     | `buildSearchTasteFilter()` メソッド追加  |
| `ProductListSearchService`   | 変更なし | 既存の11引数オーバーロードをそのまま利用 |
| `ProductService`             | 修正     | `getTopRankedProducts()` メソッド追加    |

---

## 2. ProductFilterOptionService — `buildSearchTasteFilter()` 追加

### 2-1. 追加背景

検索結果画面ではカテゴリを絞らずにテイストフィルタを適用する必要がある。
既存の `buildDeskFilter()` / `buildChairFilter()` / `buildStorageFilter()` はカテゴリ単体向けのため、
3カテゴリのテイストをまとめて `ProductCategoryFilter` に設定する専用メソッドを追加する。

### 2-2. メソッドシグネチャ

```java
/**
 * 検索結果画面向けのテイストフィルタを組み立てる。
 *
 * <p>カテゴリ固有条件のうちテイストのみを設定した {@link ProductCategoryFilter} を返す。
 * テイスト以外のフィールド（天板形状・寸法・機能・素材・用途）はすべて空リストとなる。
 *
 * @param rawDeskTasteIds    デスクテイストの生入力値（null 可）
 * @param rawChairTasteIds   チェアテイストの生入力値（null 可）
 * @param rawStorageTasteIds 収納テイストの生入力値（null 可）
 * @param optionsBundle      絞り込み候補群（許容 ID のホワイトリスト照合用）
 * @return テイストのみ設定した ProductCategoryFilter
 */
public ProductCategoryFilter buildSearchTasteFilter(
    List<Integer> rawDeskTasteIds,
    List<Integer> rawChairTasteIds,
    List<Integer> rawStorageTasteIds,
    ProductFilterOptionsBundle optionsBundle)
```

### 2-3. ロジック

```java
public ProductCategoryFilter buildSearchTasteFilter(
        List<Integer> rawDeskTasteIds,
        List<Integer> rawChairTasteIds,
        List<Integer> rawStorageTasteIds,
        ProductFilterOptionsBundle optionsBundle) {

    List<Integer> validDeskTasteIds    = normalizeOptionIds(rawDeskTasteIds,    optionsBundle.deskTasteOptions());
    List<Integer> validChairTasteIds   = normalizeOptionIds(rawChairTasteIds,   optionsBundle.chairTasteOptions());
    List<Integer> validStorageTasteIds = normalizeOptionIds(rawStorageTasteIds, optionsBundle.storageTasteOptions());

    return new ProductCategoryFilter(
        List.of(),              // deskTopShapeIds
        List.of(),              // deskWidthBandIds
        List.of(),              // deskDepthBandIds
        List.of(),              // deskHeightBandIds
        validDeskTasteIds,      // deskTasteIds
        List.of(),              // chairFunctionIds
        List.of(),              // chairMaterialIds
        validChairTasteIds,     // chairTasteIds
        List.of(),              // storageUsageIds
        validStorageTasteIds    // storageTasteIds
    );
}
```

### 2-4. `normalizeOptionIds` プライベートメソッド

既存の `normalizePriceBandIds` と同じパターンで実装する。

```java
/**
 * 生入力のオプション ID リストを正規化する。
 *
 * <p>null 除去・重複排除・許容 ID 以外の除外を行う。
 *
 * @param rawIds    生入力 ID リスト（null 可）
 * @param options   許容する選択肢一覧
 * @return 正規化済み ID リスト（変更不可）
 */
private List<Integer> normalizeOptionIds(
        List<Integer> rawIds,
        List<CategoryFilterOption> options) {
    if (rawIds == null || rawIds.isEmpty()) {
        return List.of();
    }
    Set<Integer> allowedIds = options.stream()
        .map(CategoryFilterOption::id)
        .collect(Collectors.toSet());
    return rawIds.stream()
        .filter(Objects::nonNull)
        .filter(allowedIds::contains)
        .distinct()
        .toList();
}
```

> **既存类似ロジックとの重複確認**: 同クラス内に `normalizePriceBandIds` 等が存在する場合は、
> 共通化可能か検討した上で実装する。重複が軽微な場合はそのまま個別メソッドとして追加してよい。

---

## 3. ProductListSearchService — 変更なし

### 3-1. 利用するオーバーロード

テイストフィルタは `ProductCategoryFilter` として渡せるため、既存の11引数オーバーロードをそのまま使用する。

```java
public ProductSearchCondition buildCondition(
    String categoryId,
    String keyword,
    boolean inStockOnly,
    List<Integer> rawPriceBandIds,
    List<String> rawColorKeys,
    ProductCategoryFilter categoryFilter,  // ← tasteFilter を渡す
    String sort,
    int page,
    int size,
    ProductSort defaultSort,
    ProductFilterOptionsBundle optionsBundle)
```

Controller から呼び出す際は `categoryId = null`（全カテゴリ対象）を渡す。

### 3-2. サービス内での動作

このオーバーロードは価格帯・カラーを正規化した後、`ProductService.buildCondition()` に委譲する。
`categoryFilter`（ここではテイストフィルタ）はそのまま `ProductSearchCondition` に格納される。

---

## 4. ProductService — `getTopRankedProducts()` 追加

### 4-1. 追加背景

`CatalogController.searchResults()` の0件時処理から呼び出すため、
`ProductRepository.findTopRankedProducts()` へのアクセスを `ProductService` 経由で公開する。

### 4-2. メソッドシグネチャ

```java
/**
 * 最新ランキング日の人気商品を指定件数取得する。
 *
 * <p>ランキングデータが存在しない場合は新着商品にフォールバックする
 * （{@link ProductRepository#findTopRankedProducts} の既存実装に準拠）。
 *
 * @param limit 取得件数上限（FR-007 では 8 を渡す）
 * @return 順位付き商品カード一覧
 */
public List<RankedProductCardView> getTopRankedProducts(int limit)
```

### 4-3. ロジック

```java
public List<RankedProductCardView> getTopRankedProducts(int limit) {
    return productRepository.findTopRankedProducts(limit);
}
```

`ProductRepository.findTopRankedProducts()` は以下の既存実装を持つ（変更不要）:

- `popular_product_rankings` から `MAX(ranking_date)` の最新日付のデータを取得
- `rank` 昇順で最大 `limit` 件を返す
- ランキングテーブルが空の場合は新着商品にフォールバックする

### 4-4. 呼び出し元との関係

```
CatalogController.searchResults()
  └─ productService.getTopRankedProducts(8)
       └─ productRepository.findTopRankedProducts(8)
            └─ ProductMapper.selectTopRankedProducts(limit)   ← SQL変更なし
```
