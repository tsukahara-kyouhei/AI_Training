# FEAT-001 実行計画 TODO

## 概要
商品キーワード検索機能改善（FEAT-001）の実装タスク一覧。

---

## タスク一覧

### Phase 1: ユーティリティ・モデル層

- [x] **`common/KeywordNormalizer.java`** 新規作成
  - 全角英数字 → 半角変換
  - 半角カタカナ → 全角カタカナ変換（濁点・半濁点合成を含む）

- [x] **`model/product/ProductSearchCondition.java`** 変更
  - `List<Integer> tasteIds` フィールドをコンポーネント設計書の位置（`colorIds` の直後）に追加
  - 既存の全コンストラクタ呼び出し箇所を更新

- [x] **`model/product/ProductFilterOptionsBundle.java`** 変更
  - `List<CategoryFilterOption> searchTasteOptions` フィールドを末尾に追加
  - compact コンストラクタに `searchTasteOptions = immutableOrEmpty(searchTasteOptions)` を追加

---

### Phase 2: サービス・リポジトリ層

- [x] **`service/product/ProductFilterOptionService.java`** 変更
  - `loadOptionsBundle()` に `searchTasteOptions` ロード処理を追加（`desk_tastes` を代表使用）
  - `normalizeSearchTasteIds(List<Integer>, ProductFilterOptionsBundle)` メソッドを追加

- [x] **`service/product/ProductService.java`** 変更
  - `buildCondition`（最終オーバーロード）に `List<Integer> tasteIds` 引数を追加
  - `ProductSearchCondition` コンストラクタ呼び出しに `tasteIds` を追加
  - `normalize` メソッドのコンストラクタ呼び出しに `condition.tasteIds()` を追加
  - `buildNewArrivalCondition` の内部呼び出しに `List.of()` を追加

- [x] **`service/product/ProductListSearchService.java`** 変更
  - `buildCondition` に `List<Integer> rawTasteIds` パラメータ付きオーバーロードを追加
  - `searchWithPageCorrection` 内の `ProductSearchCondition` コンストラクタ呼び出しに `tasteIds` を追加

- [x] **`repository/ProductRepository.java`** 変更
  - `normalizeKeyword(String)` private メソッド追加
  - `toKeywordLike(String)` を `normalizeKeyword` を使うよう変更
  - `toKeywordExact(String)` / `toKeywordPrefix(String)` private メソッド追加
  - `buildSearchParams` に `keywordExact`・`keywordPrefix`・`tasteIds`・`hasSearchTasteFilter` パラメータ追加
  - `findNewestProducts` の `ProductSearchCondition` コンストラクタに `List.of()` を追加

---

### Phase 3: Web 層

- [x] **`web/CatalogController.java`** 変更
  - `searchResults` に `@RequestParam(name = "taste") List<Integer> rawTasteIds` を追加
  - `buildCondition` → `buildSearchCondition`（tasteIds 付きオーバーロード）へ変更
  - `model.addAttribute("selectedTasteIds", ...)` および `tasteOptions` 追加

---

### Phase 4: Mapper・テンプレート層

- [x] **`resources/mappers/ProductMapper.xml`** 変更
  - `BaseProductWhere` のキーワード条件を拡張（`variation_name`・`description` 追加、商品コード一致方式変更）
  - `SearchTasteFilter` フラグメント追加（デスク・チェア・ストレージ横断 OR 結合）

- [x] **`templates/pages/product-list-search-results.html`** 変更
  - サイドバー絞込フォームにテイスト絞込 UI 追加
  - ソート・件数フォームに `taste` hidden input 追加
  - ページネーション全リンク（前へ・1ページ目・中間・最終・次へ）に `taste=${selectedTasteIds}` 追加

---

### Phase 5: DDL

- [x] **`sql/schema/products_search_idx.sql`** 新規作成
  - `CREATE EXTENSION IF NOT EXISTS pg_trgm`
  - `products.description` / `products.product_name` / `products.variation_name` への GIN インデックス作成
  - `sql/init/init.sql` に `\ir schema/products_search_idx.sql` を追加

---

## 実装ステータス

| Phase | ステータス |
|-------|---------|
| Phase 1: ユーティリティ・モデル層 | ✅ 完了 |
| Phase 2: サービス・リポジトリ層 | ✅ 完了 |
| Phase 3: Web 層 | ✅ 完了 |
| Phase 4: Mapper・テンプレート層 | ✅ 完了 |
| Phase 5: DDL | ✅ 完了 |
