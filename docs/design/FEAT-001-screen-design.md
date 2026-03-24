# FEAT-001 画面設計書

## 1. 対象画面

| 画面ID | 画面名 | URL | テンプレート |
|-------|-------|-----|-----------|
| SRCH002 | キーワード検索結果 | `/products/search` | `pages/product-list-search-results.html` |

---

## 2. 変更概要

| 変更種別 | 内容 |
|---------|-----|
| UI 追加 | サイドバー絞込フォームに「テイスト」チェックボックスを追加 |
| URL パラメータ追加 | `taste`（複数選択可、整数ID） |
| Model 属性追加 | `tasteOptions`、`selectedTasteIds` |
| ページネーション修正 | ページ遷移リンクに `taste` パラメータを保持するよう変更 |
| ソート・件数フォーム修正 | `taste` の選択状態を hidden input で保持するよう変更 |
| モバイルモーダル | **対象外**（本課題のスコープ外） |

---

## 3. 絞込フォーム（サイドバー）変更仕様

### 3.1 現状の絞込条件（SRCH002）

| # | 絞込条件 | UIタイプ |
|---|---------|---------|
| 1 | 在庫有のみ | チェックボックス 1 個 |
| 2 | 価格帯 | チェックボックス複数 |
| 3 | カラー | カラースウォッチ（チェックボックス） |

### 3.2 変更後の絞込条件（SRCH002）

| # | 絞込条件 | UIタイプ | 変更 |
|---|---------|---------|-----|
| 1 | 在庫有のみ | チェックボックス 1 個 | 変更なし |
| 2 | 価格帯 | チェックボックス複数 | 変更なし |
| 3 | カラー | カラースウォッチ（チェックボックス） | 変更なし |
| 4 | テイスト | チェックボックス複数 | **追加** |

### 3.3 テイスト絞込 UI 仕様

| 項目 | 仕様 |
|-----|-----|
| 表示位置 | カラー絞込の直下、「検索」「リセット」ボタンの直前 |
| UIコンポーネント | `class="field"` > `class="checklist"` 内にチェックボックスを縦並び |
| パラメータ名 | `taste` |
| 値 | テイストマスタの ID（整数） |
| 複数選択 | 可（OR 条件） |
| 選択状態の保持 | Thymeleaf `th:checked="${selectedTasteIds != null and selectedTasteIds.contains(option.id)}"` |
| テイスト選択肢 | `tasteOptions`（`List<CategoryFilterOption>`）を `th:each` でループ |

カテゴリ一覧ページ（`product-list-category-desk.html`）のテイスト絞込と同一のHTML構造を使用する。

---

## 4. テンプレート変更箇所詳細

### 4.1 サイドバーフォーム（デスクトップ表示）

#### 追加箇所：テイスト絞込チェックボックス

カラー絞込 `<div class="field">` の直後に以下を追加する。

```html
<div class="field">
    <label>テイスト</label>
    <div class="checklist">
        <label class="check-item" th:each="option : ${tasteOptions}">
            <input type="checkbox"
                   name="taste"
                   th:value="${option.id}"
                   th:checked="${selectedTasteIds != null and selectedTasteIds.contains(option.id)}">
            <span th:text="${option.label}">ベーシック</span>
        </label>
    </div>
</div>
```

#### 変更箇所：リセットリンク

現在:
```html
<a class="btn" th:href="@{/products/search(q=${keyword})}">リセット</a>
```
変更なし（`taste` を引き継がないリセットが正しい動作）。

### 4.2 ソート・件数フォーム（デスクトップ toolbar）

ソートと件数を変更したときに `taste` 選択状態が失われないよう、hidden input を追加する。

#### 追加箇所：`taste` hidden inputs

`color` hidden input の直後に以下を追加する。

```html
<input type="hidden" name="taste" th:each="id : ${selectedTasteIds}" th:value="${id}">
```

### 4.3 ページネーション

各ページ遷移リンクの `th:href` に `taste=${selectedTasteIds}` を追加する。

#### 変更パターン（前へ・ページ番号・次へ 共通）

**変更前**:
```html
th:href="@{/products/search(q=${keyword},page=...,size=${pageSize},sort=${sortValue},
          inStockOnly=${inStockOnly ? 'true' : null},
          priceBand=${selectedPriceBandIds},
          color=${selectedColorKeys})}"
```

**変更後**:
```html
th:href="@{/products/search(q=${keyword},page=...,size=${pageSize},sort=${sortValue},
          inStockOnly=${inStockOnly ? 'true' : null},
          priceBand=${selectedPriceBandIds},
          color=${selectedColorKeys},
          taste=${selectedTasteIds})}"
```

対象リンク: 前へ・1ページ目・中間ページ・最終ページ・次へ（計5箇所）

### 4.4 モバイルモーダル

**本課題の対象外**。`modal-filters-search` の「簡易版 UI 表示中」プレースホルダーは変更しない。

---

## 5. Model 属性一覧（変更後）

`CatalogController#searchResults` が `model.addAttribute` で設定する属性の変更後一覧。

### 5.1 既存属性（`applyProductListModel` 経由）

| 属性名 | 型 | 内容 |
|-------|---|-----|
| `products` | `List<ProductCardView>` | 商品一覧 |
| `totalCount` | `long` | 総件数 |
| `currentPage` | `int` | 現在のページ番号 |
| `pageSize` | `int` | 1ページあたりの件数 |
| `sortValue` | `String` | ソート値（例: `recommended`） |
| `inStockOnly` | `boolean` | 在庫ありのみフラグ |
| `selectedPriceBandIds` | `List<Integer>` | 選択中の価格帯IDリスト |
| `selectedColorKeys` | `List<String>` | 選択中のカラーキーリスト |
| `colorFilterOptions` | `List<ColorFilterOption>` | カラー絞込選択肢 |
| `totalPages` | `int` | 総ページ数 |

### 5.2 既存属性（`searchResults` でのみ設定）

| 属性名 | 型 | 内容 |
|-------|---|-----|
| `keyword` | `String` | 検索キーワード（空文字 or null の場合は `""`） |

### 5.3 追加属性

| 属性名 | 型 | 設定方法 | 内容 |
|-------|---|---------|-----|
| `selectedTasteIds` | `List<Integer>` | `result.condition().tasteIds()` | 選択中のテイストIDリスト |
| `tasteOptions` | `List<CategoryFilterOption>` | `optionsBundle.searchTasteOptions()` | テイスト絞込選択肢（`ProductFilterOptionsBundle#searchTasteOptions`を使用） |

---

## 6. URL パラメータ一覧（変更後）

| パラメータ名 | 型 | 必須 | 変更 | 内容 |
|-----------|---|-----|-----|-----|
| `q` | `String` | - | 変更なし | 検索キーワード |
| `inStockOnly` | `boolean` | - | 変更なし | 在庫ありのみ（デフォルト `false`） |
| `priceBand` | `List<Integer>` | - | 変更なし | 価格帯ID（複数可） |
| `color` | `List<String>` | - | 変更なし | カラーキー（複数可） |
| `taste` | `List<Integer>` | - | **追加** | テイストID（複数可） |
| `sort` | `String` | - | 変更なし | ソート順（`recommended` / `newest` / `price_asc` / `price_desc`） |
| `page` | `int` | - | 変更なし | ページ番号（デフォルト `1`） |
| `size` | `int` | - | 変更なし | 1ページあたり件数（デフォルト `15`） |

---

## 7. テイストマスタデータ

カテゴリ一覧と共通の値セットを使用する。テーブルは `desk_tastes` を代表として参照する。

| taste_id | display_name |
|---------|-------------|
| 1 | ベーシック |
| 2 | カジュアル |
| 3 | シンプル |
| 4 | モダン |
| 5 | ナチュラル |

（実際の値はDBの `desk_tastes` テーブルの `sort_order` 順で表示される）

---

## 8. 参照：カテゴリ一覧との UI 共通化

テイスト絞込 UI はデスクカテゴリ一覧（`product-list-category-desk.html`）のテイスト絞込と同一の構造を採用する。

| 構成要素 | カテゴリ一覧（SRCH003〜005） | SRCH002（今回追加） |
|--------|--------------------------|------------------|
| `<div class="field">` | 使用 | 同一 |
| `<div class="checklist">` | 使用 | 同一 |
| `<label class="check-item" th:each>` | 使用 | 同一 |
| パラメータ名 | `deskTaste` / `chairTaste` / `storageTaste` | `taste`（共通化） |
| 選択肖槟モデル | `deskTasteOptions` など | `tasteOptions`（`searchTasteOptions` 生成） |
| 選択状態モデル | `deskTasteIds` など | `selectedTasteIds` |
