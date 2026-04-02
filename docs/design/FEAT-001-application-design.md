# FEAT-001 商品検索機能強化 アプリケーション設計

## 1. 対象クラス

| 区分 | クラス | 変更区分 |
|------|--------|---------|
| Controller | `jp.co.skig.officeorder.web.CatalogController` | 変更 |
| Service | `jp.co.skig.officeorder.service.product.ProductFilterOptionService` | 変更 |
| Service | `jp.co.skig.officeorder.service.product.ProductListSearchService` | 軽微変更 |
| Repository | `jp.co.skig.officeorder.repository.ProductRepository` | 変更 |
| Model | `jp.co.skig.officeorder.model.product.ProductCategoryFilter` | 変更なし |
| Model | `jp.co.skig.officeorder.model.product.ProductSearchCondition` | 変更なし |

---

## 2. Controller 設計

### 2.1 対象メソッド

`CatalogController#searchResults`

### 2.2 変更後シグネチャ

```java
@GetMapping("/products/search")
public String searchResults(
        @RequestParam(name = "q", required = false) String keyword,
        @RequestParam(name = "taste", required = false) List<String> rawTasteNames,
        @RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
        @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
        @RequestParam(name = "color", required = false) List<String> rawColorKeys,
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "15") int size,
        Model model)
```

### 2.3 処理手順

1. `optionsBundle = productFilterOptionService.loadOptionsBundle()`
2. `searchTasteOptions = productFilterOptionService.buildSearchTasteOptions(optionsBundle)`
3. `searchTasteFilter = productFilterOptionService.buildSearchTasteFilter(rawTasteNames, optionsBundle)`
4. `buildCondition` は `ProductCategoryFilter` を受け取る既存オーバーロードを使用
5. `applyProductListModel(...)`
6. `keyword` / `tasteOptions` / `selectedTasteNames` を Model に設定

### 2.4 Model 項目

| 項目 | 型 | 用途 |
|------|----|------|
| `keyword` | `String` | 画面上部の検索キーワード表示 |
| `tasteOptions` | `List<String>` | テイストチェックボックス描画 |
| `selectedTasteNames` | `List<String>` | テイストの選択状態保持 |

---

## 3. ProductFilterOptionService 設計

### 3.1 追加メソッド

```java
public List<String> buildSearchTasteOptions(ProductFilterOptionsBundle optionsBundle)

public ProductCategoryFilter buildSearchTasteFilter(
        List<String> rawTasteNames,
        ProductFilterOptionsBundle optionsBundle)
```

### 3.2 `buildSearchTasteOptions` 仕様

- 入力: `optionsBundle`
- 出力: 検索結果画面向けテイスト名一覧
- 処理:
  1. `deskTasteOptions` / `chairTasteOptions` / `storageTasteOptions` からラベルを収集
  2. 空文字・null を除外
  3. `LinkedHashMap` で重複排除し、先勝ちで順序維持
  4. 表示順はカテゴリ候補が持つ `sort_order` 順に依存する既存取得順を維持

### 3.3 `buildSearchTasteFilter` 仕様

- 入力: `rawTasteNames`, `optionsBundle`
- 出力: `ProductCategoryFilter`
- 処理:
  1. `rawTasteNames` を trim し、空値除外、重複除外
  2. `buildSearchTasteOptions(optionsBundle)` に存在する値のみ許可
  3. 各カテゴリ候補から、ラベル一致する option id を抽出
  4. `new ProductCategoryFilter(...)` を生成
  5. テイスト以外のカテゴリ項目は空配列とする

### 3.4 実装イメージ

```java
return new ProductCategoryFilter(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        resolveTasteIds(allowedTasteNames, optionsBundle.deskTasteOptions()),
        List.of(),
        List.of(),
        resolveTasteIds(allowedTasteNames, optionsBundle.chairTasteOptions()),
        List.of(),
        resolveTasteIds(allowedTasteNames, optionsBundle.storageTasteOptions())
);
```

### 3.5 追加 private メソッド

```java
private List<String> normalizeSearchTasteNames(List<String> rawTasteNames)

private List<Integer> resolveTasteIds(List<String> selectedTasteNames,
                                      List<CategoryFilterOption> options)
```

---

## 4. ProductListSearchService 設計

### 4.1 方針

- 新たな公開メソッドは原則追加しない。
- 検索結果画面では、現在カテゴリ別画面で利用している `buildCondition(..., ProductCategoryFilter, ...)` オーバーロードを使用する。

### 4.2 変更点

- `searchResults` から `ProductCategoryFilter.empty()` ではなく `buildSearchTasteFilter(...)` の結果を渡す。
- `searchWithPageCorrection` は `ProductSearchCondition` の構造変更がないため、シグネチャ変更なし。

---

## 5. ProductRepository 設計

### 5.1 変更方針

- `ProductSearchCondition.keyword` は単一文字列のまま保持する。
- SQL パラメータ変換時に以下を生成する。

| パラメータ | 型 | 内容 |
|-----------|----|------|
| `keywordWords` | `List<String>` | 正規化・分割済み検索ワード |
| `keywordPrefixWords` | `List<String>` | 各ワードに `%` を付与した前方一致用値 |
| `hasSearchTasteFilter` | `boolean` | 検索結果画面向けテイスト条件の有無 |

### 5.2 追加 private メソッド

```java
private List<String> normalizeAndSplitKeywordWords(String keyword)

private String normalizeKeywordToken(String token)

private List<String> toKeywordPrefixWords(List<String> keywordWords)
```

### 5.3 正規化アルゴリズム

1. null / blank は空配列を返す
2. 全角スペースを半角スペースに変換
3. `split("\\s+")` で分割
4. 空要素除外
5. 各トークンに対して以下を実施
   - 全角数字 → 半角数字
   - 全角英字 → 半角英字
   - 全角カタカナ → 半角カタカナ
   - 全角記号（ハイフン等）→ 半角記号
   - 小文字・大文字は DB 側 `ILIKE` で吸収するため、Repository では文字種統一のみ行う
6. 重複除外
7. 最大 5 ワードに制限

### 5.4 `buildSearchParams` 変更点

- 既存 `keywordLike` は廃止し、`keywordWords` / `keywordPrefixWords` を使用する。
- `hasSearchTasteFilter = !deskTasteIds.isEmpty() || !chairTasteIds.isEmpty() || !storageTasteIds.isEmpty()`
- `hasDeskFilter` / `hasChairFilter` / `hasStorageFilter` はカテゴリ別画面用として維持する。

---

## 6. 影響範囲

| 項目 | 影響 |
|------|------|
| 検索結果画面 | あり |
| カテゴリ一覧画面 | なし |
| 新着一覧 | なし |
| 商品詳細 | なし |
| `ProductSearchCondition` | なし |

---

## 7. 例外・バリデーション方針

- `taste` に不正値が来た場合は無視する。
- 6 ワード以上入力された場合は先頭 5 ワードのみ使用する。
- 空白のみ入力は keyword 条件なしとして扱う。
- テイスト選択肢が 0 件の場合、画面上はセクションを非表示にせず空表示にしてもよいが、実装では候補 0 件は通常想定しない。
