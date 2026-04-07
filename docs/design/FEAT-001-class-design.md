# FEAT-001 商品検索機能強化 クラス設計書

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 詳細設計書 |
| 機能ID | FEAT-001 |
| 対象システム | office-order（オフィス家具ECサイト） |
| 作成日 | 2026-03-24 |
| バージョン | 1.0 |
| 関連設計書 | [FEAT-001-basic-design.md](FEAT-001-basic-design.md) |

---

## 凡例

| 記号 | 意味 |
|------|------|
| 🆕 | 新規追加 |
| ✏️ | 既存変更 |
| ➕ | フィールド / メソッド追加 |

---

## 1. 新規クラス

### 1.1 🆕 `KeywordNormalizer`

**パッケージ：** `jp.co.skig.officeorder.util`

キーワード検索用の文字列正規化ユーティリティ。全角→半角・半角カタカナ→全角カタカナ・大文字→小文字の変換を行い、DB データとの照合に適した形へ整える。

#### フィールド

| フィールド名 | 型 | 説明 |
|------------|------|------|
| `HALF_KANA` | `private static final String` | 半角カタカナ対応表（変換元文字列） |
| `FULL_KANA` | `private static final String` | 全角カタカナ対応表（変換先文字列） |

#### メソッド

| メソッド名 | 戻り値 | 引数 | 説明 |
|-----------|--------|------|------|
| `normalize` | `String` | `String input` | 入力文字列に正規化を適用し返す。`null` / 空文字はそのまま返す。 |

#### 正規化処理の実装仕様

```
normalize(input):
  1. null または空文字 → そのまま返す
  2. 全角 ASCII 数字 (U+FF10〜U+FF19) → 半角数字 (U+0030〜U+0039)
  3. 全角 ASCII 大文字 (U+FF21〜U+FF3A) → 半角小文字 (U+0061〜U+007A)
  4. 全角 ASCII 小文字 (U+FF41〜U+FF5A) → 半角小文字 (U+0061〜U+007A)
  5. 半角カタカナ濁音/半濁音（2文字 ﾞﾟ 付き）→ 対応する全角カタカナ1文字
     例: ｶﾞ→ガ, ｷﾞ→ギ, ..., ﾊﾟ→パ, ﾋﾟ→ピ ...
  6. 残りの半角カタカナ (U+FF66〜U+FF9F) → 対応する全角カタカナ
     例: ｱ→ア, ｲ→イ, ..., ﾝ→ン
  7. 半角英字大文字 (A-Z) → 小文字 (a-z) ※ String#toLowerCase(Locale.ENGLISH)
```

---

## 2. 変更モデルクラス

### 2.1 ✏️ `ProductFilterOptionsBundle`

**パッケージ：** `jp.co.skig.officeorder.model.product`

#### ➕ 追加フィールド

| フィールド名 | 型 | 説明 |
|------------|------|------|
| `searchTasteOptions` | `List<String>` | 検索結果画面用のテイスト名称一覧（3テーブルの UNION 結果）。不変リスト。 |

#### compact コンストラクタ変更内容

`searchTasteOptions` を `immutableOrEmpty()` で不変化する処理を追加する。

#### 変更前後の record 定義

```java
// 変更前
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

// 変更後（末尾にフィールド追加）
public record ProductFilterOptionsBundle(
    List<ColorFilterOption> colorOptions,
    List<CategoryFilterOption> deskTopShapeOptions,
    List<CategoryFilterOption> deskTasteOptions,
    List<CategoryFilterOption> chairFunctionOptions,
    List<CategoryFilterOption> chairMaterialOptions,
    List<CategoryFilterOption> chairTasteOptions,
    List<CategoryFilterOption> storageUsageOptions,
    List<CategoryFilterOption> storageTasteOptions,
    List<String> searchTasteOptions              // ← 追加
)
```

---

### 2.2 ✏️ `ProductCategoryFilter`

**パッケージ：** `jp.co.skig.officeorder.model.product`

#### ➕ 追加フィールド

| フィールド名 | 型 | 説明 |
|------------|------|------|
| `searchTasteNames` | `List<String>` | 検索結果画面で選択されたテイスト名称リスト（カテゴリ横断フィルタ用）。 |

#### 変更前後の record 定義

```java
// 変更後（末尾にフィールド追加）
public record ProductCategoryFilter(
    List<Integer> deskTopShapeIds,
    List<Integer> deskWidthBandIds,
    List<Integer> deskDepthBandIds,
    List<Integer> deskHeightBandIds,
    List<Integer> deskTasteIds,
    List<Integer> chairFunctionIds,
    List<Integer> chairMaterialIds,
    List<Integer> chairTasteIds,
    List<Integer> storageUsageIds,
    List<Integer> storageTasteIds,
    List<String> searchTasteNames                // ← 追加
)
```

#### `empty()` の変更

```java
public static ProductCategoryFilter empty() {
    return new ProductCategoryFilter(
        List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of()   // searchTasteNames
    );
}
```

#### `normalize()` の変更

```java
// searchTasteNames: null 除去・重複除去・不変リスト化
normalizeStringList(searchTasteNames)
```

#### ➕ 追加プライベートメソッド

| メソッド名 | 戻り値 | 引数 | 説明 |
|-----------|--------|------|------|
| `normalizeStringList` | `List<String>` | `List<String> values` | 既存の `normalizeList(List<Integer>)` と同等。null / 空文字列の除去・distinct を行い不変リストを返す。 |

---

## 3. 変更リポジトリクラス

### 3.1 ✏️ `ProductFilterOptionRepository`

**パッケージ：** `jp.co.skig.officeorder.repository`

#### ➕ 追加メソッド

| メソッド名 | 戻り値 | 説明 |
|-----------|--------|------|
| `findActiveSearchTasteOptions` | `List<String>` | 3テーブルを UNION して重複排除したテイスト名称一覧を返す。`is_active = TRUE` の行のみ対象。`display_name` でソート。 |

```java
public List<String> findActiveSearchTasteOptions() {
    return productMapper.selectActiveSearchTasteOptions();
}
```

---

### 3.2 ✏️ `ProductRepository`

**パッケージ：** `jp.co.skig.officeorder.repository`

#### `search(ProductSearchCondition condition)` の変更

**変更概要：** 商品コードの一致モードを制御しながら、完全一致→前方一致の2パス検索を実施する。

```
search(condition):
  1. buildSearchParams(condition, "exact") でパラメータマップを構築
  2. countProducts(params) で件数取得
  3. 件数 = 0 かつ condition.keyword() が非空の場合:
       params = buildSearchParams(condition, "prefix") で再構築
       totalCount = countProducts(params) で再取得
  4. totalCount = 0 → 空の ProductListPage を返す
  5. params に limit, offset を設定して selectProducts(params) で一覧取得
  6. toCard() で ProductCardView に変換して ProductListPage を返す
```

#### `buildSearchParams` のシグネチャ変更

```java
// 変更前
private Map<String, Object> buildSearchParams(ProductSearchCondition condition)

// 変更後
private Map<String, Object> buildSearchParams(
    ProductSearchCondition condition,
    String productCodeMatchMode   // "exact" または "prefix"
)
```

#### `buildSearchParams` への追加パラメータ

| パラメータキー | 値 | 説明 |
|-------------|----|------|
| `productCodeMatchMode` | `"exact"` または `"prefix"` | SQL 内の `<choose>` 分岐に使用 |
| `productCodeExact` | `normalizedKeyword` | 完全一致用（`= #{productCodeExact}`） |
| `productCodePrefix` | `normalizedKeyword + "%"` | 前方一致用（`LIKE #{productCodePrefix}`） |
| `keywordLike` | `"%" + normalizedKeyword + "%"` | テキストフィールドの部分一致用（変更なし） |
| `searchTasteNames` | `List<String>` | テイスト横断フィルタ用名称リスト（新規） |
| `hasSearchTasteFilter` | `boolean` | `searchTasteNames` が空でない場合 true（新規） |

#### `toKeywordLike` メソッドの削除

現行の `toKeywordLike()` は `buildSearchParams()` 内でインライン化に吸収する。

---

## 4. 変更サービスクラス

### 4.1 ✏️ `ProductFilterOptionService`

**パッケージ：** `jp.co.skig.officeorder.service.product`

#### `loadOptionsBundle()` の変更

`searchTasteOptions` を追加するため、コンストラクタ呼び出しを変更する。

```java
return new ProductFilterOptionsBundle(
    productFilterOptionRepository.findActiveColorOptions(),
    productFilterOptionRepository.findActiveDeskTopShapeOptions(),
    productFilterOptionRepository.findActiveDeskTasteOptions(),
    productFilterOptionRepository.findActiveChairFunctionOptions(),
    productFilterOptionRepository.findActiveChairMaterialOptions(),
    productFilterOptionRepository.findActiveChairTasteOptions(),
    productFilterOptionRepository.findActiveStorageUsageOptions(),
    productFilterOptionRepository.findActiveStorageTasteOptions(),
    productFilterOptionRepository.findActiveSearchTasteOptions()   // ← 追加
);
```

#### ➕ 追加メソッド

| メソッド名 | 戻り値 | 引数 | 説明 |
|-----------|--------|------|------|
| `searchTasteOptions` | `List<String>` | なし | `loadOptionsBundle()` から `searchTasteOptions` を返す。 |
| `buildSearchCategoryFilter` | `ProductCategoryFilter` | `List<String> rawTasteNames` | 検索結果画面用のカテゴリフィルタを構築する。デスク/チェア/収納の全フィールドは空リストで、`searchTasteNames` のみ設定する。 |
| `buildSearchCategoryFilter` | `ProductCategoryFilter` | `List<String> rawTasteNames, ProductFilterOptionsBundle optionsBundle` | `searchTasteNames` を valid な選択肢で正規化したうえで `ProductCategoryFilter` を返す。 |

**`buildSearchCategoryFilter` の処理：**

```
1. optionsBundle.searchTasteOptions() で有効な名称セットを取得
2. rawTasteNames から有効な名称のみ抽出（不正＋重複排除）
3. ProductCategoryFilter.empty() に searchTasteNames のみ設定して返す
```

---

### 4.2 ✏️ `ProductListSearchService`

**パッケージ：** `jp.co.skig.officeorder.service.product`

#### `buildCondition()` オーバーロードの変更

現行は `ProductCategoryFilter categoryFilter` を受け取るオーバーロードが既に存在する。このシグネチャは変更しない。呼び出し元で `buildSearchCategoryFilter` を使って `searchTasteNames` 入りの `ProductCategoryFilter` を渡す設計とする。

#### キーワード正規化の追加

`keyword` を `ProductSearchCondition` に格納する前に `KeywordNormalizer.normalize()` を呼び出す。

```java
String normalizedKeyword = KeywordNormalizer.normalize(
    keyword == null ? null : keyword.trim()
);
// その後 productService.buildCondition(categoryId, normalizedKeyword, ...) へ渡す
```

---

## 5. 変更 Controller クラス

### 5.1 ✏️ `CatalogController`

**パッケージ：** `jp.co.skig.officeorder.web`

#### `searchResults()` メソッドの変更

**追加リクエストパラメータ：**

```java
@RequestParam(name = "taste", required = false) List<String> rawTasteNames
```

**処理変更：**

```java
// 変更前
ProductSearchCondition condition = productListSearchService.buildCondition(
    null, keyword, inStockOnly, rawPriceBandIds, rawColorKeys,
    sort, page, size, ProductSort.RECOMMENDED, optionsBundle
);

// 変更後
ProductCategoryFilter searchFilter =
    productFilterOptionService.buildSearchCategoryFilter(rawTasteNames, optionsBundle);
ProductSearchCondition condition = productListSearchService.buildCondition(
    null, keyword, inStockOnly, rawPriceBandIds, rawColorKeys,
    searchFilter,       // ← 変更：カテゴリフィルタを渡す
    sort, page, size, ProductSort.RECOMMENDED, optionsBundle
);
```

**Model への追加属性：**

| 属性名 | 型 | 説明 |
|--------|------|------|
| `searchTasteOptions` | `List<String>` | チェックボックスの選択肢（テンプレートで `th:each`） |
| `selectedTasteNames` | `List<String>` | 選択中のテイスト名称（チェック状態の復元用） |

---

## 6. 変更 Mapper インターフェース

### 6.1 ✏️ `ProductMapper`

**パッケージ：** `jp.co.skig.officeorder.mapper`

#### ➕ 追加メソッド

```java
/**
 * 検索結果画面用のテイスト統合選択肢を取得する。
 * desk_tastes / chair_tastes / storage_tastes を display_name で UNION し、
 * 重複なし・アクティブなもののみを返す。
 */
List<String> selectActiveSearchTasteOptions();
```

---

## 7. 影響を受ける既存メソッド一覧

| クラス | メソッド | 変更内容 |
|--------|---------|---------|
| `ProductFilterOptionsBundle` | コンストラクタ | `searchTasteOptions` 引数追加 |
| `ProductCategoryFilter` | `empty()` | `searchTasteNames` の `List.of()` 追加 |
| `ProductCategoryFilter` | `normalize()` | `searchTasteNames` の正規化処理追加 |
| `ProductCategoryFilter` | `isEmpty()` | `searchTasteNames` が空かどうかを判定に含める |
| `ProductFilterOptionService` | `loadOptionsBundle()` | `findActiveSearchTasteOptions()` 呼び出し追加 |
| `ProductRepository` | `search()` | 2パス検索ロジックへ変更 |
| `ProductRepository` | `buildSearchParams()` | シグネチャ変更・新パラメータ追加 |
| `ProductListSearchService` | `buildCondition()` | キーワード正規化の呼び出し追加 |
| `CatalogController` | `searchResults()` | `taste` パラメータ追加・モデル属性追加 |

---

## 8. 後方互換性への配慮

- `ProductCategoryFilter` の record フィールド追加に伴い、既存のカテゴリ一覧 Controller（`desks()`, `chairs()`, `storages()`）が `buildDeskFilter` / `buildChairFilter` / `buildStorageFilter` 経由で `ProductCategoryFilter` を生成している。これらメソッドは `searchTasteNames = List.of()` として新フィールドを初期化するよう変更する。
- `ProductFilterOptionsBundle` のコンストラクタ引数が増えるため、テストコード等での全引数コンストラクタ呼び出し箇所を追加引数対応に修正する。
