# FEAT-001 バックエンド詳細設計書（概要）

| 項目     | 内容                              |
| -------- | --------------------------------- |
| Issue ID | FEAT-001                          |
| 作成日   | 2026-04-27                        |
| 対象層   | Controller / Service / Repository |

## レイヤー別設計書

| 設計書                                                         | 対象クラス                                                                   |
| -------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| [FEAT-001-controller-design.md](FEAT-001-controller-design.md) | `CatalogController`                                                          |
| [FEAT-001-service-design.md](FEAT-001-service-design.md)       | `ProductFilterOptionService` / `ProductListSearchService` / `ProductService` |
| [FEAT-001-repository-design.md](FEAT-001-repository-design.md) | `ProductRepository`                                                          |

---

## 1. 変更コンポーネント一覧

| コンポーネント               | 変更種別 | 概要                                                              |
| ---------------------------- | -------- | ----------------------------------------------------------------- |
| `CatalogController`          | 修正     | テイストパラメータ受取・NFKC正規化・バリデーション・0件時処理追加 |
| `ProductFilterOptionService` | 修正     | 検索用テイストフィルタ組み立てメソッド追加                        |
| `ProductListSearchService`   | 変更なし | カテゴリフィルタを渡す既存オーバーロードを利用                    |
| `ProductRepository`          | 修正     | `keyword`/`keywordLike` 2パラメータ分離・テイスト検索フラグ追加   |
| `ProductService`             | 修正     | 0件時おすすめ商品取得メソッド追加                                 |

---

## 2. 処理フロー（シーケンス）

```
ブラウザ
  │  GET /products/search?q=...&deskTaste=1&chairTaste=2
  ▼
CatalogController.searchResults()
  │  1. keyword NFKC 正規化
  │  2. 100文字バリデーション → エラー時は即返却（空結果＋エラーメッセージ）
  │  3. optionsBundle = productFilterOptionService.loadOptionsBundle()
  │  4. tasteFilter = productFilterOptionService.buildSearchTasteFilter(...)
  │  5. condition = productListSearchService.buildCondition(..., tasteFilter, ...)
  ▼
ProductListSearchService.buildCondition()
  │  既存ロジック（価格帯・カラー正規化）＋ tasteFilter をそのまま渡す
  ▼
ProductRepository.search(condition)
  │  buildSearchParams() でパラメータマップに変換
  │   - keyword（生値・trimのみ）、keywordLike（% 囲み）、hasTasteSearchFilter
  ▼
ProductMapper.countProducts / selectProducts（SQL詳細: FEAT-001-sql-design.md）
  ▼
CatalogController（結果受取後）
  │  totalCount == 0 の場合:
  │    suggestedProducts = productService.getTopRankedProducts(8)
  │  model に検索結果 + suggestedProducts + keyword + deskTasteIds 等を設定
  ▼
テンプレート返却（pages/product-list-search-results）
```

詳細は各レイヤー別設計書を参照。
