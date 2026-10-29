# FEAT-001 実装計画 最終確定版

## 1. 実装対象

実装は既存コードを直接変更して行う。新規クラスは作成しない。

- [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java)
- [src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java)
- [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)

新規クラスの追加は行わない。責務は上記既存クラスへ集約する。

## 2. 実装前提（確定事項）

### 2.1 product_code の検索仕様

- `product_code exact` は完全一致を使用する
- `product_code prefix` は設計書で定義した以下の形式だけを対象とする
- `product_code contains` は実装しない
- `product_name` / `variation_name` / `description` は contains の対象とする

#### 2.1.1 product_code exact / prefix / contains の判定

- exact: `LOWER(pvk.product_code) = #{keywordExactNormalized}`
- prefix: `LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'`
- contains: 実装しない

#### 2.1.2 prefix 境界の固定ルール

- 入力 `P0001` は `p0001-%` を prefix パターンとする
- 入力 `P0001-C` は `p0001-c%` を prefix パターンとする
- 入力 `P0001-C01` は exact のみ扱う
- 入力 `P0001-C010` は exact のみ扱う
- 入力 `P00010` は prefix 対象外
- 入力 `P0001XYZ` は prefix 対象外
- 入力 `P0001-` は prefix 対象外

#### 2.1.3 判定根拠

- `P0001` はベースコードであり、`P0001-C01` と `P0001-C02` を同一系列として扱うため `p0001-%` を prefix とする
- `P0001-C` はカテゴリ境界までの前方一致として `p0001-c%` を prefix とする
- `P00010` は `P0001` を前置に持つが別のベースコードであり、`P0001` そのものの prefix には含めない
- `P0001XYZ` と `P0001-` は `-` で区切られない不正または非対象形式として prefix 対象外とする

### 2.2 keyword の論理構造

keyword検索全体は次の OR を使用する。

```
product_code exact
OR
product_code prefix
OR
product_name contains
OR
variation_name contains
OR
description contains
```

keyword条件全体と、その他フィルタ条件の関係は AND とする。

```
(keyword exact OR keyword prefix OR product_name contains OR variation_name contains OR description contains)
AND price filter
AND color filter
AND stock filter
AND taste filter
AND category filter
AND sale_start filter
```

## 3. 正規化ルール（完全固定）

Javaで行う正規化順序は次の通りとする。

```
trim()
→ NFKC
→ toLowerCase(Locale.ROOT)
```

### 3.1 正規化対象と最終値

| 入力 | 正規化後 | 処理内容 |
|---|---|---|
| `null` | `null` | keyword 条件を無効化 |
| `""` | `null` | keyword 条件を無効化 |
| `"   "` | `null` | 空白のみは条件を無効化 |
| `"ＡＢ－００１２"` | `"ab-0012"` | 全角英数字・記号を NFKC で正規化し小文字化 |
| `"AB-0012"` | `"ab-0012"` | 大文字小文字を統一 |
| `"ab-0012"` | `"ab-0012"` | そのまま |
| `%` | `\%` | contains でリテラルとして扱う |
| `_` | `\_` | contains でリテラルとして扱う |
| `\` | `\\` | contains でリテラルとして扱う |

### 3.2 keyword 正規化の取り扱い

- `null`, `""`, 空白のみ: keyword は未入力として扱い、keyword 条件を付与しない
- `product_code` exact / prefix は正規化後の値を使用する
- `product_name` / `variation_name` / `description` contains は、正規化後に `%` / `_` / `\` を escape した値を使用する
- `normalizedProductName` / `normalizedVariationName` / `normalizedDescription` は DB カラム名ではなく、比較用の論理値として扱う

## 4. LIKE / ILIKE / ESCAPE の固定仕様

### 4.1 exact

- SQL: `LOWER(pvk.product_code) = #{keywordExactNormalized}`
- バインド: `keywordExactNormalized`
- ワイルドカード: 付与しない
- `%`, `_`, `\`: exact では使用しない
- `ESCAPE '\'`: 付与しない

### 4.2 prefix

- SQL: `LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'`
- バインド: `keywordPrefixPattern`
- ワイルドカードの付与位置: 末尾に `%` を付与
- 例:
  - `P0001` -> `keywordPrefixPattern = "p0001-%"`
  - `P0001-C` -> `keywordPrefixPattern = "p0001-c%"`
  - `P0001-C01` -> exact 条件のみで prefix 条件は不要
- `%`, `_`, `\`: 文字列の escape を行った後に `%` を付与する
- `ESCAPE '\'`: 必須

### 4.3 contains

- SQL:
  - `p.product_name ILIKE #{keywordContainsPattern} ESCAPE '\'`
  - `p.variation_name ILIKE #{keywordContainsPattern} ESCAPE '\'`
  - `p.description ILIKE #{keywordContainsPattern} ESCAPE '\'`
- バインド: `keywordContainsPattern`
- ワイルドカードの付与位置: 先頭と末尾に `%` を付与
- 例: `"desk"` -> `keywordContainsPattern = "%desk%"`
- `%`, `_`, `\` の escape ルール:
  - `%` -> `\%`
  - `_` -> `\_`
  - `\` -> `\\`
- `ESCAPE '\'`: 必須

### 4.4 escape ルールの具体例

| 文字 | 正規化前 | escape 後 | contains の最終値 |
|---|---|---|---|
| `%` | `%` | `\%` | `%\%%` 形式 |
| `_` | `_` | `\_` | `%\_%` 形式 |
| `\` | `\` | `\\` | `%\\%` 形式 |

実装では、contains 用に以下の順序で構築する。

```
input
→ trim
→ NFKC
→ lower
→ escape wildcard characters
→ prefix '%' + value + '%'
```

## 5. 実装対象の処理分担

### 5.1 `CatalogController`

- [src/main/java/jp/co/skig/officeorder/web/CatalogController.java](../../src/main/java/jp/co/skig/officeorder/web/CatalogController.java)
- `searchResults()` で `q` / `inStockOnly` / `priceBand` / `color` / `sort` / `page` / `size` を受け取る
- `deskTaste` / `chairTaste` / `storageTaste` は `searchResults()` では受け取らない
- `/categories/desks`, `/categories/chairs`, `/categories/storages` のみが `*Taste` を受け取る

### 5.2 `ProductListSearchService`

- [src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java)
- 画面入力値を `ProductSearchCondition` に変換する
- `normalizePriceBandIds()`, `normalizeColorKeys()` を使用して入力値を検証する
- `searchWithPageCorrection()` をそのまま利用する

### 5.3 `ProductService`

- [src/main/java/jp/co/skig/officeorder/service/product/ProductService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductService.java)
- `normalize(condition)` の中でキーワード正規化を実行する
- `keywordExactNormalized`, `keywordPrefixPattern`, `keywordContainsPattern` を生成する
- `buildCondition()` は `ProductCategoryFilter` を保持したまま `ProductRepository.search()` に渡す

### 5.4 `ProductFilterOptionService`

- [src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java](../../src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java)
- `buildDeskFilter()`, `buildChairFilter()`, `buildStorageFilter()` で有効な `tasteId` を抽出する
- 候補外の `tasteId` は破棄する
- `normalizePriceBandIds()` と `normalizeColorKeys()` は既存仕様を維持する

### 5.5 `ProductRepository`

- [src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java](../../src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java)
- `buildSearchParams()` で `Map<String, Object>` を生成する
- `countProducts` と `selectProducts` のパラメータを同じマップで伝える
- `search()` は `countProducts` で件数取得後、`selectProducts` を実行する

### 5.6 `ProductMapper.xml`

- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml)
- `BaseProductWhere` に keyword 条件をまとめる
- `countProducts` と `selectProducts` は同じ `BaseProductWhere` を共有する
- `keywordLike` という別名の既存キーは使わず、固定パラメータに置き換える

## 6. countProducts / selectProducts の完全一致

`countProducts` と `selectProducts` は以下の条件を同じ値で共有する。

### 6.1 共通WHERE条件

- `p.sale_start_at <= #{now}`
- `AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})`
- `categoryId` が指定されている場合 `AND p.category_id = #{categoryId}`
- keyword group:
  - exact OR prefix OR product_name contains OR variation_name contains OR description contains
- `colorIds` が存在する場合 `EXISTS (...) AND pvf.color_id IN (...)`
- `priceRanges` が存在する場合 `EXISTS (...)` または `pv.unit_price` に対する range condition
- `deskTasteIds` / `chairTasteIds` / `storageTasteIds` が存在する場合 `EXISTS (...) AND taste_id IN (...)`

### 6.2 keyword 条件の実SQL例

```sql
AND (
  LOWER(pvk.product_code) = #{keywordExactNormalized}
  OR LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'
  OR p.product_name ILIKE #{keywordContainsPattern} ESCAPE '\'
  OR p.variation_name ILIKE #{keywordContainsPattern} ESCAPE '\'
  OR p.description ILIKE #{keywordContainsPattern} ESCAPE '\'
)
```

### 6.3 color / price / taste の実SQL例

```sql
AND EXISTS (
  SELECT 1
  FROM product_variants pvf
  WHERE pvf.product_id = p.product_id
    AND pvf.color_id IN (?, ?, ?)
)
```

```sql
AND (
  pvf.unit_price >= ?
  AND pvf.unit_price <= ?
)
```

```sql
AND EXISTS (
  SELECT 1
  FROM product_desk_attributes da
  WHERE da.product_id = p.product_id
    AND da.taste_id IN (?, ?)
)
```

### 6.4 count/select の一致要件

- `countProducts` の `WHERE` と `selectProducts` の `WHERE` は完全一致とする
- `countProducts` は `SELECT COUNT(*)` だけが異なる
- `selectProducts` は `GROUP BY` / `ORDER BY` / `LIMIT` / `OFFSET` を付けるが、フィルタ条件は同一とする
- `keywordLike` を使って count と select で条件を分岐させない

## 7. ORDER BY の固定仕様

### 7.1 sort 条件

- `price_asc` -> `MIN(pv.unit_price) ASC, p.product_id ASC`
- `price_desc` -> `MIN(pv.unit_price) DESC, p.product_id ASC`
- `newest` -> `MAX(p.sale_start_at) DESC, p.product_id ASC`
- `default` -> `p.product_id ASC`

### 7.2 MyBatis `<choose>`

```xml
<choose>
  <when test="sort == 'price_asc'">
    MIN(pv.unit_price) ASC, p.product_id ASC
  </when>
  <when test="sort == 'price_desc'">
    MIN(pv.unit_price) DESC, p.product_id ASC
  </when>
  <when test="sort == 'newest'">
    MAX(p.sale_start_at) DESC, p.product_id ASC
  </when>
  <otherwise>
    p.product_id ASC
  </otherwise>
</choose>
```

### 7.3 禁止事項

- `CASE ... THEN ... DESC` を使わない
- `ORDER BY CASE` 形式を用いない
- `sort` が不正な場合は `default` にフォールバックする

## 8. taste API の固定仕様

### 8.1 `/products/search`

- `deskTaste`, `chairTaste`, `storageTaste` を受け取らない
- `deskTaste` / `chairTaste` / `storageTaste` は category endpoint のみを対象とする

### 8.2 category endpoint の仕様

- `/categories/desks` -> `deskTaste`
- `/categories/chairs` -> `chairTaste`
- `/categories/storages` -> `storageTaste`

### 8.3 全レイヤー整合

- Controller: `CatalogController.desks()`, `chairs()`, `storages()` が該当 param を受け取る
- Service: `ProductListSearchService.buildCondition(...)` と `ProductFilterOptionService.buildDeskFilter(...)` / `buildChairFilter(...)` / `buildStorageFilter(...)` が同じ param を扱う
- Repository: `ProductRepository.search()` が `ProductCategoryFilter` の `*TasteIds` を SQL に落とす
- Mapper: `EXISTS (...) AND taste_id IN (...)` を用いる
- Template: category page の checkbox/select が `name="deskTaste"` / `name="chairTaste"` / `name="storageTaste"` を使う
- Test: endpoint ごとの明示的テストを追加する

## 9. 実装順序（依存関係付き）

1. 正規化
   - `ProductService` で `trim()` / `NFKC` / `lower` を確定する
2. SearchCondition 構築
   - `ProductListSearchService.buildCondition()` と `ProductService.buildCondition()` で `ProductSearchCondition` を作る
3. Mapper 条件組み立て
   - `BaseProductWhere` に exact / prefix / contains を実装する
4. count/select 整合
   - `countProducts` と `selectProducts` に同じ `WHERE` を適用する
5. sort / pagination
   - `ORDER BY` と `LIMIT/OFFSET` を固定する
6. Controller
   - request parameter と `Model` への設定を正す
7. Template
   - query retention と hidden input / checkbox / select を実装する
8. unit test
   - 正規化・境界値・escape・taste・count/select・sort を検証する
9. integration/regression test
   - 全画面の回帰と既存 `page correction` を確認する

## 10. テストケース（期待結果付き）

| 入力 | 正規化後 | 利用される検索方式 | 期待SQL条件 | ヒットするデータ | ヒットしないデータ |
|---|---|---|---|---|---|
| `P0001` | `p0001` | `product_code prefix` | `LOWER(product_code) LIKE 'p0001-%' ESCAPE '\'` | `P0001-C01`, `P0001-C02` | `P00010-C01` |
| `P0001-C` | `p0001-c` | `product_code prefix` | `LOWER(product_code) LIKE 'p0001-c%' ESCAPE '\'` | `P0001-C01`, `P0001-C010` | `P00010-C01` |
| `P0001-C01` | `p0001-c01` | `product_code exact` | `LOWER(product_code) = 'p0001-c01'` | `P0001-C01` | `P0001-C010` |
| `P0001-C010` | `p0001-c010` | `product_code exact` | `LOWER(product_code) = 'p0001-c010'` | `P0001-C010` | `P0001-C01` |
| `P00010` | `p00010` | exact/prefix の対象外 | exact を試みてもしない; prefix にはしない | `P00010` を exact で明示検索した場合のみ | `P0001-C01` |
| `P0001XYZ` | `p0001xyz` | exact/prefix 非対象 | product_code search はしない | `product_name` 等に `P0001XYZ` を含む商品 | product_code prefix match |
| `P0001-` | `p0001-` | exact/prefix 非対象 | product_code search はしない | `product_name` 等に `P0001-` を含む商品 | product_code prefix match |
| `%` | `\%` | contains literal match | `ILIKE '%\%%' ESCAPE '\'` | `%` を含む商品名 | `P0001-C01` などの product_code |
| `_` | `\_` | contains literal match | `ILIKE '%\_%' ESCAPE '\'` | `_` を含む商品名 | `P0001-C01` などの product_code |
| `\` | `\\` | contains literal match | `ILIKE '%\\%' ESCAPE '\'` | `\` を含む商品名 | `P0001-C01` などの product_code |

### 10.1 重要テスト観点

- exact と prefix が同一の `keyword` で混ざらない
- contains は `product_name` / `variation_name` / `description` のみに使用する
- product_code contains は実装しない
- `%`, `_`, `\` は literal として扱う
- `P0001` と `P00010` は分離される
- `P0001-C` と `P0001-C01` は分離される
- `P0001-C010` が exact でのみヒットする

## 11. 実装完了条件（厳格）

以下をすべて満たした場合のみ実装完了とする。

- TODOに未確定事項がない
- `...` がない
- `#{...}.size` がない
- `CASE ... THEN ... DESC` を使っていない
- `product_code contains` を実装対象にしていない
- `countProducts` と `selectProducts` の条件が一致している
- taste仕様が Controller / Service / Repository / Mapper / Template / Test で一致している
- ORDER BY が PostgreSQL で有効な構造になっている
- 正規化順序が `trim() → NFKC → toLowerCase(Locale.ROOT)` に一致している
- escape仕様が `LIKE/ILIKE` の全箇所で一致している
- テストに入力/正規化/expected SQL/fixture/期待結果がある
- 設計書と TODO の間に矛盾がない

## 12. 実装開始前の最終チェック

### PASS / FAIL

- product_code exact: PASS
- product_code prefix: PASS
- product_code contains 非実装: PASS
- product_name / variation_name / description contains: PASS
- normalization order fixed: PASS
- null / blank handling fixed: PASS
- `%/_/\` の escape fixed: PASS
- exact/prefix/contains SQL fixed: PASS
- keyword OR logic fixed: PASS
- keyword AND other filters fixed: PASS
- count/select same condition: PASS
- ORDER BY valid: PASS
- taste API fixed: PASS
- new class policy fixed: PASS
- no ambiguous wording: PASS
- no `...` / no MyBatis pseudo SQL: PASS
- tests with expected results: PASS

結論: PASS

FEAT-001 は実装開始可能。追加の設計判断は不要。
