# FEAT-001 — 商品検索機能強化 要件定義書

> 作成日: 2026-03-16  
> ステータス: Draft

---

## 1. 背景・目的

商品検索のヒット率を上げ、離脱率を下げることを目的として、ヘッダーの検索ボックスを起点とする商品検索機能を強化する。

具体的には以下の課題を解消する。

| 課題 | 詳細 |
|------|------|
| 商品コードの誤ヒット | 現状は商品コードも部分一致（中間一致）で検索されるため、コードの一部の数字が意図せず別商品にマッチする |
| 表記揺れによる検索漏れ | 全角・半角、大文字・小文字の違いで同一商品がヒットしない場合がある |
| 絞り込み条件の不足 | 検索結果画面にはテイスト絞込がなく、カテゴリ一覧ページとの機能差が存在する |

---

## 2. 対象範囲

| 対象 | 詳細 |
|------|------|
| ✅ **ヘッダー検索ボックス** | 全ページ共通ヘッダーの検索フォーム（`q` パラメーター） |
| ✅ **商品検索結果一覧画面** | `GET /products/search` |
| ❌ カテゴリ別一覧 | `/products/category/{category}` は対象外 |
| ❌ 新着商品一覧 | `/products/new-arrivals` は対象外 |
| ❌ バッチ・管理機能 | 変更なし |

---

## 3. 現状仕様（As-Is）

### 3-1. 検索マッチング

現在の `ProductMapper.xml`（`BaseProductWhere`）の実装：

```sql
AND (
  p.product_name ILIKE #{keywordLike}           -- 中間一致（%...%）
  OR EXISTS (
    SELECT 1 FROM product_variants pvk
    WHERE pvk.product_id = p.product_id
      AND pvk.product_code ILIKE #{keywordLike}  -- 中間一致（%...%）
  )
)
```

- `keywordLike` は `%キーワード%` の形式で渡される（中間一致）
- 商品コード・商品名とも同一条件で検索される
- PostgreSQL の `ILIKE` による英字大文字小文字区別なし
- 全角・半角の正規化は未実装
- 検索対象は `product_name`（`products`）と `product_code`（`product_variants`）の 2 カラムのみ

### 3-2. 検索結果画面の絞込条件

| 絞込項目 | パラメーター名 | 現状 |
|----------|---------------|------|
| 在庫あり | `inStockOnly` | ✅ あり |
| 価格帯 | `priceBand` | ✅ あり |
| カラー | `color` | ✅ あり |
| テイスト | — | ❌ なし |

### 3-3. 検索条件モデル

`ProductSearchCondition` の `keyword` フィールドが単一文字列としてサービス層に渡され、`keywordLike`（`%keyword%`）に変換されてマッパーに渡される。検索フィールドの区別なし。

---

## 4. 機能要件（To-Be）

### FR-01: 商品コード検索は前方一致のみにする

| 項目 | 仕様 |
|------|------|
| 対象カラム | `product_variants.product_code` |
| マッチング方式 | **前方一致**（`ILIKE #{keywordPrefix}%`）のみ |
| 除外ルール | 中間一致（`%keyword%`）は適用しない |
| 理由 | コードの一部数字列が意図しない商品にマッチするのを防ぐため |
| 備考 | 前方一致は完全一致を包含するため、完全一致を別条件として扱わない |

**動作イメージ:**

| 入力キーワード | `product_code = 'DK-100'` の商品 | 備考 |
|---|---|---|
| `DK-100` | ✅ ヒット（前方一致・完全一致を包含） | |
| `DK-1` | ✅ ヒット（前方一致） | |
| `DK` | ✅ ヒット（前方一致） | |
| `100` | ❌ ヒットしない | 中間一致のため除外 |
| `-100` | ❌ ヒットしない | 中間一致のため除外 |

> **実装上の補足:** `ProductRepository.toKeywordLike()` を前方一致版 `toKeywordPrefix()`（`keyword + '%'`）に変更し、Map に `keywordPrefix` として渡す。XML 側では `pvk.product_code ILIKE #{keywordPrefix}` に変更する。正規化処理（FR-03）と組み合わせること。

---

### FR-02: 商品名・バリエーション名・説明文は部分一致検索

| 項目 | 仕様 |
|------|------|
| 対象カラム | `products.product_name`、`products.variation_name`、`products.description` |
| マッチング方式 | 部分一致（`ILIKE '%' \|\| #{keyword} \|\| '%'`） |
| 備考 | 現状の `product_name` の中間一致に加え、`variation_name` と `description` も新たに対象とする |

**検索対象カラム一覧（変更後）:**

| カラム | テーブル | マッチング方式 | 現状 |
|--------|----------|---------------|------|
| `product_code` | `product_variants` | 前方一致のみ | 中間一致→**変更** |
| `product_name` | `products` | 部分一致 | 部分一致（変更なし） |
| `variation_name` | `products` | 部分一致 | **新規追加** |
| `description` | `products` | 部分一致 | **新規追加** |

---

### FR-03: 英字・数字・カタカナの表記揺れを吸収する

#### FR-03-1: 英字の大文字・小文字を区別しない

- 既存の `ILIKE` による大文字小文字非区別を維持する
- 全角英字も半角英字と同一視する（FR-03-2 の正規化で吸収）

#### FR-03-2: 数字・英字・カタカナの全角・半角を区別しない

| 文字種 | 例 | 要件 |
|--------|-----|------|
| 英字 | `A` ↔ `Ａ` | 全角・半角を同一視 |
| 数字 | `1` ↔ `１` | 全角・半角を同一視 |
| カタカナ | `ア` ↔ `ｱ` | 全角・半角を同一視 |

**実装方針（案）:**

アプリケーション層（サービス層）で、入力キーワードを**半角正規化**してから検索条件に渡す。  
DB 側のカラムも同様に正規化済みの値で比較するか、DB 側で正規化関数を適用する。  
具体的な正規化方針は実装フェーズで確定する。下記の 2 案を実装フェーズで選択する。

| 案 | 方法 | メリット | デメリット |
|----|------|---------|-----------|
| 案A | Java サービス層でキーワードを正規化し、DB カラムも正規化済みで保存 / インデックスに正規化関数を使用 | インデックスが効く | カラムへの正規化インデックス追加が必要 |
| 案B | PostgreSQL の `regexp_replace` + `translate` 等をクエリ内で適用 | スキーマ変更不要 | インデックスが効かない可能性あり（性能要件 PER-01 を確認） |

> **決定:** 商品数の上限が 1,000 件程度（PER-01 参照）であるため、インデックス不使用でも 2 秒以内の目標達成が見込まれる。実装の簡易さを優先し、必要に応じて案A に切り替える。

---

### FR-04: 検索結果画面の絞込条件に「テイスト」を追加する

#### 要件

- 現在の検索結果画面（`/products/search`）の絞込フィルターに「テイスト」を追加する
- カテゴリ一覧（デスク・チェア・収納）のテイスト絞込と同一仕様とする

#### テイスト絞込の既存仕様（参照元）

| カテゴリ | DBテーブル | 絞込フィールド |
|----------|-----------|--------------|
| デスク | `product_desk_attributes.taste_id` | `deskTasteIds`（`ProductCategoryFilter`） |
| チェア | `product_chair_attributes.taste_id` | `chairTasteIds`（`ProductCategoryFilter`） |
| 収納 | `product_storage_attributes.taste_id` | `storageTasteIds`（`ProductCategoryFilter`） |

#### テイストマスタの構造（調査済み）

テイストマスタはカテゴリ別に **3 テーブルに分離**されており、各テーブルの `taste_id` は独立した連番。

| テーブル | 用途 | 主キー |
|---------|------|-------|
| `desk_tastes` | デスクのテイスト | `taste_id`（独立連番） |
| `chair_tastes` | チェアのテイスト | `taste_id`（独立連番） |
| `storage_tastes` | 収納のテイスト | `taste_id`（独立連番） |

同名のテイスト（例: 「ナチュラル」）でも各テーブル間で `taste_id` は異なる。

#### 検索結果画面での適用仕様

**フロントエンド送信:**
- パラメーター名: `taste`（複数選択可）
- 送信値: テイストの **`display_name` 文字列**（例: `taste=ナチュラル&taste=モダン`）
- 選択肢の表示順: 各マスタテーブルの `sort_order` カラムに従う

**サービス層の変換処理（新規実装）:**
- `taste` パラメーターで受け取った `display_name` 文字列をもとに、各テイストテーブルで ID を引き当てる
- 引き当てた ID を既存の `ProductCategoryFilter` の各フィールドに設定する
  - `desk_tastes.display_name IN (選択表示名)` → `ProductCategoryFilter.deskTasteIds`
  - `chair_tastes.display_name IN (選択表示名)` → `ProductCategoryFilter.chairTasteIds`
  - `storage_tastes.display_name IN (選択表示名)` → `ProductCategoryFilter.storageTasteIds`
- `ProductSearchCondition` に新フィールドは追加しない（既存 `ProductCategoryFilter` の 3 フィールドを流用）

#### 検索結果ヒット条件（テイスト絞込あり時）

商品が以下のいずれか一つでも満たす場合にヒットとする（**OR 条件**）。

- `product_desk_attributes.taste_id IN (deskTasteIds)`
- `product_chair_attributes.taste_id IN (chairTasteIds)`
- `product_storage_attributes.taste_id IN (storageTasteIds)`

> **既存実装との差異:** カテゴリ一覧ページの `DeskAttributeFilter` / `ChairAttributeFilter` / `StorageAttributeFilter` は各カテゴリ限定で `AND EXISTS` を使う。検索結果画面はカテゴリ横断のため、テイスト絞込専用に `OR EXISTS` を使う新 SQL フラグメントが必要になる。既存フラグメントの流用は不可。

#### UI 仕様

- チェックボックス形式（複数選択可）— カテゴリ一覧のテイスト絞込と同じデザイン
- ページ遷移・フィルター変更時に選択状態を保持する
- 絞込件数をリアルタイム表示する必要はない（サーバーへの送信後に件数更新）
- 選択したテイストはフォームの hidden フィールドでページング時も保持する

---

### FR-05: 上記以外は現状の検索機能を踏襲する

以下の仕様は変更しない。

| 機能 | 備考 |
|------|------|
| 在庫あり絞込（`inStockOnly`） | 変更なし |
| 価格帯絞込（`priceBand`） | 変更なし |
| カラー絞込（`color`） | 変更なし |
| ソート順（`sort`） | 変更なし |
| ページング（`page`、`size`） | 変更なし |
| キーワードなし時の動作 | 変更なし（全商品対象） |
| 販売期間フィルタ（`sale_start_at` / `sale_end_at`） | 変更なし |

---

## 5. 非機能要件

### PER-01: 検索結果の初回表示速度

| 指標 | 目標値 |
|------|--------|
| 対象商品数（最大） | 1,000 件 |
| 初回表示応答時間（P95） | 2 秒以内 |

- ここでの「初回表示」とは、ブラウザがリクエストを送信してから検索結果一覧が描画されるまでを指す。
- 計測環境: ローカル Docker 環境ではなく、ステージング相当の環境で確認すること。

---

## 6. 影響範囲

### 6-1. バックエンド

| レイヤー | ファイル | 変更内容 |
|----------|---------|---------|
| Repository | `repository/ProductRepository.java` | `toKeywordLike()` を前方一致版 `toKeywordPrefix()` に変更（FR-01）。`params.put("keywordPrefix", ...)` の追加（FR-01）。全角・半角正規化処理の追加（FR-03） |
| Mapper XML | `resources/mappers/ProductMapper.xml` | `BaseProductWhere` の `keywordLike` 条件を `keywordPrefix` に変更（FR-01）。`variation_name` / `description` を検索対象に追加（FR-02）。テイスト絞込用 OR EXISTS フラグメントを新規追加（FR-04） |
| Model | `model/product/ProductSearchCondition.java` | 変更なし（既存 `categoryFilter` の 3 フィールドを流用） |
| Model | `model/product/ProductCategoryFilter.java` | 変更なし（既存 `deskTasteIds` / `chairTasteIds` / `storageTasteIds` を流用） |
| Service | サービス層（`ProductListSearchService` 等） | `taste` パラメーターの `display_name` → 各カテゴリ `taste_id` への変換処理を追加（FR-04）。変換後の ID を `ProductCategoryFilter` の既存 3 フィールドに設定 |
| Filter Option Service | `ProductFilterOptionService` | 全カテゴリのテイスト選択肢を返す新メソッド（例: `allTasteOptions()`）を追加（FR-04）。`display_name` 文字列 → 各テーブルの `taste_id` 変換のための参照メソッドを追加 |
| Controller | `web/CatalogController.java`（`searchResults` メソッド） | `@RequestParam(name = "taste") List<String>` の受け取りを追加（FR-04）。テイスト選択肢と選択済み値をモデルに追加 |

### 6-2. フロントエンド（Thymeleaf）

| ファイル | 変更内容 |
|---------|---------|
| `templates/pages/product-list-search-results.html` | テイスト絞込チェックボックスの追加（FR-04）。hidden フィールドでのパラメーター保持 |
| `templates/fragments/` （絞込部品があれば） | テイスト絞込コンポーネントの追加または共通化 |

### 6-3. データベース

| 対象 | 変更内容 |
|------|---------|
| スキーマ | 基本的に変更なし。正規化方針が案Aになった場合は、`products` / `product_variants` テーブルへの正規化インデックス追加 |
| マスタデータ | テイストマスタは `desk_tastes` / `chair_tastes` / `storage_tastes` の 3 テーブルに分離済み（変更不要） |

---

## 7. 制約・前提条件

1. **テイストマスタは 3 テーブル分離**  
   `desk_tastes` / `chair_tastes` / `storage_tastes` は独立した連番（同名テイストでも `taste_id` が異なる）。`display_name` 文字列を検索キーとして各テーブルを引き当て、取得した `taste_id` を既存フィルタフィールドに設定する方式とする。

2. **`product_code` の命名規則**  
   FR-01 の前方一致が意図どおりに機能するには、商品コードが一意なプレフィックスを持つ命名規則であることが望ましい。命名規則を仕様書またはシードデータで確認すること。

3. **正規化方針の確定**  
   FR-03 の全角・半角正規化の実装方針（案A/案B）は、パフォーマンス計測後に確定する。最初は案B（クエリ内完結）で実装し、PER-01 を満たせない場合は案A に移行する。

4. **既存の検索機能との後方互換性**  
   本変更により現状ヒットしていた検索結果がヒットしなくなるケース（商品コードの中間一致が廃止されることによるもの）が発生する。これは設計上の意図した仕様変更であるが、リリース前にリグレッションテストで確認すること。

---

## 8. 未決事項

| No. | 内容 | 確認先 | 期限 | 状態 |
|-----|------|--------|------|------|
| U-01 | テイストマスタが全カテゴリ共通 ID で管理されているか | DB スキーマ調査 | — | ✅ 解決済み（3 テーブル分離・`display_name` 文字列で突き合わせる方式に確定） |
| U-02 | 商品コード検索が「前方一致のみ」か「完全一致 + 前方一致」か | PO 確認 | — | ✅ 解決済み（前方一致のみ） |
| U-03 | `variation_name`・`description` を検索対象に加えることでノイズが増えないか | PO / ステークホルダー確認 | 実装着手前 | ⏳ 未解決 |
| U-04 | 全角・半角正規化の実装方針（案A / 案B）の最終決定 | パフォーマンス計測後 | 実装中 | ⏳ 未解決 |
| U-05 | テイスト絞込 UI のデザイン詳細（カテゴリ一覧と共通部品化するか） | フロントエンド担当確認 | 実装着手前 | ⏳ 未解決 |

---

## 9. 受け入れ条件

### AC-01: 商品コードの前方一致検索

- [ ] `DK-100` と入力したとき、`product_code = 'DK-100'` の商品がヒットする（前方一致が完全一致を包含）
- [ ] `DK-1` と入力したとき、`product_code = 'DK-100'` の商品がヒットする（前方一致）
- [ ] `DK` と入力したとき、`product_code = 'DK-100'` の商品がヒットする（前方一致）
- [ ] `100` と入力したとき、`product_code = 'DK-100'` の商品が商品コード一致でヒットしない（`product_name` 等に `100` を含む場合は除く）

### AC-02: 全角・半角の非区別

- [ ] `DK-100`（半角）と `ＤＫ－１００`（全角）で同じ検索結果が返される
- [ ] `ｱｰﾑﾁｪｱ`（半角カタカナ）と `アームチェア`（全角カタカナ）で同じ検索結果が返される

### AC-03: 検索対象の拡張

- [ ] `variation_name` にのみ含まれるキーワードで商品がヒットする
- [ ] `description` にのみ含まれるキーワードで商品がヒットする

### AC-04: テイスト絞込

- [ ] 検索結果画面にテイスト絞込チェックボックスが表示される
- [ ] テイストを選択すると、そのテイストに該当する商品のみに絞り込まれる
- [ ] 複数テイストを選択すると OR 条件で絞り込まれる
- [ ] ページング後もテイスト選択状態が保持される

### AC-05: 既存機能の非破壊

- [ ] 在庫あり・価格帯・カラー絞込が従来と同様に動作する
- [ ] ソート・ページングが従来と同様に動作する

---

## 10. 関連資料

- [onboarding.md](../onboarding.md) — プロジェクト概要・アーキテクチャ・DB 設計
- [sql/schema/products.sql](../../sql/schema/products.sql) — 商品・バリアントテーブル DDL
- [sql/schema/masters.sql](../../sql/schema/masters.sql) — テイストマスタ DDL
- [src/main/resources/mappers/ProductMapper.xml](../../src/main/resources/mappers/ProductMapper.xml) — 現行の検索クエリ実装
