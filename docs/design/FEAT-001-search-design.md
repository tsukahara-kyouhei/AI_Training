# FEAT-001 検索ロジック設計

## 1. 目的

本設計は、要件定義書の検索仕様を実装可能なレベルまで詳細化し、実際の Spring MVC + MyBatis の責務分担と一致するようにする。

## 2. 確認できた実装と設計判定

### 2.1 既存確認済み

- Controller は `CatalogController.searchResults()` で `GET /products/search` を処理している
- `ProductListSearchService.buildCondition()` と `ProductService.buildCondition()` で検索条件が生成される
- `ProductRepository.search()` が SQL パラメータを構築し、`ProductMapper.countProducts()` と `ProductMapper.selectProducts()` を呼ぶ
- `ProductMapper.xml` の `BaseProductWhere` で in-stock, color, price, category attribute 条件が生成されている
- `deskTaste` / `chairTaste` / `storageTaste` は `ProductCategoryFilter` と `ProductFilterOptionService` で扱われている

### 2.2 今回設計

- キーワード条件を「商品コード完全一致/前方一致 OR 商品名/バリエーション/説明文部分一致」の形で明確化する
- 商品コード条件と部分一致条件を分離し、SQL の判断方式と Java の判定方式を分ける
- 既存の `ILIKE` 使用を前提にしつつ、要件の境界条件に合わせて正規化比較を適用する

### 2.3 分類: EXISTING / FEAT-001 DESIGN / UNRESOLVED

- EXISTING
  - 現行実装の `ProductRepository.buildSearchParams()` は `keywordLike` を生成している
  - 現行マッパー `BaseProductWhere` は `p.product_name ILIKE #{keywordLike}` と `pvk.product_code ILIKE #{keywordLike}` を使う contains 検索を実行している
  - `CatalogController.searchResults()` は `q`, `inStockOnly`, `priceBand`, `color`, `sort`, `page`, `size` を受け取る
  - `deskTaste` / `chairTaste` / `storageTaste` は現行 `searchResults()` では受け取らず、カテゴリ別 endpoint でのみ使用される
- FEAT-001 DESIGN
  - FEAT-001 では `product_code` を `exact` または `prefix` で比較する
  - `keyword` は `keywordExactNormalized` / `keywordPrefixPattern` / `keywordContainsPattern` の 3 つに分離して処理する
  - `exact` と `prefix` は `product_name` / `variation_name` / `description` の contains とは別系統の判定とする
- UNRESOLVED
  - なし。すべての決定事項を以下の節で具体化する

### 2.4 最終確定事項

- Java 正規化は `trim()` → `Normalizer.normalize(..., Form.NFKC)` → `toLowerCase(Locale.ROOT)` を実施する
- `product_code` の比較は「完全一致または前方一致」のみとし、`LIKE` / `ILIKE` の中間一致や後方一致は使用しない
- PostgreSQL の `LIKE` / `ILIKE` で `%` / `_` / `\` を含むデータを安全に扱うには `ESCAPE '\'` を用いる
- `normalizedProductName` / `normalizedVariationName` / `normalizedDescription` は SQL カラム名ではなく、論理比較値を表す設計上の概念である

## 3. 全体フロー

1. `CatalogController.searchResults()` が GET パラメータを受け取る
2. `ProductListSearchService.buildCondition()` が raw 値を正規化し、`ProductSearchCondition` を生成する
3. `ProductService.buildCondition()` が `ProductSearchCondition` を最終的な形に正規化し、`repository.search()` に渡す
4. `ProductRepository.search()` が `Map<String, Object>` を作成し、`countProducts` と `selectProducts` を呼ぶ
5. `ProductMapper.xml` で `WHERE` 条件を構成し、`LIMIT/OFFSET` と `ORDER BY` を適用して結果を返す
6. 画面は `ProductListPage` を受け取り、`product-list-search-results.html` に描画する

## 4. 層別責務

### 4.1 Controller

責務:
- URL の受け取り
- パラメータのバインド
- デフォルト値の補完
- 画面モデルへの格納

対象:
- `CatalogController.searchResults()`
- `CatalogController.desks()`
- `CatalogController.chairs()`
- `CatalogController.storages()`

### 4.2 Service

責務:
- raw 値の validation と許容値フィルタ
- `ProductSearchCondition` の生成
- `ProductCategoryFilter` の生成
- 0件/ページ境界の補正

対象:
- `ProductListSearchService`
- `ProductFilterOptionService`
- `ProductService`

### 4.3 Repository

責務:
- SQL パラメータマップ生成
- `countProducts()` / `selectProducts()` 呼出し
- バッチで色コードを取得して product card に整形

対象:
- `ProductRepository`

### 4.4 Mapper / SQL

責務:
- `WHERE`, `JOIN`, `GROUP BY`, `ORDER BY`, `LIMIT`, `OFFSET` の構築
- テイスト/価格帯/カラー/在庫条件の組み立て
- 商品一覧件数取得

対象:
- `ProductMapper`
- `ProductMapper.xml`

## 5. 検索条件オブジェクト

### 5.1 ProductSearchCondition

現行ソース上の定義を基準とする。

- `categoryId: String`
- `keyword: String`
- `inStockOnly: boolean`
- `priceBands: List<PriceBand>`
- `colorIds: List<Long>`
- `categoryFilter: ProductCategoryFilter`
- `sort: ProductSort`
- `page: int`
- `size: int`
- `saleStartFrom: OffsetDateTime`

実装方針:
- 画面入力値は `ProductListSearchService` で正規化する
- `ProductService` は `normalize()` を通して canonical form にする

### 5.2 ProductCategoryFilter

現在実装済みの条件は以下。

- `deskTopShapeIds`
- `deskWidthBandIds`
- `deskDepthBandIds`
- `deskHeightBandIds`
- `deskTasteIds`
- `chairFunctionIds`
- `chairMaterialIds`
- `chairTasteIds`
- `storageUsageIds`
- `storageTasteIds`

これを踏襲し、以下の設計を適用する。

- 同一カテゴリ属性の複数選択は OR
- 異なる属性種別は AND
- テイストはカテゴリごとの `*TasteIds` で扱う

## 6. 正規化設計

### 6.1 要件で確定していること

- 英字は大文字小文字を無視して比較する
- 数字、英字、カタカナの全角/半角差を無視する
- 前後空白を削除する
- キーワードが空文字または空白のみの場合は検索対象に含めない

### 6.2 既存実装で確認できたこと

- `ProductService.buildCondition()` は `keyword.trim()` を行っている
- `ProductFilterOptionService.normalizeColorKeys()` は候補値の許容一覧を使って入力値を制限している
- `ProductCategoryFilter.normalize()` は null 除去と重複除去を行う

### 6.3 最終確定した正規化規則

- `NULL` と空文字の扱いは以下で統一する
  - `null` は未入力とみなす
  - 空白のみは `trim()` 後に空文字扱い
  - 空文字はキーワード条件として無効
- 文字列の比較前には Java で次の正規化を行う
  1. `trim()`
  2. `Normalizer.normalize(..., Form.NFKC)`
  3. `toLowerCase(Locale.ROOT)`
- 長音・濁点・半濁点や記号の表記ゆれは本実装では「同一視の対象としない」。要件の「同一視」要件は英字・数字・全角/半角差に関する範囲に限定し、実データの記号や濁点表記の差異は現時点では対象外とする。

### 6.4 正規化規則

`normalizeForSearch(input)` を以下の順で適用する。

1. `input == null` → `null`
2. `trim()`
3. `isBlank()` → `null`
4. `Normalizer.normalize(value, Form.NFKC)`
5. `toLowerCase(Locale.ROOT)`
6. SQL では `ILIKE` / `LIKE` で比較する前に、`%` / `_` / `\` をエスケープしてワイルドカードをリテラル化する

### 6.5 比較ロジック

- `productCodeExact`: normalized keyword == normalized product code
- `productCodePrefix`: normalized product code startsWith(normalized keyword)
- `nameContains`: normalized product name contains normalized keyword
- `variationContains`: normalized variation name contains normalized keyword
- `descriptionContains`: normalized description contains normalized keyword

## 7. キーワード検索論理

### 7.1 ルール

- 商品コード検索は完全一致または前方一致のみ
- 商品名、バリエーション名、説明文は部分一致
- 4 項目の条件は OR で結合する
- キーワードと絞込条件は AND で結合する

### 7.2 判定式

`keywordMatch = productCodeExact OR productCodePrefix OR productNameContains OR variationNameContains OR descriptionContains`

`result = keywordMatch AND priceFilter AND colorFilter AND stockFilter AND tasteFilter`

### 7.3 キーワード未入力時

- `keyword == null` または `trim().isEmpty()` のとき、`keywordMatch` を適用しない
- 価格帯、カラー、在庫、有効なカテゴリ属性条件のみで検索する

## 8. 絞込条件の論理

### 8.1 同一種別の複数選択

- `priceBand` の複数選択: OR
- `color` の複数選択: OR
- `deskTaste` の複数選択: OR
- `chairTaste` の複数選択: OR
- `storageTaste` の複数選択: OR

実装上は `IN (...)` 条件で表現する。

### 8.2 異なる種別間

- 価格帯 AND カラー AND 在庫 AND テイスト
- ソートはデータ取得時の順序のみで、検索条件ではない

## 9. SQL 設計

### 9.1 再確認できた実装パターン

現行の `ProductMapper.xml` では以下の構造が使われている。

- `FROM products p`
- `JOIN product_variants pv ON pv.product_id = p.product_id`
- `WHERE p.sale_start_at <= #{now}`
- `AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})`
- `AND EXISTS (...)` でカテゴリごとの属性条件を过滤
- `GROUP BY p.product_id, p.product_name`
- `ORDER BY` で sort を解釈
- `LIMIT #{limit} OFFSET #{offset}`
- `COUNT(*)` で total count を取得

### 9.2 実装対象 SQL

以下は、`countProducts()` と `selectProducts()` の最終確定仕様である。`product_code` の prefix は「`P0001` を入力したとき `P0001-C01` / `P0001-C02` をヒットさせ、`P00010-C01` をヒットさせない」という境界条件を採用する。これは実データ `P0001-C01` / `P0001-C02` / `P00010-C01` の構造から、`P0001-` を境界とする前方一致が正しいと判断したためである。

#### 9.2.1 countProducts

```sql
SELECT COUNT(*)
FROM products p
WHERE p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
  AND (
    #{keyword} IS NULL
    OR #{keyword} = ''
    OR (
      p.product_name ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR p.variation_name ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR p.description ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) = #{keywordExactNormalized}
      )
      OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'
      )
    )
  )
  AND (
    #{categoryId} IS NULL
    OR #{categoryId} = ''
    OR p.category_id = #{categoryId}
  )
  AND (
    NOT #{inStockOnly}
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND pvf.stock_quantity > 0
    )
  )
  AND (
    #{colorIds} IS NULL
    OR #{colorIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND pvf.color_id IN
        <foreach collection="colorIds" item="colorId" open="(" close=")" separator=",">
          #{colorId}
        </foreach>
    )
  )
  AND (
    #{priceRanges} IS NULL
    OR #{priceRanges}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND (
          <foreach collection="priceRanges" item="range" separator=" OR ">
            (pvf.unit_price >= #{range.minInclusive} AND pvf.unit_price <= #{range.maxInclusive})
          </foreach>
        )
    )
  )
  AND (
    #{deskTasteIds} IS NULL
    OR #{deskTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_desk_attributes da
      WHERE da.product_id = p.product_id
        AND da.taste_id IN
        <foreach collection="deskTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
  AND (
    #{chairTasteIds} IS NULL
    OR #{chairTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_chair_attributes ca
      WHERE ca.product_id = p.product_id
        AND ca.taste_id IN
        <foreach collection="chairTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
  AND (
    #{storageTasteIds} IS NULL
    OR #{storageTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_storage_attributes sa
      WHERE sa.product_id = p.product_id
        AND sa.taste_id IN
        <foreach collection="storageTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
```

#### 9.2.2 selectProducts

```sql
SELECT
  p.product_id,
  p.product_name,
  MIN(pv.unit_price) AS min_price,
  MAX(pv.stock_quantity) AS max_stock,
  MIN(pv.product_code) AS product_code
FROM products p
JOIN product_variants pv ON pv.product_id = p.product_id
WHERE p.sale_start_at <= #{now}
  AND (p.sale_end_at IS NULL OR p.sale_end_at > #{now})
  AND (
    #{keyword} IS NULL
    OR #{keyword} = ''
    OR (
      p.product_name ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR p.variation_name ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR p.description ILIKE #{keywordContainsPattern} ESCAPE '\'
      OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) = #{keywordExactNormalized}
      )
      OR EXISTS (
        SELECT 1
        FROM product_variants pvk
        WHERE pvk.product_id = p.product_id
          AND LOWER(pvk.product_code) LIKE #{keywordPrefixPattern} ESCAPE '\'
      )
    )
  )
  AND (
    #{categoryId} IS NULL
    OR #{categoryId} = ''
    OR p.category_id = #{categoryId}
  )
  AND (
    NOT #{inStockOnly}
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND pvf.stock_quantity > 0
    )
  )
  AND (
    #{colorIds} IS NULL
    OR #{colorIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND pvf.color_id IN
        <foreach collection="colorIds" item="colorId" open="(" close=")" separator=",">
          #{colorId}
        </foreach>
    )
  )
  AND (
    #{priceRanges} IS NULL
    OR #{priceRanges}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_variants pvf
      WHERE pvf.product_id = p.product_id
        AND (
          <foreach collection="priceRanges" item="range" separator=" OR ">
            (pvf.unit_price >= #{range.minInclusive} AND pvf.unit_price <= #{range.maxInclusive})
          </foreach>
        )
    )
  )
  AND (
    #{deskTasteIds} IS NULL
    OR #{deskTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_desk_attributes da
      WHERE da.product_id = p.product_id
        AND da.taste_id IN
        <foreach collection="deskTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
  AND (
    #{chairTasteIds} IS NULL
    OR #{chairTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_chair_attributes ca
      WHERE ca.product_id = p.product_id
        AND ca.taste_id IN
        <foreach collection="chairTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
  AND (
    #{storageTasteIds} IS NULL
    OR #{storageTasteIds}.size = 0
    OR EXISTS (
      SELECT 1
      FROM product_storage_attributes sa
      WHERE sa.product_id = p.product_id
        AND sa.taste_id IN
        <foreach collection="storageTasteIds" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
    )
  )
GROUP BY p.product_id, p.product_name
ORDER BY
  CASE #{sort}
    WHEN 'price_asc' THEN MIN(pv.unit_price)
    WHEN 'price_desc' THEN MIN(pv.unit_price) DESC
    WHEN 'newest' THEN MAX(p.sale_start_at) DESC
    ELSE p.product_id
  END,
  p.product_id ASC
LIMIT #{limit} OFFSET #{offset}
```

### 9.3 `LIKE` と `ILIKE`

- PostgreSQL の `ILIKE` は大文字小文字を比較しないため、`product_name` / `variation_name` / `description` の部分一致には使用する
- `product_code` は exact または prefix のみを対象とし、中間一致や後方一致は対象外とする
- prefix の境界条件は `P0001` を入力した場合 `P0001-C01` / `P0001-C02` を hit し、`P00010-C01` は hit しない という人間が迷わない明示的ルールにする
- このルールは `P0001%` ではなく `P0001-%` / `P0001-C%` を境界とする前方一致として実装する

#### 9.3.1 product_code prefix 境界表

| 入力 | 形式 | prefix対象 | exact対象 | 生成パターン | 期待結果 |
|---|---|---:|---:|---|---|
| `P0001` | base code | Yes | No | `p0001-%` | `P0001-C01` / `P0001-C02` hit, `P00010-C01` miss |
| `P0001-C` | partial suffix | Yes | No | `p0001-c%` | `P0001-C01` hit, `P0001-C010` hit |
| `P0001-C01` | full code | No | Yes | `p0001-c01` | exact hit |
| `P0001-C010` | longer suffix | No | No | `p0001-c010%` では prefix 対象外とする | `P0001-C010` の exact のみ扱う |
| `P00010` | 5 digits | No | No | `p00010%` を prefix にしない | miss |
| `P0001XYZ` | invalid code suffix | No | No | prefix 対象外 | miss |
| `P0001-` | trailing hyphen | No | No | prefix 対象外 | miss |
| `%` / `_` / `\` | wildcard literal | No | No | escape 後に文字列として扱う | literal match |

- prefix は `P0001` / `P0001-C` / `P0001-C0` など、実際の product_code の「カテゴリコード境界」を越えない範囲でのみ許可する
- `P00010` は `P0001` を前置に持つが、`P00010` 自体は別のベースコードであるため prefix 系として扱わない
- `P0001XYZ` は `-` 区切りを伴わない不正な形式のため prefix 対象外とする

#### 9.3.2 SQL 仕様

- exact: `LOWER(product_code) = :keywordExactNormalized`
- prefix: `LOWER(product_code) LIKE :keywordPrefixPattern ESCAPE '\'`
- contains: `p.product_name ILIKE :keywordContainsPattern ESCAPE '\'` または `p.variation_name ILIKE ...` / `p.description ILIKE ...`
- `keywordPrefixPattern` は `escapedNormalizedKeyword + '%'` とし、`%` / `_` / `\` は `escape` されてから付与する

### 9.4 Java / SQL のエスケープ仕様

| 文字 | Java 正規化前 | Java エスケープ後 | SQL での扱い |
|---|---|---|---|
| `\` | `\` | `\\` | リテラルとして扱う |
| `%` | `%` | `\%` | ワイルドカードではなくリテラル |
| `_` | `_` | `\_` | ワイルドカードではなくリテラル |

- `ESCAPE '\'` は `LIKE` / `ILIKE` を使用するすべての検索条件に付与する
- 生成順序は `trim` → `NFKC` → `lower` → escape → `LIKE` / `ILIKE` で比較する

### 9.5 キーワード生成パイプライン

1. 入力（例: `P0001`, `ｐ０００１－ｃ０１`, `office`, `  wood%_  `）
2. Java で `trim()` を適用
3. Java で `Normalizer.normalize(..., Form.NFKC)` を適用して全角・半角差を吸収
4. Java で `toLowerCase(Locale.ROOT)` を適用
5. `exact` はそのまま `keywordExactNormalized` とし、`LOWER(product_code) = :keywordExactNormalized` で比較
6. `prefix` は `P0001` → `p0001-` / `P0001-C` → `p0001-c` / `P0001-C01` → `p0001-c01` を境界として生成し、`keywordPrefixPattern = escapedPrefix + '%'` とする
7. `contains` は `%` / `_` / `\` をエスケープして `keywordContainsPattern = '%' + escapedKeyword + '%'` を作成し、`ILIKE ... ESCAPE '\'` で比較

### 9.6 MyBatis XML と PostgreSQL SQL の分離

#### A. Logical Search Specification
- `keywordMatch = productCodeExact OR productCodePrefix OR productNameContains OR variationNameContains OR descriptionContains`

#### B. MyBatis XML Specification
```xml
<if test="colorIds != null and colorIds.size() > 0">
  AND pvf.color_id IN
  <foreach collection="colorIds" item="colorId" open="(" close=")" separator=",">
    #{colorId}
  </foreach>
</if>
```

#### C. PostgreSQL Representative SQL
```sql
AND EXISTS (
  SELECT 1
  FROM product_variants pvf
  WHERE pvf.product_id = p.product_id
    AND pvf.color_id IN (?, ?, ?)
)
```

- MyBatis の `#{...}` / `<if>` / `<foreach>` と PostgreSQL の実 SQL は別レイヤーとする
- 設計書の SQL は実行可能な SQL の例として記載し、MyBatis の制御用記法は XML 節で明記する

### 9.7 SQL インジェクション対策

- すべての検索値は `#{}` でバインドする
- フィルタ値は `IN (...)` または `EXISTS (...)` で安全に表現する
- `sort` は `ProductSort.fromValue()` で許可値に制限する
- `page` / `size` は正数かつ許容範囲内に限定する
- `%` / `_` / `\` には Java 側エスケープを行い、その後 SQL では `ESCAPE '\'` を固定使用する

## 10. メソッド詳細

### 10.1 `CatalogController.searchResults()`

入力:
- `q: String` (optional)
- `inStockOnly: boolean`
- `priceBand: List<Integer>`
- `color: List<String>`
- `sort: String`
- `page: int`
- `size: int`

出力:
- `Model` に検索結果を格納
- view `pages/product-list-search-results`

### 10.2 `ProductListSearchService.buildCondition()`

入力:
- categoryId, keyword, inStockOnly, rawPriceBandIds, rawColorKeys, sort, page, size

出力:
- `ProductSearchCondition`

処理:
- `normalizePriceBandIds()`
- `normalizeColorKeys()`
- `resolveColorIds()`
- `ProductService.buildCondition()` に委譲

### 10.3 `ProductService.search()`

入力:
- `ProductSearchCondition`

出力:
- `ProductListPage`

処理:
- `normalize(condition)`
- `repository.search(normalizedCondition)`

### 10.4 `ProductRepository.search()`

入力:
- `ProductSearchCondition`

出力:
- `ProductListPage`

処理:
- `buildSearchParams()`
- `countProducts(params)`
- `selectProducts(params)`
- 色コードをバッチ取得
- 表示用 `ProductCardView` に変換

## 11. 0件時挙動

- `countProducts()` が 0 の場合、`ProductListPage` の items は empty とし、画面側の `if products == null or empty` で「該当する商品は見つかりませんでした。」を表示する
- 既存テンプレートと整合させること
- エラー画面遷移は行わない

## 12. ページングとソート

### 12.1 ページング

- `page` のデフォルトは `1`
- `size` のデフォルトは `15`
- `ProductListSearchService.searchWithPageCorrection()` がページ超過時に補正する

### 12.2 ソート

既存実装の `ProductSort` は以下。

- `recommended`
- `newest`
- `price_asc`
- `price_desc`

これを踏襲し、検索条件そのものではなく `ORDER BY` の種別として使用する。

## 13. テイスト検索設計

### 13.1 実際のパラメータ

- `deskTaste`
- `chairTaste`
- `storageTaste`

### 13.2 Controller

- `CatalogController.desks()` で `@RequestParam(name = "deskTaste", required = false) List<Integer> rawDeskTasteIds`
- `CatalogController.chairs()` で `@RequestParam(name = "chairTaste", required = false) List<Integer> rawChairTasteIds`
- `CatalogController.storages()` で `@RequestParam(name = "storageTaste", required = false) List<Integer> rawStorageTasteIds`

### 13.3 Service

- `ProductFilterOptionService.buildDeskFilter(...)`
- `ProductFilterOptionService.buildChairFilter(...)`
- `ProductFilterOptionService.buildStorageFilter(...)`

### 13.4 SQL

- `ProductMapper.xml` の `DeskAttributeFilter`, `ChairAttributeFilter`, `StorageAttributeFilter` を基準とする
- `taste_id IN (...)` で OR 条件を作る
- その条件を `EXISTS (...)` の中に入れて `categoryId` に適用する

### 13.5 最終確定事項

- `taste_id` は各カテゴリマスタの主キーであり、`desk_tastes.taste_id` と `chair_tastes.taste_id` は同一の数値を持っていても別インスタンスの識別子である。
- したがって `ProductCategoryFilter.deskTasteIds` / `chairTasteIds` / `storageTasteIds` は独立したカテゴリ別条件として扱い、相互に共通のグローバル ID ではない。
- 実データでは `product_variants.product_code` は `P0001-C01` のような ASCII 形式で保存されており、全角英数字や全角ハイフンを含む値はこの実装では対象にしない。

## 14. 実装上の確定方針

- `product_variants.product_code` の実データは `P0001-C01` のような形式で、完全一致/前方一致の比較対象として安全に扱える。
- 正規化は英字大文字小文字と全角/半角の同一視に限定し、本実装では濁点・長音・記号の自動正規化までは行わない。
- `LIKE` / `ILIKE` の特殊文字エスケープは PostgreSQL の `ESCAPE '\'` と `%` / `_` / `\` の対策を必須とする。
- 画面・Controller・Service・Repository・Mapper・SQL の各層で、`keyword` は `keywordExactNormalized` / `keywordPrefixPattern` / `keywordContainsPattern` に分離して処理する。

## 15. 完了条件

以下が満たされるとき、本設計は実装可能状態とする。

- Controller → Service → Repository → Mapper の責務が明確である
- キーワード条件と絞込条件の AND/OR が明文化されている
- 商品コードと部分一致の判定が分離されている
- テイスト条件が画面・Controller・Service・SQLで一貫している
- 実装者が追加の判断なしで条件生成と SQL 構造を作れる
