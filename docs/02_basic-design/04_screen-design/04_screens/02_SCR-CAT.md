# 商品一覧

## 1. 基本情報

商品一覧は以下の5パターンで表示される。共通の構成を持ち、フィルター項目がカテゴリ別に異なる。

| パターン | URL | テンプレート | Controllerメソッド |
|---|---|---|---|
| 新着商品 | `GET /products/new-arrivals` | `product-list-new-arrivals.html` | `CatalogController#newArrivals()` |
| デスク | `GET /categories/desks` | `product-list-category-desk.html` | `CatalogController#desks()` |
| チェア | `GET /categories/chairs` | `product-list-category-chair.html` | `CatalogController#chairs()` |
| 収納家具 | `GET /categories/storages` | `product-list-category-storage.html` | `CatalogController#storages()` |
| 検索結果 | `GET /products/search` | `product-list-search-results.html` | `CatalogController#searchResults()` |

認証: 不要

---

## 2. 画面構成

| エリア | 内容 |
|---|---|
| フィルターパネル | 絞り込み条件（カラー・価格帯・在庫・カテゴリ専用フィルター） |
| ソート・表示件数 | セレクトボックス（最新順/価格順等、表示件数） |
| 件数表示 | 「XX件の商品が見つかりました」 |
| 商品カードグリッド | `productCardByView` フラグメント |
| ページネーション | 前へ/次へ/ページ番号 |

---

## 3. 共通Model変数

| 変数名 | 型 | 説明 |
|---|---|---|
| `totalCount` | `long` | 検索ヒット件数 |
| `products` | `List<ProductCardView>` | 商品カードリスト |
| `colorFilterOptions` | `List<ColorFilterOption>` | カラー絞り込み選択肢 |
| `selectedColorKeys` | `List<String>` | 選択済みカラー |
| `selectedPriceBandIds` | `List<String>` | 選択済み価格帯 |
| `inStockOnly` | `boolean` | 在庫ありのみフラグ |
| `sortValue` | `String` | 現在のソート順 |
| `pageSize` | `int` | 1ページあたり件数 |
| `currentPage` | `int` | 現在ページ |
| `totalPages` | `int` | 総ページ数 |
| `deskTasteOptions` | `List<CategoryFilterOption>` | デスクテイスト選択肢（検索結果ページのみ） |
| `chairTasteOptions` | `List<CategoryFilterOption>` | チェアテイスト選択肢（検索結果ページのみ） |
| `storageTasteOptions` | `List<CategoryFilterOption>` | 収納テイスト選択肢（検索結果ページのみ） |
| `selectedDeskTasteIds` | `List<Integer>` | 選択済みデスクテイストID（検索結果ページのみ） |
| `selectedChairTasteIds` | `List<Integer>` | 選択済みチェアテイストID（検索結果ページのみ） |
| `selectedStorageTasteIds` | `List<Integer>` | 選択済み収納テイストID（検索結果ページのみ） |

---

## 4. カテゴリ専用フィルター

### デスク

| パラメータ | 説明 |
|---|---|
| `deskTopShape` | 天板形状（複数選択） |
| `deskWidthBand` | 幅範囲（mm、11段階） |
| `deskDepthBand` | 奥行範囲（mm、5段階） |
| `deskTaste` | テイスト |

### チェア

| パラメータ | 説明 |
|---|---|
| `chairFunction` | 機能（肘付き等） |
| `chairMaterial` | 素材（メッシュ等） |
| `chairTaste` | テイスト |

### 収納家具

| パラメータ | 説明 |
|---|---|
| `storageUsage` | 用途 |
| `storageTaste` | テイスト |

### 検索結果

キーワード検索結果ページ（`/products/search`）では、カテゴリ横断でテイスト絞込みを提供する。

| パラメータ | 説明 |
|---|---|
| `deskTaste` | デスクのテイスト |
| `chairTaste` | チェアのテイスト |
| `storageTaste` | 収納家具のテイスト |

- 3カテゴリのテイストを `<details>` / `<summary>` によるアコーディオン（折りたたみ）形式で表示
- 各テイストはチェックボックスで複数選択可
- 初期状態: 折りたたみ（closed）
- テイストが1つでも選択されると、該当テイスト属性を持つ商品のみに絞り込まれる
- モバイル表示時も同じアコーディオン形式をレスポンシブで表示する（フィルターパネル自体の表示/非表示は既存のモバイルメニュー制御に従う）

---

## 5. フォーム

各テンプレート共通GETフォーム: `method="get"`

| フォーム要素 | name | 型 | 説明 |
|---|---|---|---|
| 在庫ありのみ | `inStockOnly` | boolean | チェックボックス |
| 価格帯 | `priceBand` | String[] | 複数選択チェック |
| カラー | `color` | String[] | カラースウォッチ |
| ソート | `sort` | String | セレクトボックス |
| 表示件数 | `size` | int | セレクトボックス |
| ページ | `page` | int | ページネーション |
| 検索キーワード | `q` | String | 検索結果ページのみ（hidden） |
| デスクテイスト | `deskTaste` | String[] | 検索結果ページのみ（複数選択チェックボックス） |
| チェアテイスト | `chairTaste` | String[] | 検索結果ページのみ（複数選択チェックボックス） |
| 収納テイスト | `storageTaste` | String[] | 検索結果ページのみ（複数選択チェックボックス） |
