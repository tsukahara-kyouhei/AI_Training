# FEAT-001 コントローラー詳細設計書

| 項目         | 内容                                           |
| ------------ | ---------------------------------------------- |
| Issue ID     | FEAT-001                                       |
| 作成日       | 2026-04-27                                     |
| 対象クラス   | `jp.co.skig.officeorder.web.CatalogController` |
| 対象メソッド | `searchResults()`                              |

---

## 1. 変更概要

| 種別 | 内容                                                     |
| ---- | -------------------------------------------------------- |
| 修正 | テイスト3種のリクエストパラメータ追加                    |
| 追加 | キーワードへの NFKC 正規化処理                           |
| 追加 | 100文字バリデーション（超過時はエラー画面返却）          |
| 追加 | テイストフィルタ組み立て → `buildCondition()` に引き渡し |
| 追加 | 検索結果 0件時の `getTopRankedProducts(8)` 呼び出し      |
| 追加 | Model へのテイスト候補・選択状態・おすすめ商品の追加     |

---

## 2. インポート追加

```java
import java.text.Normalizer;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
```

---

## 3. メソッドシグネチャ

### 現状

```java
@GetMapping("/products/search")
public String searchResults(
    @RequestParam(name = "q",           required = false)              String keyword,
    @RequestParam(name = "inStockOnly", defaultValue = "false")        boolean inStockOnly,
    @RequestParam(name = "priceBand",   required = false)              List<Integer> rawPriceBandIds,
    @RequestParam(name = "color",       required = false)              List<String> rawColorKeys,
    @RequestParam(name = "sort",        required = false)              String sort,
    @RequestParam(name = "page",        defaultValue = "1")            int page,
    @RequestParam(name = "size",        defaultValue = "15")           int size,
    Model model)
```

### 変更後

```java
@GetMapping("/products/search")
public String searchResults(
    @RequestParam(name = "q",            required = false)             String keyword,
    @RequestParam(name = "inStockOnly",  defaultValue = "false")       boolean inStockOnly,
    @RequestParam(name = "priceBand",    required = false)             List<Integer> rawPriceBandIds,
    @RequestParam(name = "color",        required = false)             List<String> rawColorKeys,
    @RequestParam(name = "deskTaste",    required = false)             List<Integer> rawDeskTasteIds,    // 追加
    @RequestParam(name = "chairTaste",   required = false)             List<Integer> rawChairTasteIds,   // 追加
    @RequestParam(name = "storageTaste", required = false)             List<Integer> rawStorageTasteIds, // 追加
    @RequestParam(name = "sort",         required = false)             String sort,
    @RequestParam(name = "page",         defaultValue = "1")           int page,
    @RequestParam(name = "size",         defaultValue = "15")          int size,
    Model model)
```

---

## 4. メソッド本体ロジック

```java
// ① NFKC 正規化
String normalizedKeyword = (keyword == null || keyword.isBlank())
    ? null
    : Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKC);

// ② 入力バリデーション（100文字超でエラー返却）
if (normalizedKeyword != null && normalizedKeyword.length() > 100) {
    ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
    model.addAttribute("keyword", normalizedKeyword);
    model.addAttribute("keywordTooLong", true);
    model.addAttribute("deskTasteIds",        List.of());
    model.addAttribute("chairTasteIds",       List.of());
    model.addAttribute("storageTasteIds",     List.of());
    model.addAttribute("deskTasteOptions",    optionsBundle.deskTasteOptions());
    model.addAttribute("chairTasteOptions",   optionsBundle.chairTasteOptions());
    model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions());
    model.addAttribute("suggestedProducts",   List.of());
    applyProductListModel(model, /*emptyCondition*/ null,
        new ProductListPage(List.of(), 0, 1, size), optionsBundle);
    return "pages/product-list-search-results";
}

// ③ 絞り込み候補ロード
ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();

// ④ テイストフィルタ組み立て（詳細: FEAT-001-service-design.md）
ProductCategoryFilter tasteFilter = productFilterOptionService.buildSearchTasteFilter(
    rawDeskTasteIds, rawChairTasteIds, rawStorageTasteIds, optionsBundle);

// ⑤ 検索条件組み立て（既存の11引数オーバーロードを使用）
ProductSearchCondition condition = productListSearchService.buildCondition(
    null,
    normalizedKeyword,
    inStockOnly,
    rawPriceBandIds,
    rawColorKeys,
    tasteFilter,            // ← 追加（カテゴリフィルタとして渡す）
    sort,
    page,
    size,
    ProductSort.RECOMMENDED,
    optionsBundle
);

// ⑥ 検索実行・ページ補正
ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);

// ⑦ 0件時おすすめ商品取得（詳細: FEAT-001-service-design.md）
List<RankedProductCardView> suggestedProducts = List.of();
if (result.productPage().totalCount() == 0) {
    suggestedProducts = productService.getTopRankedProducts(8);
}

// ⑧ Model 設定
applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
model.addAttribute("keyword",
    result.condition().keyword() == null ? "" : result.condition().keyword());

// 追加 Model（テイスト候補・選択状態・おすすめ商品）
model.addAttribute("suggestedProducts",   suggestedProducts);
model.addAttribute("deskTasteIds",        tasteFilter.deskTasteIds());
model.addAttribute("chairTasteIds",       tasteFilter.chairTasteIds());
model.addAttribute("storageTasteIds",     tasteFilter.storageTasteIds());
model.addAttribute("deskTasteOptions",    optionsBundle.deskTasteOptions());
model.addAttribute("chairTasteOptions",   optionsBundle.chairTasteOptions());
model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions());

return "pages/product-list-search-results";
```

---

## 5. ステップ別判断ロジック詳細

### ① NFKC 正規化

| 入力           | 処理                                                       |
| -------------- | ---------------------------------------------------------- |
| `null` or 空白 | `normalizedKeyword = null`（後続の if 条件は全てスキップ） |
| 値あり         | `keyword.trim()` → `Normalizer.normalize(..., NFKC)`       |

`java.text.Normalizer` を使用。NFKC 変換で全角英数字・記号が半角に統一される。
ひらがな・カタカナは NFKC でも変換されない（FR-004 仕様通り）。

### ② バリデーション

| 条件                                | 処理                       |
| ----------------------------------- | -------------------------- |
| `normalizedKeyword == null`         | バリデーションはスキップ   |
| `normalizedKeyword.length() <= 100` | バリデーションはスキップ   |
| `normalizedKeyword.length() > 100`  | エラー返却（後続処理なし） |

エラー時は Model に `keywordTooLong = true` を設定し、空の検索結果で画面を返す。
テイスト候補・おすすめ商品は空リストで初期化してテンプレートの null 安全を確保する。

### ⑤ buildCondition オーバーロードの選択

既存の11引数オーバーロード（`ProductCategoryFilter categoryFilter` を受け取るもの）をそのまま使用する。
テイストフィルタを `categoryFilter` として渡すことで、`ProductListSearchService` 側の変更は不要。

### ⑦ 0件時おすすめ商品

`result.productPage().totalCount() == 0` を判定基準とする（件数ベース）。
ページ補正後の `result` で判定するため、ページ超過補正と競合しない。

---

## 6. Model 属性一覧（追加分のみ）

| 属性名                | 型                            | 設定タイミング                    |
| --------------------- | ----------------------------- | --------------------------------- |
| `keywordTooLong`      | `Boolean`                     | バリデーションエラー時のみ `true` |
| `suggestedProducts`   | `List<RankedProductCardView>` | 常に設定（0件以外は空リスト）     |
| `deskTasteIds`        | `List<Integer>`               | 常に設定（選択なしは空リスト）    |
| `chairTasteIds`       | `List<Integer>`               | 常に設定（選択なしは空リスト）    |
| `storageTasteIds`     | `List<Integer>`               | 常に設定（選択なしは空リスト）    |
| `deskTasteOptions`    | `List<CategoryFilterOption>`  | 常に設定                          |
| `chairTasteOptions`   | `List<CategoryFilterOption>`  | 常に設定                          |
| `storageTasteOptions` | `List<CategoryFilterOption>`  | 常に設定                          |
