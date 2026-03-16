# FEAT-001 画面設計書 — 商品検索機能強化

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| 作成日 | 2026-03-15 |
| ステータス | Draft |
| 関連要件 | [FEAT-001-requirements.md](../issues/FEAT-001-requirements.md) |
| 関連詳細設計 | [FEAT-001-detail-design.md](FEAT-001-detail-design.md) |

---

## 1. 変更対象画面

| 画面名 | テンプレートファイル | 変更区分 |
|------|----------------|--------|
| 商品検索結果一覧 | `src/main/resources/templates/pages/product-list-search-results.html` | 変更（テイスト絞込の追加） |

カテゴリ一覧（desk/chair/storage）、商品詳細などその他の画面は変更なし。

---

## 2. 現状の絞込条件（変更前）

`product-list-search-results.html` 現在の絞込条件は以下の3項目。

| # | 絞込項目 | UI部品 | リクエストパラメータ |
|---|---------|-------|-----------------|
| 1 | 在庫有のみ表示 | チェックボックス（1個） | `inStockOnly=true` |
| 2 | 価格帯 | チェックボックス（6段階） | `priceBand=1` `priceBand=2` ... |
| 3 | カラー | カラーパレット（丸いアイコン、複数選択可） | `color=<colorId>` |

---

## 3. 変更後の絞込条件

「テイスト」を既存の3項目に追加する（合計4項目）。

| # | 絞込項目 | UI部品 | リクエストパラメータ | 備考 |
|---|---------|-------|-----------------|------|
| 1 | 在庫有のみ表示 | チェックボックス（1個） | `inStockOnly=true` | 変更なし |
| 2 | 価格帯 | チェックボックス（6段階） | `priceBand=1` ... | 変更なし |
| 3 | カラー | カラーパレット（複数選択可） | `color=<colorId>` | 変更なし |
| 4 | **テイスト** | **チェックボックス（5項目）** | **`taste=<テイスト名>`** | ★今回追加 |

---

## 4. テイスト絞込 UI 仕様

### 4.1 表示項目

| 表示名 | パラメータ値 |
|-------|-----------|
| ベーシック | `taste=ベーシック` |
| カジュアル | `taste=カジュアル` |
| シンプル | `taste=シンプル` |
| モダン | `taste=モダン` |
| ナチュラル | `taste=ナチュラル` |

- 表示名・順序は `desk_tastes` / `chair_tastes` / `storage_tastes` の `display_name` を統合・重複排除して決定する。
- 順序はマスタの `taste_id` 昇順（ベーシック → カジュアル → シンプル → モダン → ナチュラル）。

### 4.2 動作仕様

- **複数選択可能**（チェックボックス）。
- 選択したテイストのいずれかに合致する商品を表示する（OR条件）。
- 未選択の場合はテイスト絞込は適用しない（全商品対象）。
- テイストを選択した状態でフォームを送信すると、`taste` パラメータが複数付与される。
  - 例: `?keyword=デスク&taste=ナチュラル&taste=シンプル`

### 4.3 選択状態の保持

- フォーム送信後もチェック済みの選択が維持されること（`th:checked` で `selectedTasteNames` に含まれるか判定）。

---

## 5. テンプレート変更箇所

### 5.1 サイドバー（デスクトップ用絞込フォーム）

既存の「カラー」絞込セクションの下に、以下のテイスト絞込セクションを追加する。

```html
<!-- テイスト絞込（★追加） -->
<div class="filter-section">
  <h3 class="filter-title">テイスト</h3>
  <ul class="filter-list">
    <li th:each="tasteName : ${searchTasteOptions}">
      <label class="filter-checkbox-label">
        <input type="checkbox"
               name="taste"
               th:value="${tasteName}"
               th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
        <span th:text="${tasteName}">テイスト名</span>
      </label>
    </li>
  </ul>
</div>
```

### 5.2 モバイル用絞込モーダル

デスクトップ用と同様のテイスト絞込セクションを、モバイル用モーダルの対応箇所（カラー絞込の下）に追加する。

```html
<!-- テイスト絞込（モバイル用モーダル内、★追加） -->
<div class="filter-section">
  <h3 class="filter-title">テイスト</h3>
  <ul class="filter-list">
    <li th:each="tasteName : ${searchTasteOptions}">
      <label class="filter-checkbox-label">
        <input type="checkbox"
               name="taste"
               th:value="${tasteName}"
               th:checked="${selectedTasteNames != null and selectedTasteNames.contains(tasteName)}">
        <span th:text="${tasteName}">テイスト名</span>
      </label>
    </li>
  </ul>
</div>
```

### 5.3 モデル変数（テンプレートで使用する変数一覧）

| 変数名 | 型 | 内容 | 設定箇所 |
|-------|---|-----|---------|
| `searchTasteOptions` | `List<String>` | テイスト名の選択肢（5件） | `CatalogController` |
| `selectedTasteNames` | `List<String>` | 選択中のテイスト名リスト | `CatalogController` |

---

## 6. UI レイアウト（絞込フォームの構成）

```
┌────────────────────────────┐
│ 絞り込み条件               │
├────────────────────────────┤
│ □ 在庫有のみ表示           │
├────────────────────────────┤
│ 価格帯                     │
│ □ 〜5,000円               │
│ □ 5,000〜10,000円         │
│ □ 10,000〜20,000円        │
│ □ 20,000〜30,000円        │
│ □ 30,000〜50,000円        │
│ □ 50,000円〜              │
├────────────────────────────┤
│ カラー                     │
│ 🔴 🔵 🟢 ...              │
├────────────────────────────┤
│ テイスト          ★追加   │
│ □ ベーシック               │
│ □ カジュアル               │
│ □ シンプル                 │
│ □ モダン                   │
│ □ ナチュラル               │
└────────────────────────────┘
```

---

## 7. スタイル方針

- テイスト絞込セクションのHTMLクラス・スタイルは、既存の「価格帯」絞込セクションと同じ構造・CSSクラスを使用する。
- 新規CSSの追加は不要。
