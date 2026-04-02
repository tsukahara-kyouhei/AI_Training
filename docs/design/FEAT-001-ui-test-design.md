# FEAT-001 商品検索機能強化 画面・試験設計

## 1. 対象画面

| 画面 | テンプレート | 対象 |
|------|-------------|------|
| 検索結果一覧 | `pages/product-list-search-results.html` | 対象 |
| モバイル絞込モーダル | 同上 | スコープ外 |

---

## 2. 画面設計

### 2.1 絞込サイドバー追加項目

追加位置: カラーの下、検索ボタンの上

```html
<div class="field">
  <label>テイスト</label>
  <div class="taste-list">
    <label class="check-item" th:each="taste : ${tasteOptions}">
      <input type="checkbox"
             name="taste"
             th:value="${taste}"
             th:checked="${selectedTasteNames != null and selectedTasteNames.contains(taste)}">
      <span th:text="${taste}">モダン</span>
    </label>
  </div>
</div>
```

### 2.2 UI 制御方針

- 表示対象はデスクトップ用サイドバーのみ
- モバイルモーダルは現状文言のまま据え置く
- テイスト候補が 0 件でも画面エラーにはしない

---

## 3. リクエストパラメータ設計

| パラメータ | 型 | 複数 | 用途 |
|-----------|----|------|------|
| `q` | `String` | なし | 検索キーワード |
| `taste` | `String` | あり | 検索結果画面テイスト絞込 |
| `inStockOnly` | `boolean` | なし | 在庫あり絞込 |
| `priceBand` | `Integer` | あり | 価格帯絞込 |
| `color` | `String` | あり | カラー絞込 |
| `sort` | `String` | なし | 並び順 |
| `page` | `int` | なし | ページ番号 |
| `size` | `int` | なし | 表示件数 |

---

## 4. Model 設計

### 4.1 検索結果画面で参照する項目

| 項目 | 型 | 備考 |
|------|----|------|
| `keyword` | `String` | 未入力時は空文字 |
| `tasteOptions` | `List<String>` | 重複排除済み |
| `selectedTasteNames` | `List<String>` | リクエスト値の正規化後 |
| `selectedPriceBandIds` | `List<Integer>` | 既存 |
| `selectedColorKeys` | `List<String>` | 既存 |
| `inStockOnly` | `boolean` | 既存 |
| `sortValue` | `String` | 既存 |
| `pageSize` | `int` | 既存 |
| `products` | `List<ProductCardView>` | 既存 |

### 4.2 hidden 項目追加方針

以下のフォーム・リンクで `taste` を引き継ぐ。

- 並び順変更フォーム
- 表示件数変更フォーム
- ページネーションリンク

実装例:

```html
<input type="hidden" name="taste" th:each="taste : ${selectedTasteNames}" th:value="${taste}">
```

---

## 5. リセット動作

### 5.1 方針

- リセットリンクは `q` のみ保持し、他の絞込条件を解除する。
- `taste` はリセット時に除去する。

### 5.2 URL

```text
/products/search?q={keyword}
```

---

## 6. 試験設計

### 6.1 単体試験対象

| 対象 | 観点 |
|------|------|
| `ProductFilterOptionService` | テイスト候補重複排除、テイスト名→ID 変換 |
| `ProductRepository` | キーワード正規化、スペース分割、5ワード制限 |
| `CatalogController` | `taste` パラメータ受け取り、Model 設定 |

### 6.2 結合試験対象

| 対象 | 観点 |
|------|------|
| `/products/search` | キーワード・絞込条件の組み合わせ |
| `ProductMapper.xml` | 複数ワード AND / 商品コード前方一致 / SearchTasteFilter |
| Thymeleaf 画面 | テイスト状態保持、ページネーション引継ぎ |

### 6.3 テストケース一覧

| ID | 観点 | 入力 | 期待結果 |
|----|------|------|----------|
| TC-01 | 商品コード完全一致 | `q=SKU-100` | `SKU-100` のみヒット |
| TC-02 | 商品コード前方一致 | `q=SKU-1` | `SKU-1` で始まる商品コードがヒット |
| TC-03 | 商品コード中間一致除外 | `q=100` | 商品コード `SKU-21001` はコード条件でヒットしない |
| TC-04 | 商品名部分一致 | `q=デスク` | 商品名に `デスク` を含む商品がヒット |
| TC-05 | バリエーション名部分一致 | `q=ウォルナット` | `variation_name` に含む商品がヒット |
| TC-06 | 説明文部分一致 | `q=収納` | `description` に含む商品がヒット |
| TC-07 | 複数ワード AND | `q=モダン デスク` | 両方含む商品のみヒット |
| TC-08 | 空白のみ入力 | `q=   ` | キーワード未入力と同じ結果 |
| TC-09 | 6ワード入力 | 6ワード | 先頭 5 ワードのみ使用 |
| TC-10 | 全角記号 | `q=ＳＫＵ－100` | `SKU-100` と同じ結果 |
| TC-11 | テイスト候補表示 | 画面表示 | 重複なしで表示 |
| TC-12 | テイスト絞込 | `taste=モダン` | モダン該当商品のみヒット |
| TC-13 | テイスト横断 | `taste=モダン` | デスク・チェア・収納を横断して該当 |
| TC-14 | 条件保持 | `taste=モダン&page=2` | ページ遷移後も選択維持 |
| TC-15 | ソート変更 | `taste=モダン&sort=price_asc` | テイスト選択維持 |
| TC-16 | リセット | `q=デスク&taste=モダン` → リセット | `taste` が解除 |

---

## 7. レビュー観点

- `taste` パラメータがすべての導線で引き継がれているか
- キーワード未入力時の表示文言が維持されるか
- テイスト候補が 1 件ずつしか表示されないか
- モバイルモーダルに不要な変更が入っていないか
