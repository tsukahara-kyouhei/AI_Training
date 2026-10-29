# FEAT-001 画面設計

## 1. 目的

本設計は、検索結果画面とヘッダ検索フォームの実装仕様を、現行の Spring + Thymeleaf 画面と合わせて定義する。

## 2. 確認できた現状

### 2.1 既存確認済み

- ヘッダフォーム: `src/main/resources/templates/fragments/layout/header.html`
  - `action="/products/search"`
  - `method="get"`
  - `name="q"`
- 検索結果画面: `src/main/resources/templates/pages/product-list-search-results.html`
  - `@{/products/search}` を submit する
  - `q`, `priceBand`, `color`, `inStockOnly`, `sort`, `size`, `page` を扱う
- カテゴリ別テンプレート (`desk`, `chair`, `storage`) ではすでに `*Taste` が存在する
  - `deskTaste`, `chairTaste`, `storageTaste`

### 2.2 今回設計

- 検索結果一覧画面に `taste` フィルタを追加する
- 検索条件を URL パラメータとして保持してページネーションを続行する
- 利用可能な値は既存カテゴリ一覧の候補を再利用する

### 2.3 最終確定事項

- モバイルの簡易フィルタ UI は既存の GET `/products/search` 形式に合わせて、選択条件を URL に保持して再描画する。
- `keyword` と 0 件時メッセージは既存テンプレートの文言に従い、追加の設計変更は行わない。

## 3. URL と Endpoint

### 3.1 ヘッダ検索

- URL: `/products/search`
- Method: `GET`
- Form: `header.html`
- Request parameter: `q`
- Required: false
- Default: empty string
- View: `pages/product-list-search-results`

### 3.2 検索結果一覧

- URL: `/products/search`
- Method: `GET`
- Controller: `CatalogController.searchResults()`
- Request params (現行実装の実体):
  - `q` : String, optional
  - `inStockOnly` : boolean, default false
  - `priceBand` : List<Integer>, optional
  - `color` : List<String>, optional
  - `sort` : String, optional
  - `page` : int, default 1
  - `size` : int, default 15

> FEAT-001 DESIGN の最終確定として、`deskTaste` / `chairTaste` / `storageTaste` は `/products/search` では受け取らない。カテゴリ別 endpoint のみで受け取る。これは現行コードとテンプレートの実体に合致する。

### 3.3 カテゴリ一覧の taste パラメータ

- `/categories/desks` : `deskTaste`
- `/categories/chairs` : `chairTaste`
- `/categories/storages` : `storageTaste`

同一画面テンプレートのパラメータ名は、カテゴリ別一覧でのみ保持する。

## 4. 検索結果画面仕様

### 4.1 レイアウト

- `layout-2col` 2 列レイアウト
- 左側: フィルタ
- 右側: 商品一覧
- 0 件時: `card` のメッセージ表示

### 4.2 表示要素

- 検索キーワード: `keyword`
- 件数: `totalCount`
- 商品一覧: `products`
- 並び順: `sortValue`
- 表示件数: `pageSize`
- 現在ページ: `currentPage`
- 総ページ数: `totalPages`
- フィルタ選択状態: `selectedPriceBandIds`, `selectedColorKeys`, `inStockOnly`

### 4.3 0件時動作

実装済みテンプレートでは以下を表示する。

- 「該当する商品は見つかりませんでした。」

この表示は要件と整合する。

## 5. フィルタ設計

### 5.1 既存フィルタ

- `inStockOnly` : checkbox
- `priceBand` : checkbox group (IDs 1〜6)
- `color` : color swatch checkbox group
- `sort` : select
- `size` : select

### 5.2 追加するフィルタ

- `deskTaste`
- `chairTaste`
- `storageTaste`

実装ルール:
- 既存カテゴリ一覧の候補をそのまま再利用する
- チェックボックス複数選択を許可する
- `ProductFilterOptionService` の候補一覧を使う
- 画面表示と検索条件で同一 ID を利用する

## 6. パラメータ設計

### 6.1 型と範囲

| Parameter | Type | Required | Default | Notes |
|---|---|---|---|---|
| q | String | No | null | keyword |
| inStockOnly | boolean | No | false | 1 or true is accepted by Spring |
| priceBand | List<Integer> | No | [] | from checkbox |
| color | List<String> | No | [] | allowed keys |
| sort | String | No | recommended | ProductSort.fromValue() |
| page | int | No | 1 | positive integer |
| size | int | No | 15 | 15/30/60 |
| deskTaste | List<Integer> | No | [] | category filter |
| chairTaste | List<Integer> | No | [] | category filter |
| storageTaste | List<Integer> | No | [] | category filter |

### 6.2 デフォルト値

- `page` = 1
- `size` = 15
- `inStockOnly` = false
- `sort` = recommended

### 6.3 入力制約

- `page` は 1 以上
- `size` は 15 / 30 / 60 のいずれか
- `sort` は `recommended, newest, price_asc, price_desc` のいずれか
- `priceBand` は `PriceBand` の ID のみ許容
- `color` は `colorOptions` に存在する `key` のみ許容
- `*Taste` は `*TasteOptions` に存在する ID のみ許容

## 7. 画面遷移とURL維持

### 7.1 検索実行

- ヘッダ検索から `q` を引き渡す
- 検索結果一覧で `sort`, `size`, `inStockOnly`, `priceBand`, `color` を維持して遷移する

### 7.2 ページネーション

テンプレート側の `th:href` は以下を保持する。

- `/products/search` では保持対象: `q`, `page`, `size`, `sort`, `inStockOnly`, `priceBand`, `color`
- `/categories/desks` では保持対象: `page`, `size`, `sort`, `inStockOnly`, `priceBand`, `color`, `deskTopShape`, `deskWidthBand`, `deskDepthBand`, `deskHeightBand`, `deskTaste`
- `/categories/chairs` では保持対象: `page`, `size`, `sort`, `inStockOnly`, `priceBand`, `color`, `chairFunction`, `chairMaterial`, `chairTaste`
- `/categories/storages` では保持対象: `page`, `size`, `sort`, `inStockOnly`, `priceBand`, `color`, `storageUsage`, `storageTaste`

実装上の原則:
- `page` だけ変更する
- 既存の URL パラメータを壊さない
- `q` / `inStockOnly` / `priceBand` / `color` / `sort` / `size` は `/products/search` の維持対象とする
- カテゴリ別の `*Taste` / `*Usage` / `*Material` / `*Shape` などはそのカテゴリ画面でのみ維持する

例: `/products/search?q=desk&inStockOnly=true&priceBand=2&priceBand=3&color=1&sort=price_asc&size=15&page=2`

### 7.3 URL 維持の最終確定事項

- `page` のみを変更し、他の条件はそのまま URL に残す
- `/products/search` では `q`, `inStockOnly`, `priceBand`, `color`, `sort`, `size` を保持対象とする
- カテゴリ別一覧ではカテゴリ固有パラメータを同時に保持する
- `page` 以外の条件が消失した場合は画面状態が破綻するため、各画面ごとに維持対象を明確化する
- FEAT-001 DESIGN では `deskTaste` / `chairTaste` / `storageTaste` を `searchResults()` 側に持たせない。カテゴリ endpoint のみが保持対象である

## 8. PC / モバイル対応

### 8.1 PC

- 左側サイドバーのフィルタ
- 右側一覧
- すべての条件を同時に表示

### 8.2 モバイル

- `modal-filters-search` を使った簡易 UI
- 既存の GET `/products/search` 形式をそのまま使い、選択した `deskTaste` / `chairTaste` / `storageTaste` / `priceBand` / `color` / `inStockOnly` を query param として保持する
- モバイル UI では検索条件の反映を含む画面遷移が本件の対象であり、今回の設計で「追加実装が必要」の状態を残さない
- つまり「モバイル UI の表示だけを実施し、DB 条件反映を次回に延期する」という扱いは設計上の対象外とする

## 9. 実装上の注意

- `header.html` の検索フォームはすでに GET `/products/search` と `q` を使っているので、設計の変更は最小限に留める
- `sort` と `page` は `ProductSort` と `ProductListSearchService.searchWithPageCorrection()` に合わせる
- テイスト追加はカテゴリ別テンプレートの既存パターンに合わせて `name="deskTaste"` 等を使う
- `ProductSearchCondition` と `ProductCategoryFilter` の正規化がまだ曖昧なため、画面側は値の許容範囲を厳格に保持する

## 10. 完了条件

以下が満たされれば画面設計完了とする。

- `GET /products/search` の入力値と動作が明確である
- `inStockOnly`, `priceBand`, `color`, `sort`, `page`, `size` の既存値が維持される
- `deskTaste` / `chairTaste` / `storageTaste` を追加できる
- ページネーションと条件保持が可能である
- 0 件時の表示が既存仕様と整合している
