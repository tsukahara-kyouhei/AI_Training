# FEAT-001 — フロントエンド詳細設計

> 作成日: 2026-03-16  
> 関連設計概要: [FEAT-001-design-overview.md](FEAT-001-design-overview.md)

---

## 1. 変更対象ファイル

| ファイル | 変更内容 |
|---------|---------|
| `src/main/resources/templates/pages/product-list-search-results.html` | テイスト絞込 UI の追加、sort/page フォームへの hidden フィールド追加 |

---

## 2. テンプレートが受け取るモデル属性（変更後の全量）

### 既存属性（変更なし）

| 属性名 | 型 | 説明 |
|-------|-----|------|
| `keyword` | `String` | 検索キーワード（未入力時は `""`） |
| `products` | `List<ProductCardView>` | 商品カードリスト |
| `totalCount` | `long` | ヒット件数 |
| `totalPages` | `int` | 総ページ数 |
| `currentPage` | `int` | 現在ページ番号 |
| `pageSize` | `int` | 1ページ件数 |
| `sortValue` | `String` | ソート値（`recommended` など） |
| `inStockOnly` | `boolean` | 在庫ありフィルター選択状態 |
| `selectedPriceBandIds` | `List<Integer>` | 選択中の価格帯ID |
| `selectedColorKeys` | `List<String>` | 選択中のカラーキー |
| `colorFilterOptions` | `List<ColorFilterOption>` | カラー絞込の選択肢一覧 |

### 新規追加属性

| 属性名 | 型 | 説明 |
|-------|-----|------|
| `tasteOptions` | `List<String>` | テイスト絞込の選択肢（表示名文字列のリスト） |
| `selectedTasteNames` | `List<String>` | 選択中のテイスト表示名（空の場合は `List.of()`） |

---

## 3. product-list-search-results.html の変更仕様

### 3-1. 変更箇所の全体マップ

```
product-list-search-results.html
├── サイドバーフィルターフォーム（デスクトップ）
│   ├── [既存] hidden: q, sort, size, page=1
│   ├── [既存] 在庫あり チェックボックス
│   ├── [既存] 価格帯 チェックボックス × 6
│   ├── [既存] カラー チェックボックス（th:each）
│   └── [追加] テイスト チェックボックス（th:each）  ← ★ 追加
├── ツールバー（ソート・件数）
│   ├── [既存] hidden: q, inStockOnly, page=1
│   ├── [既存] hidden: priceBand × N
│   ├── [既存] hidden: color × N
│   └── [追加] hidden: taste × N                    ← ★ 追加
├── 商品グリッド
│   └── 変更なし
└── ページネーション
    ├── [既存] パラメーター: q, inStockOnly, priceBand, color, sort, size
    └── [追加] パラメーター: taste                   ← ★ 追加
```

---

### 3-2. サイドバーフィルターフォーム — テイスト絞込の追加

**追加位置:** カラー絞込セクション（`<div class="color-palette">...</div>`）の直後

```html
<!-- テイスト -->
<div class="field">
  <label class="filter-label">テイスト</label>
  <div class="checklist">
    <label class="check-item" th:each="tasteName : ${tasteOptions}">
      <input type="checkbox"
             name="taste"
             th:value="${tasteName}"
             th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
      <span th:text="${tasteName}">ナチュラル</span>
    </label>
  </div>
</div>
```

**既存カテゴリ一覧との差異:**
- カテゴリ一覧（例: `product-list-category-desk.html`）の `deskTasteOptions` は `CategoryFilterOption`（id + label）を使い、チェック状態を `deskTasteIds.contains(option.id)` で判定する
- 本画面では選択肢・選択値ともに `String`（`display_name`）を使用するため、判定式が `selectedTasteNames.contains(tasteName)` となる

**テイスト選択肢が 0 件の場合:**
- `tasteOptions` が空リストのとき、`th:each` によりチェックボックスが 1 件も生成されない
- セクション自体（`<div class="field">` ブロック）を `th:if="${not #lists.isEmpty(tasteOptions)}"` で非表示にする

```html
<!-- 0件時は非表示 -->
<div class="field" th:if="${not #lists.isEmpty(tasteOptions)}">
  <label class="filter-label">テイスト</label>
  <div class="checklist">
    <label class="check-item" th:each="tasteName : ${tasteOptions}">
      <input type="checkbox"
             name="taste"
             th:value="${tasteName}"
             th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
      <span th:text="${tasteName}">ナチュラル</span>
    </label>
  </div>
</div>
```

---

### 3-3. ツールバーフォーム — hidden フィールドの追加

ソート変更・件数変更時にフィルター状態を保持するため、ツールバー内の `<form>` に `taste` の hidden フィールドを追加する。

**追加位置:** 既存の `color` 用 hidden フィールド群の直後

```html
<!-- ▼ 既存（変更なし） -->
<input type="hidden" th:each="colorKey : ${selectedColorKeys}"
       name="color" th:value="${colorKey}">

<!-- ▼ 追加 -->
<input type="hidden" th:each="tasteName : ${selectedTasteNames}"
       name="taste" th:value="${tasteName}">
```

**ツールバーフォームの hidden フィールド完全一覧（変更後）:**

| フィールド名 | 内容 | 変更 |
|------------|------|------|
| `q` | 検索キーワード | 既存 |
| `inStockOnly` | 在庫フィルター | 既存 |
| `priceBand` (`th:each`) | 選択中の価格帯ID | 既存 |
| `color` (`th:each`) | 選択中のカラーキー | 既存 |
| `taste` (`th:each`) | 選択中のテイスト名 | **追加** |
| `page` | 固定値 `1`（ソート変更時はページリセット） | 既存 |

---

### 3-4. ページネーション — パラメーターの追加

ページリンク（前へ・番号・次へ）に `taste` パラメーターを追加する。

既存のカテゴリ一覧（`product-list-category-desk.html`）では、Thymeleaf の `@{URL(param=value,...)}` 形式でパラメーターを構築しているか、フォームの hidden フィールドを使った submit 方式を採用している。

本ファイルの実装方式に合わせて以下のどちらかを適用する:

**方式A: `th:href` 付きアンカータグ形式（URL にパラメーターを組み立てる）**

```html
<!-- ページリンクの href 生成例（現ページ → targetPage への遷移） -->
<a th:href="@{/products/search(
    q=${keyword},
    inStockOnly=${inStockOnly},
    priceBand=${selectedPriceBandIds},
    color=${selectedColorKeys},
    taste=${selectedTasteNames},
    sort=${sortValue},
    size=${pageSize},
    page=${targetPage})}">...</a>
```

**方式B: hidden フィールド付きフォーム submit 形式**

既存の pagination フォームに以下を追加:

```html
<input type="hidden" th:each="tasteName : ${selectedTasteNames}"
       name="taste" th:value="${tasteName}">
```

> **確認事項（U-05 に対応）:** 既存のページネーション実装が方式 A か方式 B かを確認し、同じ方式で `taste` を追加すること。

---

### 3-5. モバイルフィルターモーダル

現在 `#modal-filters-search` はプレースホルダー（「モバイルでは現在、簡易版の絞り込みUIを表示しています。」）が表示されている。

**本対応のスコープ: モバイルモーダルへのテイスト追加は対象外とする。**

理由: 現状のモバイル UI が暫定実装であり、テイストを追加しても UI が未完成のまま公開されることになるため。モバイル対応は別タスクとして計画する。

---

## 4. リセットリンクの仕様

フィルターフォームの「リセット」リンクは `?q={keyword}` のみを渡し、すべてのフィルター（在庫・価格帯・カラー・テイスト）をクリアする。

```html
<!-- リセットリンク（変更なし） -->
<a th:href="@{/products/search(q=${keyword})}">リセット</a>
```

`taste` パラメーターはリセット時に渡さないため、テイスト選択状態もクリアされる。変更不要。

---

## 5. 既存カテゴリ一覧テンプレートとの対比

テイスト絞込 UI の実装パターンをカテゴリ一覧（デスク）と比較する。

| 項目 | `product-list-category-desk.html`（既存） | `product-list-search-results.html`（本変更） |
|-----|------------------------------------------|---------------------------------------------|
| 選択肢の型 | `List<CategoryFilterOption>`（id + label）| `List<String>`（display_name） |
| モデル属性名 | `deskTasteOptions` | `tasteOptions` |
| チェック状態 | `deskTasteIds.contains(option.id)` | `selectedTasteNames.contains(tasteName)` |
| name 属性 | `deskTaste` | `taste` |
| value 属性 | `${option.id}`（Integer） | `${tasteName}`（String） |
| カテゴリ横断 | なし（デスクのみ） | あり（デスク・チェア・収納） |

---

## 6. Thymeleaf 実装上の注意点

### 6-1. `contains()` の動作保証

`selectedTasteNames.contains(tasteName)` の評価時、`selectedTasteNames` が `null` の場合に NPE が発生する。Controller 側で `List.of()` を設定するが、念のため Thymeleaf 式でのガードを入れる。

```html
th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}"
```

### 6-2. `#lists.isEmpty()` ユーティリティ

Thymeleaf の `#lists.isEmpty(list)` は `null` セーフで、`null` を渡しても `true` を返す。`tasteOptions` が `null` になるケースはないが（Controller で `List.of()` 相当が返る）、防御的に使用できる。

```html
th:if="${not #lists.isEmpty(tasteOptions)}"
```
