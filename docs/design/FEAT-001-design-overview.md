# FEAT-001 — 商品検索機能強化 設計概要

> 作成日: 2026-03-16  
> 関連要件: [FEAT-001-requirements.md](../issues/FEAT-001-requirements.md)

---

## 1. 設計書一覧

| ファイル | 内容 |
|---------|------|
| **本書** (`FEAT-001-design-overview.md`) | 全体設計方針・変更ファイル一覧・処理フロー |
| [`FEAT-001-backend-design.md`](FEAT-001-backend-design.md) | バックエンド詳細設計（各クラス・SQL の変更仕様） |
| [`FEAT-001-frontend-design.md`](FEAT-001-frontend-design.md) | フロントエンド詳細設計（Thymeleaf テンプレート変更仕様） |

---

## 2. 設計方針の確定事項

### 2-1. FR-01: 商品コードの前方一致化

- `keywordLike`（`%keyword%`）を product_code 用に使うのをやめ、`keywordPrefix`（`keyword%`）を新規追加する
- product_name / variation_name / description は引き続き `keywordLike`（`%keyword%`）を使用する
- `ProductRepository` で `toKeywordLike()` はそのまま残し、`toKeywordPrefix()` を追加する

### 2-2. FR-02: 検索対象の拡張

- `variation_name`・`description` を `BaseProductWhere` のキーワード条件に追加する
- どちらも `ILIKE #{keywordLike}` による部分一致（`%keyword%`）とする

### 2-3. FR-03: 全角・半角の正規化（方針: 案B — Java側 NFKC 正規化）

**採用アプローチ:** Java 標準の `java.text.Normalizer.normalize(text, Normalizer.Form.NFKC)` を `ProductRepository` 内で適用する。SQL 側の変更は不要。

**NFKC 正規化の動作:**

| 入力 | NFKC後 | 効果 |
|-----|--------|------|
| `ＤＫ－１００`（全角英数） | `DK-100`（半角） | 全角英数 → 半角に統一 |
| `ｱｰﾑﾁｪｱ`（半角カタカナ） | `アームチェア`（全角） | 半角カナ → 全角に統一 |
| `ﾃﾞｽｸ`（結合文字） | `デスク` | 濁点結合も正しく変換 |
| `DK-100`（既に半角） | `DK-100` | 変化なし |
| `アームチェア`（全角カタカナ） | `アームチェア` | 変化なし |

**前提:** DB に登録されるデータは、英数字・記号は半角、カタカナは全角で統一されている。NFKC でユーザー入力を同じ形式に正規化することで、SQL 側の関数適用なしに一致判定が可能になる。

> DB データが上記の前提と異なる場合（全角数字が混在する等）は、案A（DB インデックスへの正規化関数追加）への切り替えを検討すること。

### 2-4. FR-04: テイスト絞込の設計方針

**テイストマスタが 3 テーブル分離である問題への対処:**

テイスト絞込は `display_name` 文字列を URL パラメーターとして送受信し、サービス層で各カテゴリ別の `taste_id` に変換する。

```
URL パラメーター: ?taste=ナチュラル&taste=モダン
        ↓ (サービス層)
desk_tastes.display_name IN ('ナチュラル','モダン') → deskTasteIds = [1, 3]
chair_tastes.display_name IN ('ナチュラル','モダン') → chairTasteIds = [2, 5]
storage_tastes.display_name IN ('ナチュラル','モダン') → storageTasteIds = [1, 4]
        ↓ (SQL: SearchTasteFilter フラグメント)
AND (
  EXISTS (... product_desk_attributes.taste_id IN (1,3))
  OR EXISTS (... product_chair_attributes.taste_id IN (2,5))
  OR EXISTS (... product_storage_attributes.taste_id IN (1,4))
)
```

**既存 `DeskAttributeFilter` は流用しない理由:**
- 既存フィルターは `hasDeskFilter = categoryId == DESK` の場合のみ発動する `AND EXISTS`
- 検索結果画面は `categoryId = null` のため `hasDeskFilter = false` → 既存フラグメントは発動しない
- カテゴリ横断 OR 条件のための新 SQL フラグメント `SearchTasteFilter` を追加する

**UI での選択肢の扱い:**
- 3 テーブルの `display_name` を Java 側でマージ・重複除去して UI に表示する
- ID ではなく `display_name` 文字列を POST することで、テーブル分離を UI 側に隠蔽する

---

## 3. 変更ファイル一覧

### バックエンド

| レイヤー | ファイル | 変更種別 | 対応 FR |
|---------|---------|---------|---------|
| Repository | `repository/ProductRepository.java` | 変更 | FR-01, FR-02, FR-03 |
| Mapper XML | `resources/mappers/ProductMapper.xml` | 変更 | FR-01, FR-02, FR-04 |
| Mapper IF | `mapper/ProductMapper.java` | 変更なし | — |
| Service | `service/product/ProductFilterOptionService.java` | 変更（メソッド追加） | FR-04 |
| Service | `service/product/ProductListSearchService.java` | 変更（シグネチャ変更） | FR-04 |
| Controller | `web/CatalogController.java` | 変更 | FR-04 |

### フロントエンド

| レイヤー | ファイル | 変更種別 | 対応 FR |
|---------|---------|---------|---------|
| Template | `templates/pages/product-list-search-results.html` | 変更 | FR-04 |

### データベース

変更なし（FR-03 は案B のため）

---

## 4. 処理フロー（変更後）

### 4-1. キーワード検索フロー（FR-01/02/03）

```
GET /products/search?q=ｱｰﾑﾁｪｱ&...
  │
  ▼
CatalogController#searchResults(@RequestParam q="ｱｰﾑﾁｪｱ", ...)
  │
  ▼
ProductListSearchService#buildCondition(keyword="ｱｰﾑﾁｪｱ", ...)
  │  → ProductFilterOptionService でカラー・価格帯正規化
  │  → ProductService#buildCondition() → ProductSearchCondition
  │
  ▼
ProductRepository#buildSearchParams(condition)
  │  → normalizeKeyword("ｱｰﾑﾁｪｱ")
  │        └─ Normalizer.normalize(NFKC) → "アームチェア"
  │  → toKeywordLike("アームチェア") → "%アームチェア%"
  │  → toKeywordPrefix("アームチェア") → "アームチェア%"
  │  → params["keywordLike"] = "%アームチェア%"
  │  → params["keywordPrefix"] = "アームチェア%"
  │
  ▼
ProductMapper.xml BaseProductWhere
  AND (
    p.product_name     ILIKE '%アームチェア%'  ← keywordLike
    OR p.variation_name ILIKE '%アームチェア%'  ← keywordLike（新規）
    OR p.description    ILIKE '%アームチェア%'  ← keywordLike（新規）
    OR EXISTS (
      SELECT 1 FROM product_variants pvk
      WHERE pvk.product_id = p.product_id
        AND pvk.product_code ILIKE 'アームチェア%'  ← keywordPrefix（変更）
    )
  )
```

### 4-2. テイスト絞込フロー（FR-04）

```
GET /products/search?taste=ナチュラル&taste=モダン&...
  │
  ▼
CatalogController#searchResults(@RequestParam taste=["ナチュラル","モダン"], ...)
  │
  ▼
ProductFilterOptionService#resolveTasteFilter(["ナチュラル","モダン"], optionsBundle)
  │  → bundle.deskTasteOptions() でフィルタ → deskTasteIds = [1, 3]
  │  → bundle.chairTasteOptions() でフィルタ → chairTasteIds = [2, 5]
  │  → bundle.storageTasteOptions() でフィルタ → storageTasteIds = [1, 4]
  │  → ProductCategoryFilter(deskTasteIds=[1,3], chairTasteIds=[2,5], storageTasteIds=[1,4])
  │
  ▼
ProductListSearchService#buildCondition(categoryId=null, ..., tasteFilter)
  │  → ProductSearchCondition(categoryFilter=tasteFilter, ...)
  │
  ▼
ProductRepository#buildSearchParams(condition)
  │  → hasSearchTasteFilter = (categoryId==null) AND (deskTasteIds非空 OR ...)  = true
  │  → params["hasSearchTasteFilter"] = true
  │  → params["deskTasteIds"] = [1, 3]
  │  → params["chairTasteIds"] = [2, 5]
  │  → params["storageTasteIds"] = [1, 4]
  │
  ▼
ProductMapper.xml SearchTasteFilter フラグメント
  AND (
    EXISTS (SELECT 1 FROM product_desk_attributes da
            WHERE da.product_id = p.product_id AND da.taste_id IN (1,3))
    OR EXISTS (SELECT 1 FROM product_chair_attributes ca
               WHERE ca.product_id = p.product_id AND ca.taste_id IN (2,5))
    OR EXISTS (SELECT 1 FROM product_storage_attributes sa
               WHERE sa.product_id = p.product_id AND sa.taste_id IN (1,4))
  )
```

---

## 5. モデル属性の変更（Controller → Template）

### 追加するモデル属性（検索結果画面）

| 属性名 | 型 | 内容 |
|-------|-----|------|
| `tasteOptions` | `List<String>` | 全カテゴリのテイスト表示名（重複除去済み） |
| `selectedTasteNames` | `List<String>` | 選択中のテイスト表示名（URL パラメーターから） |

### 既存モデル属性（変更なし）

| 属性名 | 型 |
|-------|-----|
| `keyword` | `String` |
| `products` | `List<ProductCardView>` |
| `totalCount` / `totalPages` | `long` / `int` |
| `currentPage` / `pageSize` | `int` |
| `sortValue` | `String` |
| `inStockOnly` | `boolean` |
| `selectedPriceBandIds` | `List<Integer>` |
| `selectedColorKeys` | `List<String>` |
| `colorFilterOptions` | `List<ColorFilterOption>` |

---

## 6. URL パラメーター仕様（変更後）

`GET /products/search`

| パラメーター | 型 | 必須 | デフォルト | 説明 |
|------------|-----|------|-----------|------|
| `q` | `String` | ✗ | `""` | 検索キーワード（NFKC 正規化後に検索） |
| `inStockOnly` | `boolean` | ✗ | `false` | 在庫ありのみ |
| `priceBand` | `List<Integer>` | ✗ | `[]` | 価格帯ID（1〜6）複数可 |
| `color` | `List<String>` | ✗ | `[]` | カラーキー 複数可 |
| `taste` | `List<String>` | ✗ | `[]` | テイスト表示名 複数可（**新規**） |
| `sort` | `String` | ✗ | `recommended` | ソート順 |
| `page` | `int` | ✗ | `1` | ページ番号 |
| `size` | `int` | ✗ | `15` | 1ページ件数 |
