# FEAT-001 商品検索機能強化 基本設計書

## 1. 概要

要件定義書 `docs/issues/FEAT-001-requirements.md` に基づき、商品検索機能を以下の 4 点について強化する。

| # | 対応内容 | 区分 |
|---|---------|------|
| 1 | 商品コード検索を部分一致から完全一致・前方一致に変更 | 変更 |
| 2 | 検索対象フィールドにバリエーション名・商品紹介文を追加 | 追加 |
| 3 | 検索キーワードの全角・半角正規化 | 追加 |
| 4 | 検索結果一覧にテイスト絞り込みを追加 | 追加 |

---

## 2. 変更対象コンポーネント一覧

| レイヤ | クラス / ファイル | 変更概要 |
|--------|-----------------|---------|
| Controller | `CatalogController` | `searchResults()` にテイストパラメータを追加 |
| Service | `ProductListSearchService` | キーワード正規化処理の追加、テイスト名の正規化 |
| Service | `ProductFilterOptionService` | 統合テイスト選択肢取得メソッドの追加 |
| Model | `ProductSearchCondition` | `tasteNames` フィールドの追加 |
| Repository | `ProductRepository` | `buildSearchParams()` の拡張（キーワードパラメータ分割、テイスト名パラメータ追加） |
| Repository | `ProductFilterOptionRepository` | 統合テイスト選択肢取得メソッドの追加 |
| Mapper XML | `ProductMapper.xml` | キーワード検索 SQL の変更、テイスト絞り込み SQL の追加、テイスト選択肢取得クエリの追加 |
| Template | `product-list-search-results.html` | テイスト絞り込み UI の追加 |

---

## 3. アーキテクチャ概要

本システムは Spring Boot + MyBatis + Thymeleaf の構成を採用している。
本機能での新規レイヤ追加はなく、既存レイヤの拡張で対応する。

```
ブラウザ
  |
  |  GET /products/search?q=...&taste=...
  v
CatalogController
  |  searchResults()
  v
ProductListSearchService          ProductFilterOptionService
  buildCondition()                  loadUnifiedSearchTasteOptions()
  normalizeSearchKeyword()          normalizeSearchTasteNames()
  |                                 |
  v                                 v
ProductSearchCondition           List<CategoryFilterOption>（統合テイスト）
  (keyword=正規化済み, tasteNames=[...])
  |
  v
ProductRepository
  buildSearchParams()
  |  keywordLike / keywordExact / keywordPrefix
  |  searchTasteNames / hasSearchTasteFilter
  v
ProductMapper.xml
  BaseProductWhere（キーワード条件変更）
  SearchTasteFilter（新規フラグメント）
  |
  v
PostgreSQL
```

---

## 4. 各機能の設計方針

### 4.1 商品コード検索（部分一致 → 完全一致・前方一致）

現状の `product_code ILIKE #{keywordLike}` を、以下に置き換える。

```
product_code = #{keywordExact}           -- 完全一致（大文字小文字区別なし → ILIKE との等価）
OR product_code ILIKE #{keywordPrefix}   -- 前方一致
```

`ProductRepository.buildSearchParams()` でキーワード関連パラメータを 3 つに分割する。

| パラメータ名 | 値の生成ルール |
|-------------|--------------|
| `keywordLike` | `%` + 正規化済みキーワード + `%` |
| `keywordExact` | 正規化済みキーワード（そのまま） |
| `keywordPrefix` | 正規化済みキーワード + `%` |

### 4.2 検索対象フィールド拡張

`products.variation_name` と `products.description` を検索対象に追加する。
`variation_name` が NULL の商品はバリエーション名検索対象外（`ILIKE` は NULL を無視するため追加実装不要）。

### 4.3 全角・半角正規化

- **方向：** 半角 → 全角（要件定義 §4.4 の確定済み仕様）
- **実施箇所：** `ProductListSearchService` 内に新設する `normalizeSearchKeyword()` メソッド
- **対象文字種：** 半角数字、半角英字、半角カタカナ
- **DB側の対応：** SQL 内でカラム値を同一基準（全角）に変換した上で比較する。
  具体的な実装方式（PostgreSQL カスタム関数 vs 他の方式）は詳細設計フェーズで確定する（未決事項 #1）。

### 4.4 テイスト絞り込み（検索結果画面）

- テイスト選択肢は 3 カテゴリ（デスク・チェア・収納）のマスタを **display_name で名称統合**して表示する。
- URL クエリパラメータは `taste`（display_name の文字列リスト）とする。
- 絞り込み SQL は各カテゴリの属性テーブルを `display_name` で JOIN し、`OR EXISTS` で結合する。
- 選択状態はクエリパラメータで管理し、再検索・ページ遷移後も保持する。

---

## 5. データフロー（テイスト絞り込みの例）

```
[ブラウザ] ?q=デスク&taste=ナチュラル&taste=ベーシック
    |
    v
CatalogController.searchResults()
  rawTasteNames = ["ナチュラル", "ベーシック"]
    |
    v
ProductFilterOptionService.normalizeSearchTasteNames()
  validOptions = loadUnifiedSearchTasteOptions()
  → ホワイトリスト照合 → ["ナチュラル", "ベーシック"]（有効なもののみ）
    |
    v
ProductListSearchService.buildCondition()
  keyword = normalizeSearchKeyword("デスク")
  ProductSearchCondition(keyword="デスク", tasteNames=["ナチュラル","ベーシック"])
    |
    v
ProductRepository.buildSearchParams()
  keywordLike = "%デスク%"
  keywordExact = "デスク"
  keywordPrefix = "デスク%"
  searchTasteNames = ["ナチュラル","ベーシック"]
  hasSearchTasteFilter = true
    |
    v
ProductMapper.xml / BaseProductWhere
  AND (
    normalize_fullwidth(p.product_name) ILIKE '%デスク%'
    OR normalize_fullwidth(p.variation_name) ILIKE '%デスク%'
    OR normalize_fullwidth(p.description) ILIKE '%デスク%'
    OR EXISTS ( ... product_code = 'デスク' OR product_code ILIKE 'デスク%' )
  )
  AND (
    EXISTS ( SELECT 1 FROM product_desk_attributes da JOIN desk_tastes dt ON ... WHERE dt.display_name IN ('ナチュラル','ベーシック') )
    OR EXISTS ( SELECT 1 FROM product_chair_attributes ca JOIN chair_tastes ct ON ... WHERE ct.display_name IN ('ナチュラル','ベーシック') )
    OR EXISTS ( SELECT 1 FROM product_storage_attributes sa JOIN storage_tastes st ON ... WHERE st.display_name IN ('ナチュラル','ベーシック') )
  )
```

---

## 6. 未決事項（引き継ぎ）

| # | 事項 | 確認先 |
|---|------|--------|
| 1 | 全角・半角正規化の実装方法（PostgreSQL カスタム関数 vs Javaユーティリティ + OR 条件 vs 他）。正規化方向は全角統一で確定。 | 実装担当者 |
