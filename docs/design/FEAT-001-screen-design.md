# FEAT-001 画面詳細設計書

| 項目         | 内容                                                                  |
| ------------ | --------------------------------------------------------------------- |
| Issue ID     | FEAT-001                                                              |
| 作成日       | 2026-04-27                                                            |
| 対象ファイル | `src/main/resources/templates/pages/product-list-search-results.html` |

---

## 1. 変更概要

| 変更箇所                         | 種別 | 内容                                                     |
| -------------------------------- | ---- | -------------------------------------------------------- |
| デスクトップサイドバー内フォーム | 追加 | テイストフィルタ（デスク・チェア・収納）チェックボックス |
| デスクトップ並び順フォーム       | 修正 | テイスト選択状態を hidden で引き継ぐ                     |
| ページネーションリンク           | 修正 | テイスト選択状態を URL パラメータに追記                  |
| 0件時の検索結果エリア            | 追加 | おすすめ商品グリッド                                     |
| バリデーションエラー表示         | 追加 | キーワード超過時のエラーメッセージ                       |

---

## 2. Model 属性一覧（画面で使用するもの）

### 2-1. 既存属性（変更なし）

| 属性名                 | 型                        | 用途                             |
| ---------------------- | ------------------------- | -------------------------------- |
| `keyword`              | `String`                  | 検索キーワード表示・フォーム復元 |
| `inStockOnly`          | `boolean`                 | 在庫チェックボックス状態         |
| `selectedPriceBandIds` | `List<Integer>`           | 価格帯チェックボックス状態       |
| `selectedColorKeys`    | `List<String>`            | カラーチェックボックス状態       |
| `colorFilterOptions`   | `List<ColorFilterOption>` | カラー選択肢一覧                 |
| `products`             | `List<ProductCardView>`   | 検索結果一覧                     |
| `totalCount`           | `long`                    | 総件数                           |
| `totalPages`           | `int`                     | 総ページ数                       |
| `currentPage`          | `int`                     | 現在ページ                       |
| `sortValue`            | `String`                  | 並び順値                         |
| `pageSize`             | `int`                     | 表示件数                         |

### 2-2. 新規追加属性

| 属性名                | 型                            | 用途                                          |
| --------------------- | ----------------------------- | --------------------------------------------- |
| `deskTasteIds`        | `List<Integer>`               | デスクテイストチェックボックス状態復元        |
| `chairTasteIds`       | `List<Integer>`               | チェアテイストチェックボックス状態復元        |
| `storageTasteIds`     | `List<Integer>`               | 収納テイストチェックボックス状態復元          |
| `deskTasteOptions`    | `List<CategoryFilterOption>`  | デスクテイスト選択肢                          |
| `chairTasteOptions`   | `List<CategoryFilterOption>`  | チェアテイスト選択肢                          |
| `storageTasteOptions` | `List<CategoryFilterOption>`  | 収納テイスト選択肢                            |
| `suggestedProducts`   | `List<RankedProductCardView>` | 0件時おすすめ商品（空リストの場合は表示なし） |
| `keywordTooLong`      | `Boolean`                     | バリデーションエラーフラグ                    |

---

## 3. バリデーションエラー表示

### 3-1. 表示位置

キーワード表示カードの直下（既存 `<div class="card">` の後）に挿入。

### 3-2. HTML

```html
<div class="card card--error" th:if="${keywordTooLong}">
  <div class="card__pad">
    <p class="text--error" style="margin:0;">
      キーワードは100文字以内で入力してください。
    </p>
  </div>
</div>
```

---

## 4. デスクトップサイドバー — テイストフィルタ追加

### 4-1. 追加位置

サイドバーフォーム内の **カラーフィールドの後、アクションボタンの前** に追加。

```
[在庫有のみ]
[価格帯]
[カラー]
▼ ここに追加
[テイスト（デスク）]
[テイスト（チェア）]
[テイスト（収納）]
▼ 既存
[検索] [リセット]
```

### 4-2. HTML

```html
<!-- テイストフィルタ（デスク） -->
<div
  class="field"
  th:if="${deskTasteOptions != null and !#lists.isEmpty(deskTasteOptions)}"
>
  <label>テイスト（デスク）</label>
  <div class="check-list">
    <label class="check-item" th:each="opt : ${deskTasteOptions}">
      <input
        type="checkbox"
        name="deskTaste"
        th:value="${opt.id}"
        th:checked="${deskTasteIds != null and deskTasteIds.contains(opt.id)}"
      />
      <span th:text="${opt.displayName}">ナチュラル</span>
    </label>
  </div>
</div>

<!-- テイストフィルタ（チェア） -->
<div
  class="field"
  th:if="${chairTasteOptions != null and !#lists.isEmpty(chairTasteOptions)}"
>
  <label>テイスト（チェア）</label>
  <div class="check-list">
    <label class="check-item" th:each="opt : ${chairTasteOptions}">
      <input
        type="checkbox"
        name="chairTaste"
        th:value="${opt.id}"
        th:checked="${chairTasteIds != null and chairTasteIds.contains(opt.id)}"
      />
      <span th:text="${opt.displayName}">ナチュラル</span>
    </label>
  </div>
</div>

<!-- テイストフィルタ（収納） -->
<div
  class="field"
  th:if="${storageTasteOptions != null and !#lists.isEmpty(storageTasteOptions)}"
>
  <label>テイスト（収納家具）</label>
  <div class="check-list">
    <label class="check-item" th:each="opt : ${storageTasteOptions}">
      <input
        type="checkbox"
        name="storageTaste"
        th:value="${opt.id}"
        th:checked="${storageTasteIds != null and storageTasteIds.contains(opt.id)}"
      />
      <span th:text="${opt.displayName}">ナチュラル</span>
    </label>
  </div>
</div>
```

> `opt.id` は `CategoryFilterOption.id()`、`opt.displayName` は `CategoryFilterOption.displayName()` に対応。

---

## 5. 並び順フォーム — テイスト選択状態の引き継ぎ

デスクトップ並び順フォーム（`toolbar__group` 内 form）に hidden input を追加し、並び順変更後もテイストフィルタ選択を維持する。

```html
<!-- 既存 hidden に続けて追加 -->
<input
  type="hidden"
  name="deskTaste"
  th:each="id : ${deskTasteIds}"
  th:value="${id}"
/>
<input
  type="hidden"
  name="chairTaste"
  th:each="id : ${chairTasteIds}"
  th:value="${id}"
/>
<input
  type="hidden"
  name="storageTaste"
  th:each="id : ${storageTasteIds}"
  th:value="${id}"
/>
```

---

## 6. ページネーションリンク — テイスト選択状態の引き継ぎ

ページネーション内の各リンク `th:href` を修正し、`deskTaste`・`chairTaste`・`storageTaste` パラメータを追加する。

### 6-1. 修正パターン（前へ・次へ・ページ番号すべて共通）

現状:

```html
th:href="@{/products/search(q=${keyword},page=${...},size=${pageSize},sort=${sortValue},
inStockOnly=${inStockOnly ? 'true' : null}, priceBand=${selectedPriceBandIds},
color=${selectedColorKeys})}"
```

変更後:

```html
th:href="@{/products/search(q=${keyword},page=${...},size=${pageSize},sort=${sortValue},
inStockOnly=${inStockOnly ? 'true' : null}, priceBand=${selectedPriceBandIds},
color=${selectedColorKeys}, deskTaste=${deskTasteIds},
chairTaste=${chairTasteIds}, storageTaste=${storageTasteIds})}"
```

> Thymeleaf のリスト型パラメータは自動的に複数の `&deskTaste=1&deskTaste=2` に展開される。

---

## 7. 0件時おすすめ商品エリア

### 7-1. 表示条件

`suggestedProducts` が null でなく、かつ 1件以上の場合に表示する。

### 7-2. 表示位置

```
[該当する商品は見つかりませんでした。] ← 既存
▼ ここに追加
[おすすめ商品エリア（ランキング順）]
```

### 7-3. HTML（既存の「見つかりませんでした」カードの直後）

```html
<!-- 0件時おすすめ商品 -->
<div
  th:if="${suggestedProducts != null and !#lists.isEmpty(suggestedProducts)}"
  style="margin-top: 24px;"
>
  <h2 class="section-title">おすすめ商品</h2>
  <div class="grid grid--3">
    <th:block th:each="ranked : ${suggestedProducts}">
      <div
        th:replace="~{fragments/common/product-card :: productCardByView(${ranked.productCard})}"
      ></div>
    </th:block>
  </div>
</div>
```

> `RankedProductCardView` は `rank()` と `productCard()` を持つ record。
> テンプレートでは順位表示は行わず、`productCard()` を既存の `productCardByView` フラグメントに渡す。

---

## 8. リセットリンクの修正

テイストフィルタ追加に伴い、リセットリンクは変更不要。
現状の `th:href="@{/products/search(q=${keyword})}"` でテイスト選択はクリアされる（パラメータを引き継がないため）。

---

## 9. モバイルモーダル（対象外）

```html
<div class="modal" id="modal-filters-search" ...>
  ...
  <p class="muted">モバイルでは現在、簡易版の絞り込みUIを表示しています。</p>
  ...
</div>
```

FR-005 の決定により、今回の実装ではモバイルモーダルへのテイストフィルタ追加は **対象外**。
既存の「簡易版」テキストはそのまま維持する。

---

## 10. CSS クラス設計メモ

新規追加 HTML で使用するクラス:

| クラス名      | 役割                             | 参照先                                                                    |
| ------------- | -------------------------------- | ------------------------------------------------------------------------- |
| `check-list`  | チェックボックスの縦並びコンテナ | 既存カテゴリページのサイドバーに同じクラスがあれば流用                    |
| `check-item`  | 個々のチェックボックスラベル     | サイドバー内の在庫チェックに使用されている既存クラス                      |
| `card--error` | エラー状態のカード               | 既存スタイルがなければ新規定義（`border-color: var(--color-error)` 程度） |
| `text--error` | エラーテキスト                   | 既存スタイルがなければ新規定義（`color: var(--color-error)` 程度）        |

> `check-list` が既存 CSS にない場合は `display: flex; flex-direction: column; gap: 6px;` 相当の簡易スタイルを `components.css` に追加する。
