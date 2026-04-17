# FEAT-001 商品検索機能強化 基本設計書

作成日: 2026-04-17  
最終更新: 2026-04-17  
対象要件: `docs/issues/FEAT-001-requirements.md`  
関連設計書: `docs/design/FEAT-001-detail-design.md` / `docs/design/FEAT-001-test-design.md`

---

## 1. 目的・スコープ

### 1-1. 目的

本機能強化は、現状の商品検索ヒット率の低さ・離脱率の高さを解消するための改善である。  
以下の 4 つの機能を追加・変更することで、ユーザーが目的の商品に到達しやすくする。

| # | 機能 | 要件参照 |
|:-:|------|---------|
| F-1 | 検索対象フィールドの拡張と一致方式の見直し | §4-1 |
| F-2 | 複数キーワードの AND 検索 | §4-1 |
| F-3 | 全角・半角を区別しない入力正規化 | §4-3 |
| F-4 | 検索結果一覧へのテイスト絞込フィルタの追加 | §4-4 |

### 1-2. 変更対象画面・エンドポイント

| 区分 | URL | テンプレート | 変更内容 |
|------|-----|------------|---------|
| 検索結果一覧 | `GET /products/search` | `pages/product-list-search-results` | パラメータ拡張・UI 追加 |
| 全画面共通ヘッダ | （fragment） | `fragments/layout/header` | 変更なし（入力欄はそのまま） |

### 1-3. スコープ外

| 除外事項 | 理由 |
|---------|------|
| カテゴリ一覧画面（デスク / チェア / 収納）の検索機能 | ヘッダ検索ボックス経由の検索のみが対象 |
| サジェスト / オートコンプリート | 別課題で検討 |
| 同義語・表記ゆれの吸収（ソファ / ソファー等） | 本課題の対象外 |
| Elasticsearch 等全文検索エンジンの導入 | 最大 1,000 件スコープで導入コスト不釣合い |
| 管理画面・受注後業務 | システムスコープ外 |

---

## 2. システム構成概要

### 2-1. レイヤー構成

本システムは Spring Boot（MVC）+ MyBatis + PostgreSQL で構成される。  
FEAT-001 の変更は、全レイヤーにわたるが DB スキーマの変更はない。

```
┌─────────────────────────────────────────────────────────────────┐
│  ブラウザ                                                        │
│  ヘッダ検索ボックス（q, taste 等のクエリパラメータを送信）          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ GET /products/search?q=...&taste=...
┌──────────────────────────▼──────────────────────────────────────┐
│  Controller 層                                                    │
│  CatalogController.searchResults()                               │
│  ・リクエストパラメータの受け取り（taste 追加）                    │
│  ・モデル属性の組み立て（tasteFilterOptions 追加）                │
└──────┬─────────────────────────────────┬────────────────────────┘
       │                                 │
┌──────▼──────────────────┐   ┌──────────▼──────────────────────┐
│  ProductListSearchService│   │  ProductFilterOptionService     │
│  ・検索条件の組み立て     │   │  ・統合テイスト選択肢の取得  NEW │
│  ・taste 正規化  NEW      │   │  ・taste ホワイトリスト検証  NEW │
└──────┬──────────────────┘   └─────────────────────────────────┘
       │
┌──────▼──────────────────┐
│  ProductService          │
│  ・キーワード正規化  NEW  │
│  ・検索条件の正規化       │
└──────┬──────────────────┘
       │
┌──────▼──────────────────┐
│  ProductRepository        │
│  ・検索パラメータの生成   │
│  ・keywords（複数）NEW    │
│  ・tasteDisplayNames NEW  │
└──────┬──────────────────┘
       │
┌──────▼──────────────────┐
│  MyBatis Mapper / XML    │
│  ・ProductMapper.xml     │
│    - keywords foreach NEW│
│    - taste EXISTS NEW    │
└──────┬──────────────────┘
       │
┌──────▼──────────────────┐
│  PostgreSQL              │
│  products / product_     │
│  variants / *_tastes 等  │
└─────────────────────────┘
```

### 2-2. 新規追加コンポーネント

| コンポーネント | レイヤー | 役割 |
|-------------|---------|------|
| `NormalizationUtils` | Common | 検索キーワードの全角/半角正規化（static メソッド群） |
| `KeywordSearchParam` | Mapper row | 1 キーワード分の部分一致・前方一致パターンを保持する record |

---

## 3. 機能設計

### 3-1. F-1: 検索対象フィールドの拡張と一致方式の見直し

#### 検索対象フィールド一覧

| フィールド | テーブル | 一致方式 | 変更区分 |
|-----------|---------|---------|---------|
| `product_name` | `products` | 部分一致（`%keyword%`） | 変更なし |
| `variation_name` | `products` | 部分一致（`%keyword%`） | **追加** |
| `description` | `products` | 部分一致（`%keyword%`） | **追加** |
| `product_code` | `product_variants` | 前方一致（`keyword%`） | **変更**（部分一致→前方一致） |

#### 各フィールドの結合ロジック

```
1 キーワードに対する条件:

( product_name          ILIKE '%keyword%'
  OR (variation_name IS NOT NULL
      AND variation_name ILIKE '%keyword%')
  OR description         ILIKE '%keyword%'
  OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE 'keyword%'
  )
)
```

> `product_code` を前方一致に変更する理由:  
> 部分一致（`%keyword%`）では商品コード中間に偶然含まれるトークンで無関係な商品がヒットする問題があるため。  
> `keyword%` は完全一致も包含する（SQL の性質上、`keyword%` は `keyword` 自体にもマッチする）。

#### `variation_name` の NULL 考慮

`has_variation = FALSE` の商品は `variation_name` が NULL である。  
PostgreSQL において `NULL ILIKE '...'` は NULL を返すため実害はないが、  
意図を明示するために `variation_name IS NOT NULL` 条件を付与する。

---

### 3-2. F-2: 複数キーワードの AND 検索

#### 処理フロー

```
入力: "ナチュラル デスク"（正規化後）
  ↓
スペース（半角・全角）で分割
  ↓
キーワードリスト: ["ナチュラル", "デスク"]
  ↓
各キーワードに対して F-1 の OR 条件を生成
  ↓
各キーワード条件を AND で結合
  
結果:
  (name/variation/desc/code に "ナチュラル" がマッチ)
  AND
  (name/variation/desc/code に "デスク" がマッチ)
```

#### キーワード数と動作

| ケース | 動作 |
|------|------|
| キーワードなし（`q` 未指定 or 空） | キーワード条件は生成しない。他フィルタのみ適用 |
| キーワード 1 つ | 従来と同等の動作 |
| キーワード 2 つ以上（スペース区切り） | 全キーワードに AND で絞り込む |
| 連続スペース | 空要素を除去して処理（実質的に 1 スペースと同等） |

---

### 3-3. F-3: 全角・半角の入力正規化

#### 正規化方針

**正規化はサービス層（Java）で実施する。DB 側のカラム値は変更しない。**  
DB 内の文字列データは以下の形式に統一されていることを前提とする。

- 数字・英字: **半角**
- カタカナ: **全角**
- ハイフンや括弧等の記号: **半角**

#### 変換ルール

| 文字種 | 変換方向 | 例 |
|-------|---------|-----|
| 数字 | 全角 → 半角 | `１２３` → `123` |
| 英字（大文字） | 全角 → 半角 | `ＡＢＣ` → `ABC` |
| 英字（小文字） | 全角 → 半角 | `ａｂｃ` → `abc` |
| カタカナ | 半角 → 全角（濁点・半濁点の合成含む） | `ﾃﾞｽｸ` → `デスク` |
| ハイフン | 全角 → 半角 | `－` → `-` |
| 括弧 | 全角 → 半角 | `（）` → `()` |

> 大文字/小文字の区別については、PostgreSQL の `ILIKE` 演算子で吸収する（既存と同様）。  
> したがって正規化後の英字は大文字・小文字どちらに変換してもよいが、本実装では変換後の大文字/小文字は元のままとし（全角→半角変換のみ）、ILIKE で区別なし検索する。

#### 正規化の実施タイミング

```
ブラウザ入力値（生の文字列）
  ↓ CatalogController（受け取り）
  ↓ ProductListSearchService.buildCondition()
  ↓ ProductService.buildCondition()
      ↓ NormalizationUtils.normalizeForSearch(keyword)  ← ここで変換
  ProductSearchCondition.keyword = 正規化済みキーワード
```

---

### 3-4. F-4: テイスト絞込フィルタの追加

#### テイストデータ構造

テイストマスタはカテゴリごとに独立したテーブルに分かれている。  
検索結果一覧はカテゴリ横断のため、3 テーブルを統合して扱う。

```
desk_tastes    ─ product_desk_attributes    ─ products（category_id='desk'）
chair_tastes   ─ product_chair_attributes   ─ products（category_id='chair'）
storage_tastes ─ product_storage_attributes ─ products（category_id='storage'）
```

#### フィルタ選択肢の構築

各テーブルから `is_active = TRUE` のレコードを取得し、`display_name` の一致による重複除去を行う。  
並び順は `sort_order ASC`、同値の場合は `display_name ASC`。

```sql
-- 統合テイスト一覧取得イメージ
SELECT display_name
FROM (
    SELECT display_name, sort_order FROM desk_tastes    WHERE is_active = TRUE
    UNION ALL
    SELECT display_name, sort_order FROM chair_tastes   WHERE is_active = TRUE
    UNION ALL
    SELECT display_name, sort_order FROM storage_tastes WHERE is_active = TRUE
) combined
GROUP BY display_name
ORDER BY MIN(sort_order) ASC, display_name ASC
```

#### フィルタ値の設計

`display_name`（文字列）をキーとして使用する。各テーブルの数値 ID ではなく表示名を使う理由は、カテゴリをまたぐ共通キーが存在しないためである。

#### フィルタ適用ロジック

複数のテイストが選択された場合は OR 条件で結合する。

```
taste=ナチュラル&taste=モダン の場合:

AND (
  EXISTS ( ...desk_tastes.display_name IN ('ナチュラル','モダン') ... )
  OR EXISTS ( ...chair_tastes.display_name IN ('ナチュラル','モダン') ... )
  OR EXISTS ( ...storage_tastes.display_name IN ('ナチュラル','モダン') ... )
)
```

#### テイスト単独検索（キーワードなし）

`q` パラメータが空/未指定であっても `taste` パラメータ単独で絞り込みを実行できる。  
この場合、F-1・F-2・F-3 のキーワード処理は行わず、テイストおよびその他の既存フィルタのみが SQL に追加される。

---

## 4. 画面設計

### 4-1. 検索結果一覧画面（`GET /products/search`）

#### 4-1-1. 画面全体レイアウト（変更箇所のみ抜粋）

```
┌─────────────────────────────────────────────────────────────────────┐
│ ヘッダ（検索ボックス）                                               │
├─────────────────────────────────────────────────────────────────────┤
│ パンくず > 検索結果                                                  │
│ 検索キーワード: ○○○                                                 │
├───────────────────┬─────────────────────────────────────────────────┤
│ 絞込パネル        │ 件数バッジ · ソート · 表示件数                  │
│（サイドバー）      │                                                  │
│                   │                                                  │
│ [✓] 在庫有のみ    │  ┌──────┐ ┌──────┐ ┌──────┐                  │
│                   │  │商品カード│ │      │ │      │                  │
│ 価格帯（税込）    │  └──────┘ └──────┘ └──────┘                  │
│ □ ～20,000円      │                                                  │
│ □ 20,000～40,000  │  ┌──────┐ ┌──────┐ ...                      │
│ ...               │                                                  │
│                   │  ページネーション                                │
│ カラー            │                                                  │
│ ○ ○ ○ ...       │                                                  │
│                   │                                                  │
│ テイスト  ← NEW   │                                                  │
│ □ ナチュラル      │                                                  │
│ □ モダン          │                                                  │
│ □ 北欧            │                                                  │
│ □ ...             │                                                  │
│                   │                                                  │
│ [検索] [リセット] │                                                  │
└───────────────────┴─────────────────────────────────────────────────┘
```

#### 4-1-2. テイストフィルタ UI 仕様

| 項目 | 仕様 |
|------|------|
| 形式 | チェックボックスリスト（カテゴリ一覧のテイストフィルタと同様の見た目） |
| 表示タイトル | 「テイスト」 |
| 表示順 | `sort_order ASC`、同値は `display_name ASC` |
| 複数選択 | 可（OR 条件） |
| テイスト候補がない場合 | フィールドごと非表示 |
| 現在選択中の状態 | チェック済み（チェックボックスのデフォルト状態として保持） |
| フォーム送信後の引き継ぎ | 選択済み `taste` 値は次のリクエストに引き継がれる |

#### 4-1-3. 絞込フォームパラメータ設計（変更後の全パラメータ）

| パラメータ名 | 型 | 必須 | 説明 | 変更区分 |
|------------|-----|:----:|------|---------|
| `q` | `String` | | キーワード（スペース区切りで AND 検索） | 変更なし |
| `inStockOnly` | `boolean` | | 在庫有のみ（デフォルト `false`） | 変更なし |
| `priceBand` | `List<Integer>` | | 価格帯 ID（1〜6、複数可） | 変更なし |
| `color` | `List<String>` | | カラーキー（複数可） | 変更なし |
| `taste` | `List<String>` | | テイスト表示名（複数可） | **追加** |
| `sort` | `String` | | 並び順（`recommended` / `newest` / `price_asc` / `price_desc`） | 変更なし |
| `page` | `int` | | ページ番号（デフォルト `1`） | 変更なし |
| `size` | `int` | | 表示件数（`15` / `30` / `60`、デフォルト `15`） | 変更なし |

#### 4-1-4. URL 例

| ケース | URL |
|------|-----|
| キーワードのみ | `/products/search?q=デスク` |
| 複数キーワード | `/products/search?q=ナチュラル+デスク` |
| 全角入力（ブラウザがエンコード） | `/products/search?q=%EF%BC%AE%EF%BC%A4%EF%BC%AB` |
| テイスト絞込のみ | `/products/search?taste=ナチュラル` |
| キーワード + テイスト複数 | `/products/search?q=デスク&taste=ナチュラル&taste=モダン` |
| 複合（全フィルタ） | `/products/search?q=デスク&taste=ナチュラル&inStockOnly=true&priceBand=2&color=1&sort=price_asc&page=1&size=15` |

#### 4-1-5. ページネーション・ソートにおけるパラメータ引き継ぎ

ページ移動・ソート変更・件数変更時に `taste` パラメータが引き継がれるよう、  
既存の `priceBand`・`color` と同様に hidden input または URL パラメータとして埋め込む。

---

## 5. データ設計

### 5-1. DB スキーマ変更

**テーブルの追加・変更・削除はない。**  
既存テーブルの構造はそのまま使用する。

### 5-2. 検索に関わるテーブルと役割

#### キーワード検索対象テーブル

| テーブル | カラム | 用途 |
|---------|-------|------|
| `products` | `product_name` | 商品名（部分一致） |
| `products` | `variation_name` | バリエーション名（部分一致）。NULL 商品は除外 |
| `products` | `description` | 商品説明文（部分一致） |
| `product_variants` | `product_code` | 商品コード（前方一致） |

#### テイストフィルタ関連テーブル

| テーブル | 役割 |
|---------|------|
| `desk_tastes` | デスクのテイストマスタ（`taste_id`, `display_name`, `sort_order`, `is_active`） |
| `chair_tastes` | チェアのテイストマスタ |
| `storage_tastes` | 収納家具のテイストマスタ |
| `product_desk_attributes` | 商品とデスクテイストの対応（`product_id`, `taste_id`） |
| `product_chair_attributes` | 商品とチェアテイストの対応 |
| `product_storage_attributes` | 商品と収納家具テイストの対応 |

### 5-3. インデックス設計方針

ILIKE 検索の性能を確保するため `pg_trgm` 拡張を利用した GIN インデックスを追加する。  
インデックス追加は DDL として `sql/schema/` 配下に管理する。

| テーブル.カラム | インデックス種別 | 優先度 |
|---------------|---------------|:-----:|
| `products.description` | `pg_trgm` GIN | ★★★（TEXT 型のため最重要） |
| `products.variation_name` | `pg_trgm` GIN | ★★ |
| `products.product_name` | `pg_trgm` GIN | ★★ |
| `product_variants.product_code` | B-tree（既存 UNIQUE 制約） | ★（前方一致は B-tree で対応可） |

> **前提:** `pg_trgm` 拡張（`CREATE EXTENSION IF NOT EXISTS pg_trgm`）の有効化が必要。  
> インフラ担当者・DBA との事前合意が必要（未決事項 No.1）。  
> 拡張を適用しない場合は全件 ILIKE スキャンとなるが、最大 1,000 件のスコープでは 2 秒以内の性能目標を達成できる見込み。

---

## 6. インターフェース設計

### 6-1. 検索エンドポイント仕様

| 項目 | 内容 |
|------|------|
| HTTP メソッド | `GET` |
| URL | `/products/search` |
| 認証 | 不要（公開エンドポイント） |
| レスポンス | HTML（Thymeleaf テンプレートでレンダリング） |

#### リクエストパラメータ詳細

| パラメータ | 型 | 省略時 | バリデーション |
|-----------|-----|-------|--------------|
| `q` | `String` | `null`（キーワード条件なし） | 長さ制限なし（サービス層でトリム・正規化） |
| `taste` | `List<String>` | 空リスト | ホワイトリスト（DB の有効 display_name のみ通過） |
| `inStockOnly` | `boolean` | `false` | — |
| `priceBand` | `List<Integer>` | 空リスト | 1〜6 範囲外は除去 |
| `color` | `List<String>` | 空リスト | 有効カラーキーのみ通過 |
| `sort` | `String` | `recommended` | 定義外の値はデフォルト値に置換 |
| `page` | `int` | `1` | 最小 1 に補正 |
| `size` | `int` | `15` | 許可値（15/30/60）以外はデフォルトに補正 |

#### バリデーション・入力正規化の実施箇所

| 入力値 | 処理 | 実施レイヤー |
|-------|------|------------|
| `q`（キーワード） | trim → 全角/半角正規化 | `ProductService`（`NormalizationUtils` 呼び出し） |
| `taste` | ホワイトリストフィルタ（DB の有効 display_name のみ通過） | `ProductFilterOptionService` |
| `priceBand` | 有効 ID 範囲に絞込 | `ProductFilterOptionService` |
| `color` | 有効カラーキーのみ通過 | `ProductFilterOptionService` |
| `sort` | 定義外はデフォルト置換 | `ProductService` |
| `page`, `size` | 範囲外は補正 | `ProductService` |

### 6-2. モデル属性（View に渡す変数）

| 属性名 | 型 | 説明 | 変更区分 |
|-------|-----|------|---------|
| `keyword` | `String` | 現在のキーワード（正規化済み）。空の場合 `""` | 変更なし |
| `totalCount` | `long` | ヒット件数 | 変更なし |
| `products` | `List<ProductCardView>` | 商品カード一覧 | 変更なし |
| `colorFilterOptions` | `List<ColorFilterOption>` | カラー絞込選択肢 | 変更なし |
| `selectedColorKeys` | `List<String>` | 選択中カラーキー | 変更なし |
| `selectedPriceBandIds` | `List<Integer>` | 選択中価格帯 ID | 変更なし |
| `inStockOnly` | `boolean` | 在庫ありのみ選択状態 | 変更なし |
| `sortValue` | `String` | 現在の並び順 | 変更なし |
| `currentPage` | `int` | 現在ページ | 変更なし |
| `totalPages` | `int` | 総ページ数 | 変更なし |
| `pageSize` | `int` | 表示件数 | 変更なし |
| `tasteFilterOptions` | `List<String>` | テイスト選択肢（display_name リスト） | **追加** |
| `selectedTasteDisplayNames` | `List<String>` | 選択中テイスト表示名リスト | **追加** |

---

## 7. 処理フロー設計

### 7-1. キーワード検索のシーケンス概要

```
ユーザー         ブラウザ         Controller            Service            Repository        DB
  │               │                   │                    │                    │             │
  │ 検索ボックス  │                   │                    │                    │             │
  │ にキーワード  │                   │                    │                    │             │
  │ を入力・送信  │                   │                    │                    │             │
  │──────────────▶│                   │                    │                    │             │
  │               │ GET /products/    │                    │                    │             │
  │               │ search?q=...&     │                    │                    │             │
  │               │ taste=...         │                    │                    │             │
  │               │──────────────────▶│                    │                    │             │
  │               │                   │ loadOptionsBundle()│                    │             │
  │               │                   │ loadUnified        │                    │             │
  │               │                   │ TasteDisplayNames()│──────────────────▶│             │
  │               │                   │                    │                    │ SELECT      │
  │               │                   │                    │                    │ display_name│
  │               │                   │                    │                    │◀────────────│
  │               │                   │                    │◀───────────────────│             │
  │               │                   │ buildCondition()   │                    │             │
  │               │                   │───────────────────▶│                    │             │
  │               │                   │                    │ normalizeForSearch()│            │
  │               │                   │                    │ （全角→半角等）     │             │
  │               │                   │                    │ normalizeTaste      │             │
  │               │                   │                    │ DisplayNames()      │             │
  │               │                   │                    │ （ホワイトリスト）  │             │
  │               │                   │                    │ → ProductSearch     │             │
  │               │                   │                    │   Condition 生成    │             │
  │               │                   │◀───────────────────│                    │             │
  │               │                   │ search()           │                    │             │
  │               │                   │───────────────────▶│                    │             │
  │               │                   │                    │ buildSearchParams() │             │
  │               │                   │                    │ （keywords生成,     │             │
  │               │                   │                    │  taste params）     │             │
  │               │                   │                    │────────────────────▶│            │
  │               │                   │                    │                    │ SELECT      │
  │               │                   │                    │                    │ COUNT(*) /  │
  │               │                   │                    │                    │ products    │
  │               │                   │                    │                    │◀────────────│
  │               │                   │                    │◀───────────────────│             │
  │               │◀──────────────────│                    │                    │             │
  │ 検索結果HTML  │                   │                    │                    │             │
  │◀──────────────│                   │                    │                    │             │
```

### 7-2. キーワード正規化の処理フロー

```
入力例: "ﾅﾁｭﾗﾙ　ﾃﾞｽｸ"（全角スペース区切り）

① NormalizationUtils.normalizeForSearch()
   "ﾅﾁｭﾗﾙ　ﾃﾞｽｸ"
   → 半角カタカナ → 全角: "ナチュラル　デスク"
   → 全角スペース（変換なし）

② ProductService.buildCondition()
   "ナチュラル　デスク".trim() → "ナチュラル　デスク"
   ProductSearchCondition.keyword = "ナチュラル　デスク"

③ ProductRepository.buildKeywords()
   "ナチュラル　デスク".split("[\\s\u3000]+")
   → ["ナチュラル", "デスク"]
   → KeywordSearchParam[0]: {keywordLike: "%ナチュラル%", codeLike: "ナチュラル%"}
   → KeywordSearchParam[1]: {keywordLike: "%デスク%",    codeLike: "デスク%"}

④ MyBatis <foreach> で AND 結合
   ( p.product_name ILIKE '%ナチュラル%' OR ... )
   AND
   ( p.product_name ILIKE '%デスク%' OR ... )
```

---

## 8. 非機能要件設計方針

### 8-1. 性能

| 目標 | 値 |
|------|-----|
| 検索対象商品数 | 最大 1,000 件 |
| 検索結果一覧初回表示 | 95 パーセンタイルで **2 秒以内** |

#### 性能確保の方針

| 対策 | 内容 | 条件 |
|------|------|------|
| `pg_trgm` GIN インデックス | `product_name` / `variation_name` / `description` の ILIKE を高速化 | `pg_trgm` 拡張の有効化が前提 |
| 前方一致に変更（`product_code`） | 既存 B-tree インデックス（UNIQUE 制約）で対応可。部分一致より高速 | — |
| EXISTS サブクエリ（テイスト） | カテゴリ一覧の既存実装パターンに準じた EXISTS を使用 | — |

### 8-2. セキュリティ

| 観点 | 対策 |
|------|------|
| SQL インジェクション | MyBatis `#{...}` 記法によるプリペアドステートメント |
| `taste` パラメータの不正値 | `ProductFilterOptionService.normalizeTasteDisplayNames()` による DB ベースのホワイトリストフィルタリング |
| その他パラメータの不正値 | `priceBand`・`color` は既存の正規化ロジック（許可値セットとの比較）で対処済み |
| CSRF | GET リクエストのみ（読み取り専用）のためリスクなし |

### 8-3. ユーザビリティ

| 観点 | 対応方針 |
|------|---------|
| 全角/半角の違いを意識させない | F-3 の正規化により実現。ユーザーは意識不要 |
| 検索ゼロヒット時の表示 | 既存の「該当する商品は見つかりませんでした。」メッセージを使用（変更なし） |
| テイスト選択状態の維持 | ページ移動・ソート変更時に `taste` パラメータを引き継ぐ |
| モバイル対応 | テイストフィルタ UI はモバイルモーダル側の実装は今回スコープ外（別課題） |

---

## 9. エラー設計

### 9-1. 異常系の扱い

本機能はすべて参照系（SELECT）のみで更新処理を伴わない。  
異常発生時のふるまいは以下のとおりとし、既存エラーハンドリングに準じる。

| ケース | ふるまい |
|--------|---------|
| DB 接続エラー | 既存の 500 エラーハンドリング（`error/500.html`）に委ねる |
| `taste` に不正値が含まれる | ホワイトリストで除去される。エラーにはならず、条件から無視される |
| `priceBand`, `color` 等に不正値 | 既存と同様に忽視または除去 |
| `page` が範囲外 | 最終ページに自動補正（既存の `searchWithPageCorrection()` で対応） |
| キーワードが極端に長い | 性能上の懸念はあるが本課題でのバリデーションは行わない（別途検討） |

---

## 10. 既存設計書との整合性

### 10-1. `docs/design/crud-matrix.md` への影響

機能 **05「商品一覧（キーワード検索）」** の商品マスタドメイン行を変更する。

| テーブル | 変更前 | 変更後 | 理由 |
|---------|:-----:|:-----:|-----|
| `desk_tastes` | `−` | `R` | テイストフィルタ選択肢取得 + EXISTS 絞込 |
| `chair_tastes` | `−` | `R` | 同上 |
| `storage_tastes` | `−` | `R` | 同上 |

### 10-2. 他設計書への影響なし

| 設計書 | 変更要否 | 理由 |
|-------|---------|------|
| `er-diagram.md` | **なし** | テーブル追加・変更なし |
| `screen-list.md` | **なし** | URL・テンプレート変更なし |
| `screen-flow-diagram.md` | **なし** | 画面遷移の追加・変更なし |

---

## 11. 未決事項

| No. | 項目 | 確認先 | 期限 | 影響 |
|----:|------|-------|------|------|
| 1 | `pg_trgm` 拡張の適用可否 | インフラ担当 / DBA | 実装前 | インデックス設計の適用可否。未適用でも機能自体は動作する |
| 2 | `display_name` のカテゴリ間統一性 | DB シードデータ確認 | 実装前 | テイストフィルタの統合一覧の内容が変わる可能性 |
| 3 | カタカナ正規化方向のデータ整合性確認 | データ設計担当 | 実装前 | 正規化後の文字列と DB 値のミスマッチが起きると検索漏れが発生する |
| 4 | `variation_name` 追加によるヒット増加の懸念 | PO / 商品担当 | 実装前 | 要件の一部スコープ変更が必要になる可能性 |
