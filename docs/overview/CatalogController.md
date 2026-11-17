## 1. 機能概要

- 新着商品やデスク・チェア・収納家具などの商品一覧を表示し、キーワード検索や在庫・価格・色・カテゴリ固有の条件で商品を探せる。
- 商品一覧を並べ替えたり、ページを切り替えたりして、目的の商品を見つけやすくする。
- 商品の詳細情報を表示し、ログイン中の会員にはお気に入り登録状況も確認できる。
- 最近見た商品を一覧表示し、閲覧した商品へ戻りやすくする。
- 会員がお気に入り商品の登録・解除を行えるようにし、未ログインの場合はログインへ案内する。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | リクエストパラメータ `inStockOnly` | 在庫のある商品のみに絞るかを指定する。省略時は `false`。 |
| 入力 | リクエストパラメータ `priceBand` | 商品一覧の価格帯絞り込み条件。複数指定可能。 |
| 入力 | リクエストパラメータ `color` | 商品一覧の色絞り込み条件。複数指定可能。 |
| 入力 | リクエストパラメータ `sort` | 商品一覧の並び順。省略時は機能に応じた既定順を使用する。 |
| 入力 | リクエストパラメータ `page`、`size` | 商品一覧のページ番号とページサイズ。既定値はそれぞれ `1`、`15`。 |
| 入力 | リクエストパラメータ `q` | 商品検索キーワード。検索結果一覧で使用する。省略可能。 |
| 入力 | リクエストパラメータ `deskTaste`、`chairTaste`、`storageTaste` | キーワード検索で使用する、デスク・チェア・収納家具のテイスト絞り込み条件。複数指定可能。 |
| 入力 | リクエストパラメータ `deskTopShape`、`deskWidthBand`、`deskDepthBand`、`deskHeightBand` | デスク一覧で使用する天板形状・幅・奥行・高さの絞り込み条件。複数指定可能。 |
| 入力 | リクエストパラメータ `chairFunction`、`chairMaterial` | チェア一覧で使用する機能・素材の絞り込み条件。複数指定可能。 |
| 入力 | リクエストパラメータ `storageUsage` | 収納家具一覧で使用する用途の絞り込み条件。複数指定可能。 |
| 入力 | URL パス変数 `productId` | 商品詳細やお気に入り操作の対象商品を識別する ID。 |
| 入力 | リクエストパラメータ `stock` | 商品詳細・お気に入り操作で在庫切れ表示を指定する値。`out` の場合は在庫切れ状態として表示する。 |
| 入力 | リクエストパラメータ `ids` | 最近見た商品の ID をカンマ区切りで指定する。正の数値の重複を除いた最大 4 件を対象とする。省略可能。 |
| 入力 | リクエストパラメータ `redirect` | お気に入り操作後の戻り先候補。安全な相対パスとして利用できない場合は商品詳細画面を戻り先とする。 |
| 入力 | リクエストパラメータ `action` | お気に入り操作の指定。`remove` は解除、それ以外は登録状態の切り替えとして扱う。省略時は `toggle`。 |
| 入力 | HTTP セッションのログイン会員情報 | 商品詳細のお気に入り状態表示、およびお気に入り操作の認証・会員識別に利用する。 |
| 出力 | 遷移先 `pages/product-list-new-arrivals` | 新着商品一覧画面。 |
| 出力 | 遷移先 `pages/product-list-search-results` | キーワード検索結果画面。 |
| 出力 | 遷移先 `pages/product-list-category-desk`、`pages/product-list-category-chair`、`pages/product-list-category-storage` | デスク、チェア、収納家具のカテゴリ別商品一覧画面。 |
| 出力 | 画面モデル `products`、`totalCount`、`currentPage`、`pageSize`、`totalPages` | 商品一覧と件数、現在ページ、ページサイズ、総ページ数。 |
| 出力 | 画面モデル `sortValue`、`inStockOnly`、`selectedPriceBandIds`、`selectedColorKeys` | 現在選択中の並び順・在庫・価格帯・色の条件。 |
| 出力 | 画面モデル `colorFilterOptions` | 商品一覧で選択できる色の絞り込み候補。 |
| 出力 | 画面モデル `keyword`、`deskTasteIds`、`chairTasteIds`、`storageTasteIds` | 検索結果画面で表示するキーワードと選択中のテイスト条件。 |
| 出力 | 画面モデル `deskTasteOptions`、`chairTasteOptions`、`storageTasteOptions` | キーワード検索で利用するカテゴリ別テイスト候補。 |
| 出力 | 画面モデル `deskTopShapeIds`、`deskWidthBandIds`、`deskDepthBandIds`、`deskHeightBandIds`、`deskTasteIds` | デスク一覧で選択中のカテゴリ固有条件。 |
| 出力 | 画面モデル `deskTopShapeOptions`、`deskTasteOptions` | デスク一覧の天板形状・テイスト絞り込み候補。 |
| 出力 | 画面モデル `chairFunctionIds`、`chairMaterialIds`、`chairTasteIds` | チェア一覧で選択中のカテゴリ固有条件。 |
| 出力 | 画面モデル `chairFunctionOptions`、`chairMaterialOptions`、`chairTasteOptions` | チェア一覧の機能・素材・テイスト絞り込み候補。 |
| 出力 | 画面モデル `storageUsageIds`、`storageTasteIds` | 収納家具一覧で選択中のカテゴリ固有条件。 |
| 出力 | 画面モデル `storageUsageOptions`、`storageTasteOptions` | 収納家具一覧の用途・テイスト絞り込み候補。 |
| 出力 | 遷移先 `pages/product-detail` | 商品詳細画面。画面モデル `detail`、`categoryLabel`、`isFavorite`、`detailPagePath` を渡す。 |
| 出力 | HTTP 404 | 指定された商品が存在しない場合に返す。 |
| 出力 | JSON 商品一覧 | 最近見た商品 ID に対応する商品カード一覧を返す。該当 ID がない場合は空の一覧。 |
| 出力 | 遷移先 `/login?redirect=...` | 未ログインでお気に入り操作を行った場合、操作後に戻るパスを付けてログイン画面へ案内する。 |
| 出力 | 遷移先 `redirect` または商品詳細画面 | ログイン済みの場合、お気に入り操作後に指定された安全な戻り先へ遷移する。 |
| 出力 | フラッシュ属性 `favoriteLimitError` | お気に入り登録数の上限に達した場合に表示するエラーメッセージ。 |

## 3. 処理フロー

### `newArrivals`（`GET /products/new-arrivals`）

1. `ProductFilterOptionService.loadOptionsBundle` を呼び出し、絞り込み候補を取得する。
2. 受け取った在庫・価格帯・色・並び順・ページ条件から、`ProductListSearchService.buildNewArrivalCondition` を呼び出して新着商品検索条件を作成する。
3. `ProductListSearchService.searchWithPageCorrection` を呼び出し、ページ番号を補正しながら商品を検索する。
4. `applyProductListModel` を呼び出し、商品一覧、ページ情報、選択条件、絞り込み候補を画面モデルに設定する。
5. `pages/product-list-new-arrivals` を返す。

### `searchResults`（`GET /products/search`）

1. `ProductFilterOptionService.loadOptionsBundle` を呼び出し、絞り込み候補を取得する。
2. キーワード、在庫・価格帯・色・カテゴリ別テイスト、並び順、ページ条件から検索条件を組み立てる。既定の並び順はおすすめ順とする。
3. `ProductListSearchService.searchWithPageCorrection` を呼び出して商品を検索し、ページ条件を補正する。
4. `applyProductListModel` を呼び出し、共通の商品一覧情報を画面モデルに設定する。
5. キーワード、選択中のテイスト条件、各カテゴリのテイスト候補を画面モデルに設定する。
6. `pages/product-list-search-results` を返す。

### `desks`（`GET /categories/desks`）

1. `ProductFilterOptionService.loadOptionsBundle` を呼び出し、絞り込み候補を取得する。
2. `ProductFilterOptionService.buildDeskFilter` を呼び出し、天板形状・幅・奥行・高さ・テイスト条件を組み立てる。
3. 共通条件とデスク固有条件から商品検索条件を作成し、既定の並び順はおすすめ順とする。
4. `ProductListSearchService.searchWithPageCorrection` を呼び出して商品を検索し、ページ条件を補正する。
5. `applyProductListModel` を呼び出して共通の商品一覧情報を設定し、デスク固有の選択条件と候補を画面モデルに設定する。
6. `pages/product-list-category-desk` を返す。

### `chairs`（`GET /categories/chairs`）

1. `ProductFilterOptionService.loadOptionsBundle` を呼び出し、絞り込み候補を取得する。
2. `ProductFilterOptionService.buildChairFilter` を呼び出し、機能・素材・テイスト条件を組み立てる。
3. 共通条件とチェア固有条件から商品検索条件を作成し、既定の並び順はおすすめ順とする。
4. `ProductListSearchService.searchWithPageCorrection` を呼び出して商品を検索し、ページ条件を補正する。
5. `applyProductListModel` を呼び出して共通の商品一覧情報を設定し、チェア固有の選択条件と候補を画面モデルに設定する。
6. `pages/product-list-category-chair` を返す。

### `storages`（`GET /categories/storages`）

1. `ProductFilterOptionService.loadOptionsBundle` を呼び出し、絞り込み候補を取得する。
2. `ProductFilterOptionService.buildStorageFilter` を呼び出し、用途・テイスト条件を組み立てる。
3. 共通条件と収納家具固有条件から商品検索条件を作成し、既定の並び順はおすすめ順とする。
4. `ProductListSearchService.searchWithPageCorrection` を呼び出して商品を検索し、ページ条件を補正する。
5. `applyProductListModel` を呼び出して共通の商品一覧情報を設定し、収納家具固有の選択条件と候補を画面モデルに設定する。
6. `pages/product-list-category-storage` を返す。

### `productDetail`（`GET /products/{productId}`）

1. `stock` が `out` の場合、在庫切れ商品として扱う指定を有効にする。
2. `ProductService.findDetail` を呼び出し、商品詳細を取得する。
   - 商品が見つからない場合：HTTP 404 を返す。
3. `MemberSessionService.currentMember` を呼び出し、ログイン中の会員を確認する。
   - ログイン中の場合：`MemberService.isFavorite` を呼び出し、対象商品のお気に入り登録状態を取得する。
   - 未ログインの場合：お気に入り状態を `false` とする。
4. 商品詳細、カテゴリ表示名、お気に入り状態、詳細ページのパスを画面モデルに設定する。
5. `pages/product-detail` を返す。

### `recentlyViewed`（`GET /products/recently-viewed`）

1. `parseRecentlyViewedIds` を呼び出し、ID の文字列を解析する。
   - 空・不正な値は除外し、正の数値の重複を除いた最大 4 件を対象とする。
2. 有効な商品 ID がない場合：空の JSON 配列を返す。
3. 有効な商品 ID がある場合：`ProductService.findRecentlyViewedProducts` を呼び出し、商品カード一覧を JSON で返す。

### `toggleFavorite`（`POST /products/{productId}/favorite`）

1. `stock` が `out` の場合は在庫切れ表示を有効にし、商品詳細画面の既定戻り先を作成する。
2. `MemberSessionService.sanitizeRedirectPath` を呼び出し、指定された戻り先を検証する。
   - 戻り先が利用できない場合：商品詳細画面を戻り先にする。
3. `ProductService.findDetail` を呼び出し、対象商品が存在することを確認する。
   - 商品が見つからない場合：HTTP 404 を返す。
4. `MemberSessionService.currentMember` を呼び出し、ログイン中の会員を確認する。
   - 未ログインの場合：ログを記録し、戻り先を URL エンコードして `/login?redirect=...` へリダイレクトする。
   - ログイン中の場合：次の処理を続ける。
5. `action` が `remove` の場合：`MemberService.removeFavorite` を呼び出してお気に入りを解除し、操作ログを記録して戻り先へリダイレクトする。
6. `action` が `remove` 以外の場合：`MemberService.toggleFavorite` を呼び出してお気に入り状態を切り替える。
   - 切り替えに成功した場合：操作ログを記録する。
   - `FavoritesLimitExceededException` が発生した場合：上限超過ログを記録し、例外メッセージをフラッシュ属性 `favoriteLimitError` に設定する。
7. 指定または既定の戻り先へリダイレクトする。
