# FEAT-001: 商品検索機能強化 設計書（アーキテクチャ）

## 1. 目的

- `docs/issues/FEAT-001-requirements.md` の要件を実現するためのシステム構成とコンポーネント責務を定義する。

## 2. 対象範囲

- ヘッダ検索ボックスからの検索要求
- 検索結果一覧画面の表示および絞り込み
- 検索条件の正規化、検索実行、SQL 生成

## 3. 全体構成

1. `header.html` の検索フォーム
   - `q` パラメータで検索キーワードを送信
2. `CatalogController.searchResults(...)`
   - 画面リクエストを受け取り、検索条件を組み立て
3. `ProductListSearchService`
   - 画面入力値の正規化と `ProductSearchCondition` 生成
4. `ProductService` / `ProductRepository`
   - 検索条件に基づく商品検索実行
5. `ProductMapper.xml`
   - SQL で検索条件を組み立て、結果を返却
6. 検索結果画面 (`product-list-search-results.html`)
   - 絞り込み UI、検索結果、0 件メッセージを表示

## 4. 検索条件の責務

- `ProductListSearchService` は画面入力を正規化し、カテゴリ固有フィルタや絞り込み用 ID リストを構築する。
- `ProductService` は `ProductSearchCondition` を `ProductRepository` に渡し、検索実行前後の整合性を担保する。
- `ProductRepository` は検索条件を SQL パラメータに変換し、既存のカテゴリフィルタロジックと統合する。

## 5. 画面と検索ロジックの分離

- UI 側は `q`, `inStockOnly`, `priceBand`, `color`, `sort`, `page`, `size`, `taste` 相当パラメータを送信する。
- サービス層はこれらを `ProductSearchCondition` に変換し、`keyword` と `ProductCategoryFilter` を分離して保持する。

## 6. 検索対象フィールド

- 商品コード: 完全一致 / 前方一致
- 商品名 / シリーズバリエーション名 / 説明文: 部分一致
- 上記チェックは OR 条件で結合し、キーワードによる検索対象も AND 検索となる。

## 7. 絞り込みと組み合わせ

- 「テイスト」フィルタは検索キーワードと AND 条件で組み合わせる。
- 検索処理自体はキーワードの各トークンを AND で結合して各対象フィールドに適用する。

## 8. 既存機能との統合

- 現行の検索結果スクロール、ページング、ソート、価格・カラー・在庫絞り込みはそのまま利用する。
- 本機能強化では、既存の検索条件生成および SQL 構造を拡張する形で実装する。

## 9. 追加設計ドキュメント

- `FEAT-001-search-query-spec.md`: 検索クエリ仕様
- `FEAT-001-search-ui-spec.md`: UI/UX 仕様
- `FEAT-001-search-test-plan.md`: テスト仕様
