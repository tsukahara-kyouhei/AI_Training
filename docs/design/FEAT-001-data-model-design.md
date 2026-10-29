# FEAT-001 データモデル設計

## 1. 目的

本設計では、検索の入力条件・絞込条件・DB との対応を、現行実装に照らして整理する。

## 2. 確認できた実装

### 2.1 既存確認済み

- `ProductSearchCondition` は record で定義されている
- `ProductCategoryFilter` はカテゴリ属性の境界条件をモデル化している
- `ProductFilterOptionsBundle` は候補一覧をまとめている
- `PriceBand` は価格帯 ID と min/max の対応を持つ
- `ProductSort` は画面の sort 値を保持する
- `ProductMapper.xml` から `products`, `product_variants`, `colors`, `product_desk_attributes`, `product_chair_attributes`, `product_storage_attributes` が使用されている
- `desk_tastes`, `chair_tastes`, `storage_tastes` 系テーブルを `selectActive*TasteOptions` で参照している

### 2.2 今回設計

- 検索条件モデルと DB テーブルの関連性を明示する
- キーワード検索に対する比較文字列の正規化責務を定義する
- テイスト条件の受け渡しを Controller → Service → Repository → SQL で一貫させる

### 2.3 分類: EXISTING / FEAT-001 DESIGN / UNRESOLVED

- EXISTING
  - `ProductSearchCondition` は `keyword` を単一の `String` として持つ
  - `ProductCategoryFilter` はカテゴリ属性ごとに `*Ids` を持つ
  - `taste_id` はカテゴリごとの `*tastes` テーブルに対するローカルな FK であり、カテゴリごとに別意味を持つ
- FEAT-001 DESIGN
  - `keywordExactNormalized`, `keywordPrefixPattern`, `keywordContainsPattern` は論理比較値であり、DB カラム名ではない
  - `product_code` は `LOWER(product_code) = :keywordExactNormalized` または `LOWER(product_code) LIKE :keywordPrefixPattern ESCAPE '\'` で比較する
  - `normalizedProductName`, `normalizedVariationName`, `normalizedDescription` は SQL カラムではなく比較用の論理値を表す
- UNRESOLVED
  - なし。以下の節で最終仕様を完全確定する

### 2.4 最終確定事項

- `taste_id` はカテゴリごとの `*tastes` マスタを指すローカルな FK であり、`desk_tastes`, `chair_tastes`, `storage_tastes` は別テーブルである
- `product_code` は Java で `trim` / `Normalizer.normalize(..., Form.NFKC)` / `toLowerCase(Locale.ROOT)` をかけた後、`LOWER(product_code) = :keywordExactNormalized` または `LOWER(product_code) LIKE :keywordPrefixPattern ESCAPE '\'` で比較する
- 実データの `product_code` は `P0001-C01` 形式の ASCII 大文字で保存されている。全角文字の値は本実装の対象外とし、入力値を正規化した上で `LOWER(product_code)` と比較する
- `normalizedProductName` 系は DB カラムではなく、比較用の正規化済み論理値を表す

## 3. 検索条件モデル

### 3.1 ProductSearchCondition

現行実装に合わせて以下を採用する。

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
) {}
```

責務:
- 検索入力から正規化済み条件を生成する
- 画面と SQL の両方をつなぐ条件オブジェクトとして使用する

### 3.2 ProductCategoryFilter

現行実装の定義を採用する。

```java
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
    List<Integer> storageTasteIds
) {}
```

責務:
- カテゴリ属性による絞込条件を持つ
- 各カテゴリの `taste` はここに持つ
- `normalize()` で null と重複を除去する

## 4. フィルタ候補モデル

### 4.1 ProductFilterOptionsBundle

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
) {}
```

責務:
- 画面に表示する絞込候補を後続の Service に渡す
- 値の安全性を `extractOptionIds()` で管理する

### 4.2 PriceBand

```java
public enum PriceBand {
  BAND_1(1, null, 20000L),
  BAND_2(2, 20000L, 40000L),
  ...
}
```

責務:
- 画面の `priceBand` 値と SQL の価格範囲を対応させる
- `PriceBand.fromId(id)` で値確認

## 5. DB テーブル対応

### 5.1 `products`

実体:
- `product_id`
- `product_name`
- `category_id`
- `description`
- `assembly_available`
- `assembly_fee`
- `has_variation`
- `variation_group_id`
- `variation_name`
- `sale_start_at`
- `sale_end_at`

利用目的:
- product list の基本件名、カテゴリ、販売期間
- `sale_start_at <= now` と `sale_end_at` の共通条件に使用

### 5.2 `product_variants`

実体:
- `product_variant_id`
- `product_id`
- `product_code`
- `color_id`
- `unit_price`
- `stock_quantity`

利用目的:
- 商品コード検索の比較対象
- 在庫条件
- 価格帯条件
- カラー条件

### 5.3 `colors`

実体:
- `color_id`
- `color_name`
- `color_code`
- `swatch_type`
- `sort_order`
- `is_active`

利用目的:
- `color` フィルタ候補
- `product_variants.color_id` と join

### 5.4 `product_desk_attributes`

実体:
- `product_id`
- `top_shape_id`
- `width_mm`
- `depth_mm`
- `height_mm`
- `taste_id`

利用目的:
- デスク用属性条件
- `deskTopShape`, `deskWidthBand`, `deskDepthBand`, `deskHeightBand`, `deskTaste`

### 5.5 `product_chair_attributes`

実体:
- `product_id`
- `function_id`
- `material_id`
- `taste_id`

利用目的:
- チェア用機能・素材・テイスト条件

### 5.6 `product_storage_attributes`

実体:
- `product_id`
- `usage_id`
- `taste_id`

利用目的:
- 収納家具の用途・テイスト条件

## 6. テイスト系マスタ

実装上は以下を確認できる。

- `desk_tastes`
- `chair_tastes`
- `storage_tastes`
- `top_shape`, `chair_functions`, `chair_materials`, `storage_usages`

各テーブルは `is_active = TRUE` と `sort_order` を使って候補取得を行っている。

## 7. 正規化データモデル

### 7.1 定義するべき比較用文字列

今回設計では、以下を比較用に持つことを明示する。

- `normalizedKeyword`
- `normalizedProductCode`
- `normalizedProductName`
- `normalizedVariationName`
- `normalizedDescription`

### 7.2 ここで決めるべきこと

- `keyword` と商品データの比較は「normalized」済み文字列を基準に行う
- `normalize()` は、入力時に `trim` と Unicode 正規化を行う
- ルールは要件定義の範囲で決める

### 7.3 最終確定事項

- 正規化の対象は英字の大小と全角/半角差、前後空白と Java 文字正規化に限定する。
- `-` や `/` を含む記号はそのまま比較対象とし、表記ゆれの自動正規化はこの実装では行わない。
- `product_code` の例として `ｐ０００１－ｃ０１` は Java の `NFKC` 後に `p0001-c01` となり、DB の `P0001-C01` と `LOWER(product_code)` で同一視できる。
- ただし DB の値自体が正規化されるわけではない。DB 側で一致判定するのは `LOWER(product_code)` に対して `LOWER(...) = ...` / `LOWER(...) LIKE ...` を行うことであり、Java 側の `NFKC` は入力の正規化のみを行う。
- `P0001` はそのままの文字列で exact にはヒットせず、`P0001-C01` / `P0001-C02` の prefix としてのみ扱う。`P00010-C01` は `P0001` の prefix 範囲に含まれない。`P0001XYZ` も `P0001-` 区切りを持たないため prefix 対象外である。

## 8. 検索条件と SQL の対応

| 条件 | Model | SQL 実装 |
|---|---|---|
| キーワード | `keyword` | `p.product_name ILIKE :keywordContainsPattern ESCAPE '\'` / `LOWER(product_code) = :keywordExactNormalized` / `LOWER(product_code) LIKE :keywordPrefixPattern ESCAPE '\'` |
| 在庫 | `inStockOnly` | `pv.stock_quantity > 0` |
| 価格帯 | `PriceBand` | `pv.unit_price >= :minInclusive AND pv.unit_price <= :maxInclusive` |
| カラー | `colorIds` | `pv.color_id IN (:colorIds)` |
| テイスト | `ProductCategoryFilter.*TasteIds` | `EXISTS (SELECT 1 FROM ... WHERE taste_id IN (:tasteIds))` |
| 範囲条件 | `deskWidthBandIds` etc | `width_mm >= :minInclusive AND width_mm <= :maxInclusive` |

## 9. エラー/不正入力の対策としてのモデル設計

- `ProductSort.fromValue()` が不正値を `defaultValue` に変換する
- `ProductFilterOptionService` の許容候補チェックにより、DB に存在しない ID を弾く
- `ProductRepository.search()` は empty result を返し、画面側が 0 件表示を行う

## 10. 完了条件

以下を満たしたときに完了とする。

- `ProductSearchCondition` と `ProductCategoryFilter` が検索条件の中核であることが明確である
- 抽出操作に必要なテーブルとカラムが対応付けられている
- テイストに関する ID と候補一覧の対応が実装式で整理されている
- 要件と DB 設計にズレがない
