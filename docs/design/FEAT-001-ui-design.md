# FEAT-001 商品検索機能強化 画面詳細設計書

対象ファイル: `src/main/resources/templates/pages/product-list-search-results.html`

---

## 1. 変更概要

検索結果一覧画面の絞り込みフォームに「テイスト」セクションを追加する。

カテゴリ一覧画面（デスク・チェア・収納）の実装と同様のチェックボックス形式とし、
選択肢は 3 カテゴリのテイストマスタを名称統合して表示する。

---

## 2. クエリパラメータ設計

### 2.1 追加パラメータ（テイスト絞り込み）

| パラメータ名 | 型 | 複数選択 | 例 |
|------------|-----|---------|-----|
| `taste` | `String` | 可（同名パラメータの繰り返し） | `?taste=ナチュラル&taste=ベーシック` |

**ルール：**

- 値は `display_name`（テイストマスタの名称文字列）とする。
- 未選択の場合はパラメータ自体を送信しない（絞り込みなし）。
- 無効な名称をパラメータに含めた場合、サーバー側でホワイトリスト照合により除外する。

### 2.2 既存パラメータとの共存

テイストパラメータは既存の絞り込みパラメータ（`inStockOnly`, `priceBand`, `color`）と AND 条件で結合される。

他の絞り込み条件と同様に、ページネーションリンク・再検索フォームにも引き回す必要がある（後述）。

---

## 3. Model 属性（サーバーサイド）

`CatalogController.searchResults()` が追加する Model 属性：

| 属性名 | 型 | 内容 |
|--------|-----|------|
| `searchTasteOptions` | `List<String>` | 統合テイスト選択肢（display_name の一覧） |
| `selectedTasteNames` | `List<String>` | 選択状態のテイスト名（ホワイトリスト照合済み） |

---

## 4. テイスト絞り込み UI の HTML 設計

### 4.1 追加箇所

既存の絞り込みフォーム内の「カラー」セクションの後に追加する。
カテゴリ一覧画面の実装（`product-list-category-desk.html` 等）と同じ CSS クラス・構造を踏襲する。

### 4.2 Thymeleaf 実装

```html
<!-- テイスト絞り込み（新規追加） -->
<div class="field">
  <label>テイスト</label>
  <div class="checklist">
    <label class="check-item" th:each="tasteName : ${searchTasteOptions}">
      <input type="checkbox"
             name="taste"
             th:value="${tasteName}"
             th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
      <span th:text="${tasteName}">ナチュラル</span>
    </label>
  </div>
</div>
```

#### カテゴリ一覧画面との実装差分

| 項目 | カテゴリ一覧（例：デスク） | 検索結果一覧（本機能） |
|------|--------------------------|----------------------|
| Model 属性名 | `deskTasteOptions`（`List<CategoryFilterOption>`） | `searchTasteOptions`（`List<String>`） |
| カテゴリ専用 ID | `option.id`（Long） | 不使用 |
| フォーム値（`th:value`） | `${option.id}` | `${tasteName}` |
| チェック判定 | `deskTasteIds.contains(option.id)` | `selectedTasteNames.contains(tasteName)` |
| パラメータ名（`name`） | `deskTaste` | `taste` |

---

## 5. フォーム action とパラメータ引き回し

### 5.1 絞り込みフォーム（サイドバー・モーダル）

フォームの `action` は `/products/search`（既存と同じ）。
`keyword`（検索ワード）を引き継ぐ hidden フィールドはすでに実装済み。

```html
<!-- 既存（変更なし） -->
<form action="/products/search" method="get">
  <input type="hidden" name="q" th:value="${condition.keyword}">
  <!-- ... 既存フィルタ ... -->

  <!-- テイスト絞り込み（新規追加） -->
  <div class="field">
    <label>テイスト</label>
    <div class="checklist">
      <label class="check-item" th:each="tasteName : ${searchTasteOptions}">
        <input type="checkbox"
               name="taste"
               th:value="${tasteName}"
               th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
        <span th:text="${tasteName}">ナチュラル</span>
      </label>
    </div>
  </div>

  <button type="submit">絞り込む</button>
</form>
```

### 5.2 ページネーションリンク

ページ遷移後もテイスト選択状態を保持するため、ページネーションリンクに `taste` パラメータを付加する。

```html
<!-- ページリンク生成（Thymeleaf の例） -->
<a th:href="@{/products/search(
    q=${condition.keyword},
    inStockOnly=${condition.inStockOnly},
    priceBand=${selectedPriceBandIds},
    color=${selectedColorKeys},
    taste=${selectedTasteNames},   <!-- 新規追加 -->
    sort=${sortValue},
    page=${p},
    size=${pageSize}
  )}"> ... </a>
```

### 5.3 並び替えリンク

並び替えプルダウンもページネーションと同様に `taste` パラメータを引き回す。

---

## 6. 選択状態クリア

「絞り込みをリセット」や個別の「× で選択解除」機能が現状のテンプレートに実装されている場合は、
`taste` パラメータも対象に含める。

現状のリセットリンクが `/products/search?q=...` 形式（絞り込みなしの URL）であれば、
`taste` を含まないリンクにすることで自動的にリセットされる（追加実装不要）。

---

## 7. レイアウト上の配置

```
絞り込みフォーム（サイドバー）
├── 在庫あり チェックボックス    ← 既存（変更なし）
├── 価格帯   チェックボックス群  ← 既存（変更なし）
├── カラー   スウォッチ群        ← 既存（変更なし）
└── テイスト チェックボックス群  ← 新規追加（本設計）
```

モバイルモーダル（`modal-filters-search`）内にも同じ構造を追加する。

---

## 8. 表示例（参考）

```
[ ] 絞り込み
├── 在庫あり
│   ☑ 在庫ありのみ表示
├── 価格帯
│   ☐ ～1万円  ☐ 1～3万円  ☐ 3万円～
├── カラー
│   ● ● ● ○ ●
└── テイスト           ← 新規
    ☑ ナチュラル
    ☐ ベーシック
    ☑ ヴィンテージ
    ☐ モダン
```

選択肢は `searchTasteOptions` の順序で表示される（`sort_order` の MIN 値昇順）。
