# FEAT-001 商品検索機能強化 要件定義書

作成日: 2026-04-17  
最終更新: 2026-04-17  
ステータス: Draft

---

## 1. 目的

商品検索のヒット率を上げ、離脱率を下げることを目的として、商品検索機能を強化する。

---

## 2. 対象範囲

| 対象 | 詳細 |
|------|------|
| ヘッダ検索ボックス | 全画面共通ヘッダに配置されているキーワード入力欄 |
| 検索結果一覧画面 | `GET /products/search`（テンプレート: `pages/product-list-search-results`） |

---

## 3. 用語定義

| 用語 | 定義 |
|------|------|
| 商品コード | `product_variants.product_code`（SKU 単位の管理コード、例: `DSK-1001-WH`） |
| 商品名 | `products.product_name`（商品単位の名称） |
| シリーズバリエーション名 | `products.variation_name`（バリエーション展開商品の枝番名称。`has_variation = TRUE` の商品のみ保持） |
| 説明文 | `products.description`（商品の説明テキスト） |
| テイスト | 各カテゴリ属性テーブルが持つインテリア様式の分類（`desk_tastes` / `chair_tastes` / `storage_tastes`） |
| 前方一致 | キーワードが文字列の先頭から一致すること（SQL: `LIKE 'keyword%'`） |
| 部分一致 | キーワードが文字列の任意の位置に含まれること（SQL: `LIKE '%keyword%'`） |
| 全角半角正規化 | 全角の数字・英字・カタカナを半角に統一する（または半角を全角に統一する）変換処理 |

---

## 4. 機能要件

### 4-1. 検索対象フィールドと一致方式

#### 現状

| 検索対象フィールド | 一致方式 |
|-----------------|---------|
| `products.product_name` | 部分一致（`%keyword%`） |
| `product_variants.product_code` | 部分一致（`%keyword%`） |

#### 変更後

| 検索対象フィールド | 一致方式 | 変更区分 |
|-----------------|---------|--------|
| `products.product_name` | 部分一致（`%keyword%`） | 変更なし |
| `products.variation_name` | 部分一致（`%keyword%`） | **追加** |
| `products.description` | 部分一致（`%keyword%`） | **追加** |
| `product_variants.product_code` | 前方一致（`keyword%`）または完全一致 | **変更** |

> **変更理由（商品コード）:** 部分一致では商品コードの中間に含まれる数字や記号が意図せず一致し、無関係な商品がヒットする問題が起きるため。前方一致はその懸念を排除しつつ、コードの先頭部分での絞り込みを可能にする。完全一致はその特殊ケースに含まれる（`keyword%` は `keyword` 自体にも一致するため、SQL 上は `keyword%` 1 条件で実装可）。

> **`variation_name` の NULL 考慮:** `has_variation = FALSE` の商品は `variation_name` が NULL となる。検索条件の OR 句に含める際は NULL を除外して評価すること（PostgreSQL では NULL に対する ILIKE は NULL を返すため、実害はないが明示的に `variation_name IS NOT NULL` を記述することが望ましい）。

#### 各フィールドの結合方式

上記 4 フィールドは **OR 条件** で結合する。いずれか 1 つ以上にキーワードが一致した場合、その商品を検索結果に含める。

#### 複数キーワード（スペース区切り）の扱い

- 入力キーワードを半角スペースおよび全角スペースで分割し、**各キーワードを AND 条件** で結合する。
- 例: `ナチュラル デスク` → 「ナチュラル」かつ「デスク」の両方にいずれかのフィールドでマッチする商品のみ表示。
- 各キーワードは上記の OR 条件（4 フィールド横断）で個別に評価され、そのキーワード単位の結果を AND で絞り込む。
- 実装: 正規化後の文字列を空白（半角・全角）で `split` してキーワードリストとし、Mapper XML の `<foreach>` で AND 結合する。キーワードが 1 つの場合は従来と同等の動作となる。

---

### 4-2. 大文字小文字の区別

- 英字については大文字・小文字を区別せず検索する。
- 実装: PostgreSQL の `ILIKE` 演算子を使用する（現状の商品コード・商品名にも既に適用済みのため、追加フィールドにも同様に適用する）。

---

### 4-3. 全角・半角の正規化

数字・英字・カタカナについては、全角・半角を区別せずに検索できること。

#### 対象文字種と正規化ルール

| 文字種 | 正規化方向 | 例 |
|-------|-----------|-----|
| 数字 | 全角 → 半角 | `１２３` → `123` |
| 英字（大文字・小文字） | 全角 → 半角 | `ＡＢＣ` / `ａｂｃ` → `ABC` / `abc` |
| カタカナ | 半角 → 全角 | `ﾃﾞｽｸ` → `デスク` |
| 記号（ハイフン・括弧等） | 全角 → 半角 | `－` → `-`、`（` → `(`、`）` → `)` |

#### 実装方針

1. **正規化は Java のサービス層で実施する。** 入力キーワードをサービスメソッドに渡す前（または `ProductRepository` に渡す前）に変換処理を適用し、正規化済みの文字列を SQL に渡す。
2. DB 側のカラム値は変更しない。そのため、DB 内のカタカナが全角で統一されていることを前提とする（現行データ投入 SQL との整合性を確認済みであること）。
3. 変換処理は共通ユーティリティとして `common/` パッケージに実装すること。

---

### 4-4. 検索結果一覧画面への「テイスト」絞込条件の追加

#### 概要

検索結果一覧画面（`/products/search`）の絞込パネルに「テイスト」フィルタを追加する。

#### テイストデータの構造

テイストは各カテゴリ専用のマスタテーブルとして分離されている。

| テーブル | カテゴリ | 結合先属性テーブル |
|---------|--------|----------------|
| `desk_tastes` | デスク | `product_desk_attributes.taste_id` |
| `chair_tastes` | チェア | `product_chair_attributes.taste_id` |
| `storage_tastes` | 収納家具 | `product_storage_attributes.taste_id` |

検索結果はカテゴリをまたぐため、3 つのテイストマスタを統合して扱う。

#### フィルタ選択肢の構築方針

- 各カテゴリのテイストマスタ（`desk_tastes` / `chair_tastes` / `storage_tastes`）から `is_active = TRUE` のレコードを取得し、**`display_name` の一致により重複を除去した統合テイスト一覧** をフィルタ選択肢として表示する。
- 選択肢の並び順は `sort_order ASC` → `display_name ASC` とする（各テーブルに `sort_order` がある場合はそれを優先）。
- 実装例: UNION + DISTINCT + ORDER BY による統合クエリ、または Java 側でマージして重複除去。

#### パラメータ仕様

| 項目 | 内容 |
|------|------|
| パラメータ名 | `taste`（`List<String>`） |
| 値の型 | テイストの `display_name`（文字列。カテゴリをまたぐ統合キー） |
| 複数選択 | 可（複数選択時は **OR 条件**） |
| URL 例 | `/products/search?q=デスク&taste=ナチュラル&taste=モダン` |

> `display_name` を用いる理由: 各カテゴリのテイストマスタは独立した連番 ID（`BIGINT GENERATED ALWAYS AS IDENTITY`）を持ち、カテゴリ横断で ID を共通化できないため。`display_name` がカテゴリ間の実質的なマッチングキーとなる。

> **キーワードなし・テイストのみの検索:** `q` が空または未指定の場合でも、`taste` パラメータ単独で検索を実行できる（在庫有・価格帯・カラー等の他フィルタも同様）。この場合、キーワード条件は SQL に追加せず、指定されたフィルタ条件のみで商品を絞り込む。

#### フィルタ適用の SQL 方針

テイストが選択された場合、以下の 3 つの EXISTS サブクエリを **OR 結合** で追加する（カテゴリ一覧の実装に準じる）。

```sql
AND (
  EXISTS (
    SELECT 1
    FROM product_desk_attributes da
    JOIN desk_tastes dt ON da.taste_id = dt.taste_id
    WHERE da.product_id = p.product_id
      AND dt.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1
    FROM product_chair_attributes ca
    JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
    WHERE ca.product_id = p.product_id
      AND ct.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1
    FROM product_storage_attributes sa
    JOIN storage_tastes st ON sa.taste_id = st.taste_id
    WHERE sa.product_id = p.product_id
      AND st.display_name IN (...)
  )
)
```

---

### 4-5. 現状踏襲の検索機能

本課題で変更しない機能は以下のとおり。

| 機能 | 内容 |
|------|------|
| 在庫有のみフィルタ | `inStockOnly` パラメータ |
| 価格帯フィルタ | `priceBand` パラメータ（6 段階） |
| カラーフィルタ | `color` パラメータ（複数選択可） |
| 並び順 | `sort` パラメータ（おすすめ / 新着 / 価格昇順 / 価格降順） |
| ページング | `page` / `size` パラメータ |

---

## 5. 性能要件

| 項目 | 要件 |
|------|------|
| 検索対象商品数 | 最大 1,000 件程度を想定 |
| 一覧初回表示のレスポンスタイム | 95 パーセンタイルで **2 秒以内** を目標とする |

### 性能対応方針

- `products.description` は `TEXT` 型であり、ILIKE による全文スキャンはデータ増加時に低速化するリスクがある。最大 1,000 件のスコープ内では全件スキャンでも許容範囲内と予想されるが、以下のインデックスを検討すること。

  | カラム | インデックス種別 | 備考 |
  |--------|---------------|------|
  | `products.product_name` | `pg_trgm` GIN インデックス（trigram） | ILIKE の高速化 |
  | `products.variation_name` | `pg_trgm` GIN インデックス（trigram） | ILIKE の高速化 |
  | `products.description` | `pg_trgm` GIN インデックス（trigram） | ILIKE の高速化（特に重要） |
  | `product_variants.product_code` | B-tree インデックス（既存 UNIQUE 制約） | 前方一致は B-tree で対応可 |

- `pg_trgm` 拡張は `CREATE EXTENSION IF NOT EXISTS pg_trgm;` で有効化が必要。初回適用時は DBA または インフラ担当者と確認すること。

---

## 6. 変更影響範囲（実装対象コンポーネント）

| レイヤー | コンポーネント | 変更内容 |
|---------|-------------|---------|
| Controller | `CatalogController` | `taste`（`List<String>`）パラメータを追加。`SearchForm` (または相当のクラス) に `tasteDisplayNames` フィールドを追加 |
| Service | `ProductService` | 入力キーワードの全角/半角正規化処理を追加 |
| Common | 新規ユーティリティクラス（`NormalizationUtils` 等） | 全角→半角変換（数字・英字）、半角→全角変換（カタカナ）の実装 |
| Repository | `ProductRepository` | taste フィルタ用選択肢取得メソッド（統合テイスト一覧）を追加。`toKeywordLike()` を複数キーワード対応に改修し、`product_code` 用前方一致パターン生成メソッド `toCodeLike()` を追加 |
| Mapper interface | `ProductMapper` | 統合テイスト一覧取得メソッドを追加 |
| Mapper XML | `ProductMapper.xml` | ① キーワード検索条件を修正：`product_code` には前方一致パターン（`keyword%`）用パラメータ `codeLike` を、`product_name` / `variation_name` / `description` には部分一致パターン（`%keyword%`）用パラメータ `keywordLike` を使用し **2 パラメータに分離する**。複数キーワード時は `<foreach>` で AND 結合 ② テイスト絞込条件を追加 ③ 統合テイスト一覧取得 SQL を追加 |
| Template | `product-list-search-results.html` | テイスト絞込 UI コンポーネントを追加（カテゴリ一覧と同様のチェックボックス形式） |

---

## 7. 除外事項・スコープ外

| 項目 | 理由 |
|------|------|
| 管理画面・受注後業務 | 本システムのスコープ外 |
| カテゴリ一覧画面（デスク / チェア / 収納）の検索機能 | ヘッダ検索ボックス経由の検索のみが対象範囲 |
| 全文検索エンジン（Elasticsearch 等）の導入 | 最大 1,000 件というスコープでは導入コストが不釣合い |
| サジェスト / オートコンプリート | 本課題の対象外（別課題で検討） |
| 同義語・表記ゆれ（ソファ/ソファー等）の吸収 | 本課題の対象外 |

---

## 8. 未決事項・要確認事項

| No. | 項目 | 確認先 | 期限 |
|----:|------|-------|------|
| 1 | DB に `pg_trgm` 拡張を適用するか（代替として Java 側の全スキャンで運用するか） | インフラ担当 / DBA | 実装前 |
| 2 | テイストの `display_name` はカテゴリ間で統一されているか（重複排除の前提確認） | DB シードデータ確認 | 実装前 |
| 3 | カタカナ正規化の方向（全角統一 or 半角統一）はサイト全体のデータ投入ルールと整合しているか | データ設計担当 | 実装前 |
| 4 | `variation_name` のキーワード検索追加で意図しないヒット増加が起きる懸念はないか | PO / 商品担当 | 実装前 |
