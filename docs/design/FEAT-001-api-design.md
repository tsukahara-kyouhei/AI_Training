# FEAT-001 API / Controller 設計

## 1. 目的

本設計では、商品検索機能の Controller と Request/Response を明確化する。主な対象は `CatalogController` と検索結果画面の遷移である。

## 2. 確認できた実装

### 2.1 既存確認済み

- `CatalogController.searchResults()` が `GET /products/search` を処理する
- `CatalogController.desks()`, `chairs()`, `storages()` がカテゴリ一覧を処理する
- `ProductListSearchService.buildCondition()` が検索条件を生成する
- `ProductService.search()` が `ProductListPage` を返す

### 2.2 今回設計

- API とは画面コントローラを指し、クライアントからの HTTP リクエストを `ProductSearchCondition` に変換する
- すべての応答はビュー返却と画面渲染を前提とする
- JSON API は本機能では追加しない

### 2.3 最終確定事項

- 現行実装では `@RestController` は使われず、検索機能は画面遷移主体の MVC endpoint である。
- リクエスト/レスポンスの実体は `ProductSearchCondition` と `Model` の画面データに限定する。

## 3. Endpoint 一覧

| Endpoint | Method | Purpose | Controller |
|---|---|---|---|
| /products/search | GET | 商品検索結果一覧 | CatalogController.searchResults |
| /categories/desks | GET | デスクカテゴリ一覧 | CatalogController.desks |
| /categories/chairs | GET | チェアカテゴリ一覧 | CatalogController.chairs |
| /categories/storages | GET | 収納家具カテゴリ一覧 | CatalogController.storages |
| /products/{productId} | GET | 商品詳細 | CatalogController.productDetail |

## 4. 検索結果 API の詳細

### 4.1 URL

- `/products/search`

### 4.2 Method

- `GET`

### 4.3 Request

| Parameter | Type | Required | Default | Notes |
|---|---|---|---|---|
| q | String | No | null | search keyword |
| inStockOnly | boolean | No | false | stock flag |
| priceBand | List<Integer> | No | [] | price filter |
| color | List<String> | No | [] | color code key |
| sort | String | No | recommended | ProductSort |
| page | int | No | 1 | positive int |
| size | int | No | 15 | 15/30/60 |

> FEAT-001 DESIGN の最終確定として、`deskTaste` / `chairTaste` / `storageTaste` は `/products/search` の Request Parameter では受け取らない。カテゴリ別一覧の endpoint のみで受け取る。

### 4.4 Validation

- `page >= 1`
- `size` is one of 15, 30, 60
- `sort` is in allowed values
- `priceBand` IDs are limited to `PriceBand.fromId()`
- `color` is checked against candidate list
- `*Taste` IDs are checked against candidate list

### 4.5 Service 呼び出し

```java
String q = request.getParameter("q");
List<Integer> priceBand = request.getParameterValues("priceBand") == null ? List.of() : Arrays.stream(request.getParameterValues("priceBand")).map(Integer::parseInt).toList();
List<String> color = request.getParameterValues("color") == null ? List.of() : List.of(request.getParameterValues("color"));
List<Integer> deskTaste = request.getParameterValues("deskTaste") == null ? List.of() : Arrays.stream(request.getParameterValues("deskTaste")).map(Integer::parseInt).toList();
List<Integer> chairTaste = request.getParameterValues("chairTaste") == null ? List.of() : Arrays.stream(request.getParameterValues("chairTaste")).map(Integer::parseInt).toList();
List<Integer> storageTaste = request.getParameterValues("storageTaste") == null ? List.of() : Arrays.stream(request.getParameterValues("storageTaste")).map(Integer::parseInt).toList();

ProductCategoryFilter filter = new ProductCategoryFilter(
    List.of(),
    deskTaste,
    chairTaste,
    storageTaste
);

ProductSearchCondition condition = productListSearchService.buildCondition(
    null,
    q,
    inStockOnly,
    priceBand,
    color,
    sort,
    page,
    size,
    filter
);
```

- `deskTaste` / `chairTaste` / `storageTaste` は画面入力時に `List<Integer>` として保持する
- `ProductCategoryFilter` はカテゴリ別の taste filter を表し、`ProductRepository.buildSearchParams()` の `deskTasteIds` / `chairTasteIds` / `storageTasteIds` にそのまま詰める
- 画面・Controller・Service・Repository・Mapper の各層で同一のパラメータ名と意味を使用する

### 4.6 Response

- `Model` に以下を格納
  - `products`
  - `totalCount`
  - `totalPages`
  - `currentPage`
  - `pageSize`
  - `keyword`
  - `selectedPriceBandIds`
  - `selectedColorKeys`
  - `inStockOnly`
  - `sortValue`

## 5. カテゴリ API の詳細

### 5.1 デスク

Request params:
- `inStockOnly`, `priceBand`, `color`, `deskTopShape`, `deskWidthBand`, `deskDepthBand`, `deskHeightBand`, `deskTaste`, `sort`, `page`, `size`

### 5.2 チェア

Request params:
- `inStockOnly`, `priceBand`, `color`, `chairFunction`, `chairMaterial`, `chairTaste`, `sort`, `page`, `size`

### 5.3 収納家具

Request params:
- `inStockOnly`, `priceBand`, `color`, `storageUsage`, `storageTaste`, `sort`, `page`, `size`

## 6. Error / Response Handling

- 不正パラメータは Service で許容候補範囲に制限して回避する
- 0件は通常の 200 OK で画面表示する
- DB エラーや想定外の例外は 500 で画面は一般エラー画面に遷移する

## 7. 画面と API の関係

- 本機能は MVC 画面のコントローラが主な API である
- REST JSON API ではないため、ResponseBody を返す endpoint は本件では定義しない

## 8. 確認ポイント

- `/products/search` の検索パラメータは画面内の `hidden input` と一致するように実装する
- モバイル用簡易検索フォームは既存の GET /products/search 形式に合わせて保持パラメータを維持する
