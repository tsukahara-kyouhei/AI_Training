# FEAT-001 商品検索機能強化 画面仕様変更書

| 項目 | 内容 |
|------|------|
| ドキュメント種別 | 詳細設計書 |
| 機能ID | FEAT-001 |
| 対象システム | office-order（オフィス家具ECサイト） |
| 作成日 | 2026-03-24 |
| バージョン | 1.0 |
| 関連設計書 | [FEAT-001-basic-design.md](FEAT-001-basic-design.md) |

---

## 1. 対象画面

| 画面ID | 画面名 | ファイル |
|--------|--------|---------|
| SCR-006 | 商品検索結果 | `src/main/resources/templates/pages/product-list-search-results.html` |

---

## 2. 変更箇所一覧

| # | 変更箇所 | 変更種別 | 概要 |
|---|---------|---------|------|
| 1 | デスクトップ用絞り込みサイドバーフォーム | 追加 | テイストチェックリストを追加 |
| 2 | デスクトップ用ツールバーフォーム（並び順・件数） | 追加 | テイスト選択値の hidden input を追加 |
| 3 | ページネーションリンク（前へ・次へ・ページ番号） | 変更 | テイストパラメータを `th:href` に追加 |
| 4 | モバイル用絞り込みモーダル | 変更 | テイストチェックリストを含む本格的なフォームへ置き換え |

---

## 3. テンプレート変数（Model 属性）

Controller から Template へ渡す属性の追加・変更。

| 属性名 | 型 | 変更種別 | 説明 |
|--------|------|---------|------|
| `searchTasteOptions` | `List<String>` | **新規** | チェックボックスの選択肢。`${searchTasteOptions}` で参照する。 |
| `selectedTasteNames` | `List<String>` | **新規** | 現在選択中のテイスト名称。チェック状態の復元に使用する。 |

既存属性（`keyword`, `inStockOnly`, `selectedPriceBandIds`, `selectedColorKeys`, `sortValue`, `pageSize`, `currentPage`, `totalPages`, `products`, `totalCount`, `colorFilterOptions`）は変更なし。

---

## 4. 変更詳細

### 4.1 デスクトップ用絞り込みフォーム（テイスト追加）

**挿入位置：** カラーフィールド（`<div class="field">` カラー部分）の直後、「絞り込む」ボタン（`<div class="actions actions--stack">`）の直前。

**追加 HTML（Thymeleaf）：**

```html
<div class="field">
    <label>テイスト</label>
    <div class="checklist">
        <label class="check-item" th:each="tasteName : ${searchTasteOptions}">
            <input type="checkbox"
                   name="taste"
                   th:value="${tasteName}"
                   th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
            <span th:text="${tasteName}">ベーシック</span>
        </label>
    </div>
</div>
```

**動作仕様：**
- `th:each` で `searchTasteOptions`（`List<String>`）を反復してチェックボックスを生成する。
- `th:checked` で Model の `selectedTasteNames` を参照し選択状態を復元する。
- リクエストパラメータ名は `taste`（複数選択可、同名パラメータの繰り返しで送信）。

---

### 4.2 デスクトップ用ツールバーフォーム（hidden input 追加）

**変更位置：** 並び順・表示件数切り替えフォーム内、`<input type="hidden" name="color" ...>` の直後。

**追加 HTML（Thymeleaf）：**

```html
<input type="hidden" name="taste"
       th:each="name : ${selectedTasteNames}"
       th:value="${name}">
```

**目的：** 並び順・表示件数を変更した際に、テイストの選択状態を維持する。

---

### 4.3 ページネーションリンク（テイストパラメータ追加）

現行のページネーションリンク（前へ・次へ・ページ番号）のすべての `th:href` に `taste=${selectedTasteNames}` を追加する。

**変更前（例：前へリンク）：**

```html
th:href="@{/products/search(
    q=${keyword},
    page=${currentPage - 1},
    size=${pageSize},
    sort=${sortValue},
    inStockOnly=${inStockOnly ? 'true' : null},
    priceBand=${selectedPriceBandIds},
    color=${selectedColorKeys}
)}"
```

**変更後：**

```html
th:href="@{/products/search(
    q=${keyword},
    page=${currentPage - 1},
    size=${pageSize},
    sort=${sortValue},
    inStockOnly=${inStockOnly ? 'true' : null},
    priceBand=${selectedPriceBandIds},
    color=${selectedColorKeys},
    taste=${selectedTasteNames}
)}"
```

**変更必要箇所（計6箇所）：**

| 対象 | 現在行（概算） |
|------|-------------|
| 前へリンク（`th:if="${currentPage > 1}"`） | `th:href` |
| 先頭ページ（`page=1`）リンク | `th:href` |
| ウィンドウページ番号ループ内リンク | `th:href` |
| 最終ページリンク | `th:href` |
| 次へリンク（`th:if="${currentPage < totalPages}"`） | `th:href` |
| リセットリンク（`@{/products/search(q=${keyword})}`） | 変更なし（意図的にテイストをリセット） |

---

### 4.4 モバイル用絞り込みモーダル（フォーム実装へ変更）

現行のモバイルモーダル（`#modal-filters-search`）はプレースホルダテキストのみで機能していない。テイスト追加に合わせて、デスクトップ用サイドバーと同等のフォームで実装する。

**変更前：**

```html
<div class="modal" id="modal-filters-search" data-modal hidden aria-hidden="true">
    <div class="modal__panel" role="dialog" aria-modal="true" aria-label="条件で絞り込む（検索結果）">
        <div class="modal__head">
            <p class="modal__title">条件で絞り込む</p>
            <button class="btn btn--ghost" type="button" data-modal-close="modal-filters-search">閉じる</button>
        </div>
        <div class="modal__body">
            <p class="muted" style="margin:0;">モバイルでは現在、簡易版の絞り込みUIを表示しています。</p>
        </div>
    </div>
</div>
```

**変更後：**

```html
<div class="modal" id="modal-filters-search" data-modal hidden aria-hidden="true">
    <div class="modal__panel" role="dialog" aria-modal="true" aria-label="条件で絞り込む（検索結果）">
        <div class="modal__head">
            <p class="modal__title">条件で絞り込む</p>
            <button class="btn btn--ghost" type="button"
                    data-modal-close="modal-filters-search">閉じる</button>
        </div>
        <div class="modal__body">
            <form class="form form--filters" method="get" th:action="@{/products/search}">
                <input type="hidden" name="q" th:value="${keyword}">
                <input type="hidden" name="sort"
                       th:value="${sortValue != null ? sortValue : 'recommended'}">
                <input type="hidden" name="size"
                       th:value="${pageSize != null ? pageSize : 15}">
                <input type="hidden" name="page" value="1">

                <div class="field">
                    <label class="check-item">
                        <input type="checkbox" name="inStockOnly" value="true"
                               th:checked="${inStockOnly}"> 在庫有のみ表示
                    </label>
                </div>
                <div class="field">
                    <label>価格帯（税込）</label>
                    <div class="price-band-list">
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="1"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(1)}"> ～20,000円
                        </label>
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="2"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(2)}"> 20,000円～40,000円
                        </label>
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="3"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(3)}"> 40,000円～60,000円
                        </label>
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="4"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(4)}"> 60,000円～80,000円
                        </label>
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="5"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(5)}"> 80,000円～100,000円
                        </label>
                        <label class="check-item">
                            <input type="checkbox" name="priceBand" value="6"
                                   th:checked="${selectedPriceBandIds != null and selectedPriceBandIds.contains(6)}"> 100,000円～
                        </label>
                    </div>
                </div>
                <div class="field">
                    <label>カラー</label>
                    <div class="color-palette">
                        <label class="color-option"
                               th:each="colorOption : ${colorFilterOptions}"
                               th:title="${colorOption.label}">
                            <input type="checkbox"
                                   name="color"
                                   th:value="${colorOption.key}"
                                   th:checked="${selectedColorKeys != null and selectedColorKeys.contains(colorOption.key)}"
                                   th:aria-label="${colorOption.label}">
                            <span class="color-option__swatch"
                                  th:style="'background:' + ${colorOption.colorCode}"></span>
                        </label>
                    </div>
                </div>
                <div class="field">
                    <label>テイスト</label>
                    <div class="checklist">
                        <label class="check-item"
                               th:each="tasteName : ${searchTasteOptions}">
                            <input type="checkbox"
                                   name="taste"
                                   th:value="${tasteName}"
                                   th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
                            <span th:text="${tasteName}">ベーシック</span>
                        </label>
                    </div>
                </div>
                <div class="actions actions--stack">
                    <button class="btn btn--primary" type="submit">検索</button>
                    <a class="btn" th:href="@{/products/search(q=${keyword})}">リセット</a>
                </div>
            </form>
        </div>
    </div>
</div>
```

---

## 5. パラメータ連携まとめ

### 5.1 リクエストパラメータ（`GET /products/search`）

| パラメータ名 | 型 | 変更種別 | 説明 |
|-----------|------|---------|------|
| `q` | `String` | 変更なし | 検索キーワード |
| `inStockOnly` | `boolean` | 変更なし | 在庫ありのみ |
| `priceBand` | `List<Integer>` | 変更なし | 価格帯ID |
| `color` | `List<String>` | 変更なし | カラーキー |
| `sort` | `String` | 変更なし | 並び順 |
| `page` | `int` | 変更なし | ページ番号 |
| `size` | `int` | 変更なし | 表示件数 |
| `taste` | `List<String>` | **新規** | テイスト名称（例: `taste=ベーシック&taste=モダン`） |

### 5.2 Model 属性（デスクトップ用サイドバー参照）

| 属性名 | テンプレートでの用途 |
|--------|-------------------|
| `searchTasteOptions` | チェックボックス生成（`th:each`） |
| `selectedTasteNames` | チェックボックスの選択状態復元（`th:checked`）、hidden input の繰り返し、ページネーションリンクへの引き渡し |
