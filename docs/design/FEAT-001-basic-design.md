# FEAT-001 基本設計書 — 商品検索機能強化

| 項目 | 内容 |
|------|------|
| 課題ID | FEAT-001 |
| 作成日 | 2026-03-15 |
| ステータス | Draft |
| 関連要件 | [FEAT-001-requirements.md](../issues/FEAT-001-requirements.md) |

---

## 1. 変更概要

ヘッダ検索ボックスによる商品キーワード検索について、以下3点の機能追加・変更を行う。

| # | 変更区分 | 内容 |
|---|---------|------|
| 1 | 変更 | 商品コード検索を「完全一致 or 前方一致」に限定し、入力桁数で自動判定する |
| 2 | 追加 | キーワード検索の対象フィールドに `variation_name`・`description` を追加する |
| 3 | 追加 | 英字の大文字小文字、数字・英字・カタカナの全角半角を区別しない正規化処理を導入する |
| 4 | 追加 | 検索結果画面の絞込条件に「テイスト」を追加する |

---

## 2. アーキテクチャ概要

本システムは以下の多層アーキテクチャで構成されている。今回の変更が及ぶ層を示す。

```
[Browser]
   │  HTTP GET /products/search?keyword=...&taste=...
   ▼
[Controller層]            CatalogController              ★変更あり
   │
   ▼
[Service層]               ProductService                  ★変更あり
                          ProductListSearchService         ★変更あり
                          ProductFilterOptionService       ★変更あり
   │
   ▼
[Repository層]            ProductRepository               ★変更あり
                          ProductFilterOptionRepository    ★変更あり
   │
   ▼
[Mapper層]                ProductMapper (Interface)        ★変更あり
                          ProductMapper.xml               ★変更あり
   │
   ▼
[DB]                      products, product_variants       変更なし
                          product_*_attributes             変更なし
                          desk_tastes / chair_tastes       変更なし
                          storage_tastes                   変更なし
```

DB スキーマの変更はなし。既存テーブル・カラムのまま対応する。

---

## 3. 変更コンポーネント一覧

### 3.1 新規追加

| コンポーネント | パッケージ | 役割 |
|-------------|---------|------|
| `SearchKeywordNormalizer` | `jp.co.skig.officeorder.util` | キーワードの正規化処理（全角→半角、大文字→小文字）を担う静的ユーティリティクラス |

### 3.2 変更（既存クラスへの追加・修正）

| コンポーネント | ファイルパス | 変更内容の概要 |
|-------------|-----------|-------------|
| `ProductSearchCondition` | `model/product/ProductSearchCondition.java` | テイスト名リスト（`tasteNames`）フィールドを追加 |
| `ProductFilterOptionsBundle` | `model/product/ProductFilterOptionsBundle.java` | 検索結果画面用テイスト選択肢（`searchTasteOptions`）フィールドを追加 |
| `ProductListSearchService` | `service/product/ProductListSearchService.java` | キーワード正規化呼び出し・テイストパラメータのバインドを追加 |
| `ProductFilterOptionService` | `service/product/ProductFilterOptionService.java` | 全カテゴリのテイスト選択肢を重複排除して返すメソッドを追加 |
| `ProductFilterOptionRepository` | `repository/ProductFilterOptionRepository.java` | 全カテゴリテイストマスタを一括取得するメソッドを追加 |
| `ProductRepository` | `repository/ProductRepository.java` | テイスト名リストを検索パラメータに追加 |
| `ProductMapper` (Interface) | `mapper/ProductMapper.java` | 変更なし（XML側のSQLフラグメントで対応） |
| `ProductMapper.xml` | `resources/mappers/ProductMapper.xml` | キーワード検索SQL変更・テイスト絞込フラグメント追加 |
| `CatalogController` | `web/CatalogController.java` | テイストパラメータの受取・テイスト選択肢のモデル追加 |
| `product-list-search-results.html` | `templates/pages/product-list-search-results.html` | テイスト絞込UIの追加 |

### 3.3 変更なし

- `ProductService` — 呼び出し構造に変更なし
- `ProductCardView`・`ProductDetailView` など表示モデル — 変更なし
- DB スキーマ全般 — 変更なし
- カテゴリ別一覧ページ（desk/chair/storage）— 変更なし

---

## 4. 処理フロー

### 4.1 キーワード検索のリクエストフロー

```
1. ユーザーがヘッダ検索ボックスにキーワードを入力し、検索を実行する
2. GET /products/search?keyword=<input>&taste=<tasteNameA>,<tasteNameB>&...

3. CatalogController.searchResults()
   └─ ProductListSearchService.buildCondition() を呼び出す
        ├─ SearchKeywordNormalizer.normalize(keyword)
        │    全角数字・英字・カタカナ → 半角
        │    英字 → 小文字
        │    ※正規化済みキーワードを以降で使用する
        ├─ 商品コード検索方式の判定
        │    keyword.length() >= 9 → 完全一致フラグ = true
        │    keyword.length() <  9 → 前方一致フラグ = true
        └─ ProductSearchCondition を構築（tasteNames を含む）

4. ProductService.search(condition) を呼び出す
   └─ ProductRepository.search(condition) を呼び出す
        └─ ProductMapper.countProducts / selectProducts を呼び出す
             ├─ キーワードが存在する場合:
             │    ├─ 商品コード: 完全一致 or 前方一致
             │    └─ 商品名・シリーズバリエーション名・説明文: 部分一致
             │         ※DB側も同じ正規化関数で変換した列と比較（後述）
             └─ テイスト名が指定されている場合:
                  各カテゴリ属性テーブルをOR条件で絞り込む

5. 検索結果をレスポンスとして返す
```

### 4.2 テイスト選択肢ロードフロー

```
1. CatalogController.searchResults() の初期化時
2. ProductFilterOptionService.loadSearchTasteOptions() を呼び出す
   └─ ProductFilterOptionRepository.findAllTasteOptions() を呼び出す
        ├─ desk_tastes を全件取得
        ├─ chair_tastes を全件取得
        └─ storage_tastes を全件取得
3. display_name で重複排除し、5種のテイスト名リストを返す
4. テンプレートへ `searchTasteOptions` としてバインドする
```

---

## 5. キーワード正規化方針

### 5.1 正規化の対象と変換内容

| 対象文字種 | 変換内容 | 例 |
|----------|---------|-----|
| 全角数字（０-９） | 半角数字（0-9）へ変換 | `１２３` → `123` |
| 全角英大文字（Ａ-Ｚ） | 半角英小文字（a-z）へ変換 | `ＡＢＣ` → `abc` |
| 全角英小文字（ａ-ｚ） | 半角英小文字（a-z）へ変換 | `ａｂｃ` → `abc` |
| 半角英大文字（A-Z） | 半角英小文字（a-z）へ変換 | `ABC` → `abc` |
| 全角カタカナ（ア-ン） | 半角カタカナ（ｱ-ﾝ）へ変換 | `ナチュラル` → `ﾅﾁｭﾗﾙ` |
| ひらがな | 変換しない（対象外） | `なちゅらる` → そのまま |

### 5.2 DBカラム側の正規化

キーワードを Java で正規化しても、DB の列値が全角のまま格納されている場合は一致しない。そのため、SQL の WHERE 句において DB 列側も正規化関数で変換した上で比較する。

PostgreSQL の `TRANSLATE()` を用いて全角→半角変換を行う SQL ヘルパー関数  
`normalize_search_text(text)` を新規に作成し、以下のように使用する。

```sql
-- キーワード比較（商品名の例）
LOWER(normalize_search_text(p.product_name)) LIKE LOWER(#{normalizedKeyword})
```

`normalize_search_text` の定義はスキーマ管理スクリプトに追加する  
（`sql/schema/` 配下に新規SQLファイルとして追加）。

---

## 6. DB 変更有無

| 対象 | 変更有無 | 内容 |
|------|---------|------|
| テーブル定義 | なし | — |
| カラム追加 | なし | — |
| インデックス | 設計者判断 | 正規化関数を使用する場合、関数インデックスの追加を検討する（性能要件達成のため） |
| PostgreSQL 関数 | **あり** | `normalize_search_text(text)` を新規追加 |

---

## 7. 性能方針

- 検索対象は最大 1,000 件程度であるため、全件スキャンでも 2 秒以内を目標として許容する。
- ただし `normalize_search_text()` を使用した LIKE 検索は通常のインデックスが効かなくなるため、必要に応じて**式インデックス（関数インデックス）** を作成する。
  - 対象列候補: `product_variants.product_code`, `products.product_name`, `products.variation_name`, `products.description`
- インデックス追加の要否は実装後の負荷テスト結果を踏まえて判断する。
