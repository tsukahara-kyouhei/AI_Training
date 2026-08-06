# FEAT-001: 商品検索機能強化 TODO リスト

## 1. 概要

`docs/issues/FEAT-001-requirements.md` と各設計書をもとに、実装と単体テストのための作業を整理する。

## 2. 実装対象

- 検索キーワードの正規化と検索条件生成
- 商品コード検索（完全一致 / 前方一致）とテキスト検索（部分一致）の組み合わせ
- 検索結果画面の「テイスト」絞り込み追加
- 検索結果画面の AND / OR 論理整理
- UI 表示と 0 件メッセージ確認
- 単体テストの追加

## 3. 変更予定ファイル

- `src/main/java/jp/co/skig/officeorder/web/CatalogController.java`
- `src/main/java/jp/co/skig/officeorder/service/product/ProductListSearchService.java`
- `src/main/java/jp/co/skig/officeorder/service/product/ProductService.java`
- `src/main/java/jp/co/skig/officeorder/model/product/ProductSearchCondition.java`
- `src/main/java/jp/co/skig/officeorder/service/product/ProductFilterOptionService.java`
- `src/main/java/jp/co/skig/officeorder/repository/ProductRepository.java`
- `src/main/resources/mappers/ProductMapper.xml`
- `src/main/resources/templates/pages/product-list-search-results.html`
- テストクラス（`src/test/java/...`）

## 4. 実装 TODO

### 4.1 バックエンド実装

1. `ProductSearchCondition` に検索結果ページ専用の `taste` 絞り込み条件を追加する。
   - 既存のカテゴリ固有フィルタとは別に、検索結果全体で扱える汎用 `searchTasteIds` を検討する。
2. `ProductListSearchService` の `buildCondition(...)` に、検索結果用の `rawTasteIds` を受け取る引数を追加する。
   - `CatalogController.searchResults(...)` から `taste` パラメータを受け取れるように変更する。
3. `ProductFilterOptionService` に検索結果ページ用のテイスト候補取得メソッドを追加する。
   - 既存カテゴリ一覧のテイスト候補取得ロジックを再利用する。
   - `taste` の正規化関数を追加し、不正な ID を排除する。
4. 検索キーワードの正規化ロジックを実装する。
   - 前後空白削除、全角スペースを半角、連続スペースの正規化
   - 英字小文字化
   - 全角数字・英字・カタカナの半角化
   - 空文字/null の無視
   - 空白区切りでトークン分割し、複数語は AND 検索とする
5. `ProductRepository.buildSearchParams(...)` を拡張する。
   - キーワード検索用の SQL パラメータを構築する。
   - 各トークンについて、商品コード条件（完全一致 / 前方一致）とテキスト条件（部分一致）を OR で結合する。
   - `keywordLike` と `normalizedKeyword` など、必要なパラメータを追加する。
   - 検索結果ページの `taste` 絞り込み条件を SQL パラメータに含める。
6. `ProductMapper.xml` の検索 SQL を修正する。
   - 商品コード検索とテキスト検索を設計どおりの論理で組み立てる。
   - 検索結果ページのテイスト絞り込みを追加する。
   - categoryId が null の場合でもテイスト絞り込みが動作するようにする。
7. `ProductService` の `search` 呼び出しおよび `normalize(...)` 周りの整合性を確認する。
   - 既存の検索条件構築ロジックとの互換性を保つ。

### 4.2 UI 実装

1. `product-list-search-results.html` に「テイスト」絞り込みセクションを追加する。
   - 既存カテゴリ一覧のチェックボックス UI と同じ操作感を目指す。
   - `taste` パラメータをフォームに含める。
2. 検索結果画面のサイドバー / モバイルフィルターにテイスト選択を反映する。
3. キーワード検索とテイスト絞り込みの組み合わせが URL に反映されることを確認する。
4. 0 件時メッセージを要件どおりに確認する。
   - 現在の文言が `該当する商品は見つかりませんでした。` の場合、要件の `該当する商品がありませんでした。` に揃えるか確認する。

### 4.3 テスト実装

1. 単体テスト: 検索キーワード正規化ロジック
   - スペース、全角半角、英字大小、複数語 AND 条件を検証する。
2. 単体テスト: `ProductFilterOptionService` の `taste` 正規化
   - 不正値除去、複数選択、既存候補との整合性。
3. 単体テスト: `ProductRepository.buildSearchParams(...)`
   - キーワードパラメータと検索 SQL パラメータの正しさを検証する。
   - `searchTasteIds` が含まれる場合のパラメータ生成。
4. 単体テスト / 結合テスト: `CatalogController.searchResults(...)`
   - `q` と `taste` の組み合わせが検索条件に渡ること。
5. 結合テスト: 検索結果ページでテイストチェックボックス表示と選択状態を検証する。
6. 境界値テスト: 0 件表示、複数語 AND 検索、商品コード前方一致 / 完全一致、部分一致検索。
7. 性能確認（実装後）
   - 最大 1000 件程度で 95% ケース 2 秒以内を目標とする。

## 5. 注意点

- 既存のカテゴリ検索ロジックを大きく変更しないよう、検索結果用の拡張は追加モジュールとして実装する。
- テイスト絞り込みが検索結果ページの全カテゴリを対象に動作するかを特に注意する。
- SQL の AND / OR 構成ミスが誤検索につながるため、検索論理をテストで明確に確認する。
