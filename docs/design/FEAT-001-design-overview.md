# FEAT-001 商品検索機能強化 設計概要

## 1. 文書情報

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| タイトル | 商品検索機能強化 |
| 作成日 | 2026-04-02 |
| 対応要件 | ../issues/FEAT-001-requirements.md |

---

## 2. 設計書一覧

| 文書 | 目的 |
|------|------|
| FEAT-001-design-overview.md | 全体方針、責務分割、変更対象一覧 |
| FEAT-001-application-design.md | Java アプリケーション層の詳細設計 |
| FEAT-001-sql-design.md | MyBatis / SQL / 検索ロジック詳細設計 |
| FEAT-001-ui-test-design.md | 画面設計、リクエスト・Model 項目、試験観点 |
| FEAT-001-screen-transition.md | 画面遷移図、主要遷移パターン、パラメータ引継ぎ方針 |

---

## 3. 設計方針

### 3.1 基本方針

- 既存の Controller → Service → Repository → MyBatis XML の責務分離を維持する。
- カテゴリ別一覧で利用している `ProductCategoryFilter` を、検索結果画面のテイスト絞込にも再利用する。
- `ProductSearchCondition` の構造は変更せず、検索キーワードは `keyword`、カテゴリ横断テイスト絞込は `categoryFilter` に格納する。
- キーワード正規化とスペース分割は Repository 層の SQL パラメータ変換責務に集約する。
- テイスト名から各カテゴリの taste_id への変換は Service 層で行い、MyBatis には ID 一覧だけを渡す。

### 3.2 変更しない方針

- モバイル絞込モーダルは現状の簡易 UI のままとする。
- DB 側の正規化関数・正規化カラムは追加しない。
- 検索結果画面にカテゴリ絞込は追加しない。

---

## 4. 変更対象レイヤ

| レイヤ | 主対象 | 変更概要 |
|--------|--------|---------|
| Web | `CatalogController` | `taste` パラメータ受け取り、Model 項目追加 |
| Service | `ProductFilterOptionService` | 検索結果画面向けテイスト候補生成、テイスト名→ID 変換 |
| Service | `ProductListSearchService` | 検索結果画面で `categoryFilter` を受け取る既存オーバーロードを使用 |
| Repository | `ProductRepository` | キーワード正規化、複数ワード分割、検索用パラメータ生成 |
| Mapper XML | `ProductMapper.xml` | 複数ワード AND 検索、商品コード前方一致、SearchTasteFilter 追加 |
| Template | `product-list-search-results.html` | テイスト絞込 UI、 hidden パラメータ、ページネーション引継ぎ |

---

## 5. 全体処理フロー

```text
ヘッダ検索ボックス
  ↓ GET /products/search?q=...&taste=...
CatalogController#searchResults
  ↓
ProductFilterOptionService
  - loadOptionsBundle()
  - buildSearchTasteOptions()
  - buildSearchTasteFilter()
  ↓
ProductListSearchService#buildCondition(..., ProductCategoryFilter, ...)
  ↓
ProductService#buildCondition(...)
  ↓
ProductRepository#buildSearchParams()
  - normalize keyword
  - split to keywordWords / keywordPrefixWords
  - build hasSearchTasteFilter
  ↓
ProductMapper.xml
  - BaseProductWhere
  - SearchTasteFilter
  ↓
検索結果一覧描画
```

---

## 6. 設計上の主要判断

### 6.1 ProductCategoryFilter を再利用する理由

- 既存の `deskTasteIds` / `chairTasteIds` / `storageTasteIds` をそのまま使える。
- Repository / Mapper 側のカテゴリ別テイスト条件と整合が取れる。
- `ProductSearchCondition` の変更が不要になり、影響範囲を抑えられる。

### 6.2 テイスト候補は Service で統合する理由

- 既に `ProductFilterOptionsBundle` に各カテゴリの有効テイスト候補が読み込まれている。
- 検索結果画面用の統合候補は、Repository を追加せず Service で重複排除できる。
- リクエスト値のバリデーションと UI 候補生成を同一責務にまとめられる。

### 6.3 キーワードの複数ワード化を Repository で行う理由

- `ProductSearchCondition` は UI 入力値を保持する責務に留める。
- SQL 実行直前に `keywordWords` / `keywordPrefixWords` を作ることで、MyBatis パラメータ構造を一元化できる。

---

## 7. 成果物の粒度

この設計で、実装担当は以下をそのまま着手できる状態にする。

- 追加・変更メソッド一覧
- Controller の受け取りパラメータと Model 項目
- SQL パラメータ名と MyBatis 条件分岐
- 画面 hidden 項目とページネーション引継ぎ項目
- 試験項目と期待結果

---

## 8. 実装順序

1. `ProductFilterOptionService` に検索結果画面向けテイスト統合・変換機能を追加
2. `CatalogController` で `taste` パラメータを受け取るよう変更
3. `ProductRepository` でキーワード分割・正規化・検索パラメータ生成を実装
4. `ProductMapper.xml` に `keywordWords` / `keywordPrefixWords` / `SearchTasteFilter` を追加
5. `product-list-search-results.html` の絞込 UI とパラメータ引継ぎを更新
6. 試験実施
