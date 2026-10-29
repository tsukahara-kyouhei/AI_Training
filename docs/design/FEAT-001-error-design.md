# FEAT-001 エラー設計

## 1. 目的

本設計では、検索機能における異常系、0件、入力値不正、DB エラーの扱いを定義する。

## 2. 既存確認済み事項

- 0件時は `product-list-search-results.html` の card で「該当する商品は見つかりませんでした。」を表示する
- `ProductListSearchService.searchWithPageCorrection()` はページ超過時に最終ページへ補正する
- `ProductSort.fromValue()` は不正値を defaultSort に置き換える
- `ProductFilterOptionService` は候補表から入力値を制限する

## 3. エラー分類

### 3.1 不正なパラメータ

対象:
- `page < 1`
- `size` not in 15/30/60
- `sort` not allowed
- `priceBand` not in valid IDs
- `color` key not in options
- `*Taste` IDs not in options

対応:
- `normalize*` で許容値だけ残す
- 不正値は無視または default に置換する
- 画面上では 200 OK として続行する

### 3.2 存在しない ID

対象:
- productId not found for detail page
- category filter values not active

対応:
- `ProductDetailView` は `Optional.empty()` を返し、`ResponseStatusException(NOT_FOUND)` を送る
- 検索一覧では候補に含まれない ID を無視する

### 3.3 不正なページ番号

対象:
- `/products/search?page=999`

対応:
- `ProductListSearchService.searchWithPageCorrection()` で totalPages を計算
- total page を超える場合は最終ページへ補正して再検索する

### 3.4 不正な size

対象:
- `size=0`, `size=10`, `size=999`

対応:
- 既存 `ProductListSearchService` は `size` を request で直接扱うが、UI では 15/30/60 の制限を設計する
- `size` が許容外なら default 15 に置換する

### 3.5 不正な sort

対象:
- `sort=foo`

対応:
- `ProductSort.fromValue(value, defaultValue)` が default に置換する

### 3.6 DB エラー

対象:
- DB 接続失敗
- SQL 文法異常
- query execution failure

対応:
- アプリ側で一般エラー画面に遷移させる
- `500` を返さないで、アプリ共通エラー画面へ誘導する

### 3.7 想定外エラー

対象:
- NullPointer / unexpected exception

対応:
- `@ControllerAdvice` を使用して共通でログ出力、一般エラー画面を返す
- 検索一覧の UI で具体的な値を露出しない

### 3.8 0件

対象:
- 条件に一致する商品がない

対応:
- `ProductListPage` を empty list で返す
- 画面が `products is empty` と判定して既存メッセージを表示する
- これは正常系の一部であり、エラー画面遷移ではない

### 3.9 モバイル UI の扱い

- モバイル UI は表示および query param で条件維持を行う
- 既存の GET `/products/search` 形式に合わせて `deskTaste` / `chairTaste` / `storageTaste` を含む条件を保持する
- 追加実装の延期は設計対象外であり、今回の設計で完結する

## 4. エラー時表示ポリシー

- 0件: 200 OK, 画面内メッセージ表示
- 不正パラメータ: 200 OK, 無視/補正して検索継続
- 不正ID: 404 Not Found for detail page
- DB エラー: 500, error page

## 5. セキュリティ上のエラー対応

- SQL インジェクション: `#{}` を強制する
- XSS: 値の表示は `th:text` を使用し、HTML エスケープを前提とする
- パラメータ改ざん: 許容候補に制限する

## 6. 確認ポイント

- 画面側エラーとアプリ共通エラーは既存の共通エラー画面テンプレートに合わせて描画する
- DB 例外のログ出力はアプリ共通の例外ハンドラで管理する
