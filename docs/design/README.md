# 設計書一覧

## 1. 目的

本ディレクトリでは、要件定義書に基づき、商品検索機能強化を実装するための設計書を管理する。

## 2. 対象機能

- ヘッダー検索ボックスからの検索
- 検索結果一覧画面の表示
- 検索結果一覧の絞り込み条件としての「テイスト」追加
- 検索性能・表示件数・0件時表示の設計

## 3. 作成済み設計書

1. [FEAT-001-search-design.md](FEAT-001-search-design.md)
   - 検索ロジック、正規化、ソート、ランキング、SQL方針を定義する。
2. [FEAT-001-ui-design.md](FEAT-001-ui-design.md)
   - ヘッダー検索、検索結果一覧、絞り込みUI、0件時メッセージを定義する。
3. [FEAT-001-data-design.md](FEAT-001-data-design.md)
   - 検索対象データ、パラメータ、SQL、ページネーション、テイスト絞り込みのデータ設計を定義する。
4. [FEAT-001-test-design.md](FEAT-001-test-design.md)
   - 単体テスト・統合テスト・受け入れテストの観点を定義する。

## 4. 実装対象の既存要素

- コントローラ: CatalogController
- 検索条件整形: ProductListSearchService
- 商品検索サービス: ProductService
- リポジトリ: ProductRepository
- Mapper: ProductMapper
- 画面テンプレート: header.html / product-list-search-results.html

## 5. 設計方針

- 既存の一覧・絞り込み構成を活かしつつ、検索機能を拡張する。
- 正規化処理はアプリケーション側とDB側で一貫させる。
- 商品コード検索は完全一致/前方一致に限定し、中間一致は許可しない。
- ユーザー体験を優先し、0件時・空入力時・ページネーションを明確に扱う。
