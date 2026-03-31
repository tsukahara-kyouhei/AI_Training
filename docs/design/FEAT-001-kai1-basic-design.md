# FEAT-001 改修1: テイストマスタ統合 基本設計書

> 要件定義書: `docs/issues/FEAT-001-requirements_kai1.md`
> 作成日: 2026-03-31

---

## 1. 概要

`desk_tastes` / `chair_tastes` / `storage_tastes` の 3 テーブルに分散しているテイストマスタを
単一の `tastes` テーブルに統合し、テイスト絞込の取得・検索クエリを単純化する。
あわせて、カテゴリ別絞込で使用していたカテゴリ別テイスト ID パラメータ
（`deskTasteIds` / `chairTasteIds` / `storageTasteIds`）を単一の `tasteIds` に集約する。

---

## 2. 変更コンポーネント一覧

| レイヤ | ファイル | 変更種別 | 主な変更内容 |
|--------|---------|---------|------------|
| DB スキーマ | `sql/schema/masters.sql` | 変更 | `tastes` テーブル追加、旧 3 テーブル削除 |
| DB スキーマ | `sql/schema/products.sql` | 変更 | FK 変更・データマイグレーション SQL |
| シードデータ | `sql/seed/masters.sql` | 変更 | 旧 3 テーブルへの INSERT → `tastes` への INSERT に統合 |
| Mapper XML | `ProductMapper.xml` | 変更 | 6 箇所（詳細は SQL 設計書参照） |
| Mapper IF | `ProductMapper.java` | 変更 | 旧 3 メソッド廃止・`selectActiveTasteOptions` 追加 |
| Model | `ProductCategoryFilter.java` | 変更 | taste 3 フィールド → `tasteIds` 1 フィールドに集約 |
| Model | `ProductFilterOptionsBundle.java` | 変更 | taste 3 フィールド → `tasteOptions` 1 フィールドに集約 |
| Repository | `ProductFilterOptionRepository.java` | 変更 | 旧 3 メソッド廃止・`findActiveTasteOptions` 追加 |
| Repository | `ProductRepository.java` | 変更 | `buildSearchParams()` の taste 関連パラメータ集約 |
| Service | `ProductFilterOptionService.java` | 変更 | `loadOptionsBundle` / `buildXxxFilter` シグネチャ変更 |
| Controller | `CatalogController.java` | 変更 | `@RequestParam` パラメータ名・モデル属性名変更 |
| Template | `product-list-category-desk.html` | 変更 | フォーム input name / Thymeleaf 変数名変更 |
| Template | `product-list-category-chair.html` | 変更 | 同上 |
| Template | `product-list-category-storage.html` | 変更 | 同上 |

---

## 3. アーキテクチャ概要

本改修における新規レイヤ追加はない。既存レイヤの変更のみで対応する。

### 3.1 テイスト取得フロー（カテゴリ一覧画面）

```
ブラウザ: GET /categories/desks?taste=1&taste=3
    │
    ▼
CatalogController.desks()
  @RequestParam(name = "taste") List<Integer> rawTasteIds  ← パラメータ名を統一
    │
    ▼
ProductFilterOptionService.buildDeskFilter(..., rawTasteIds)
  allowedTasteIds = tasteOptions(optionsBundle)  ← 統合テイスト選択肢で検証
  → ProductCategoryFilter(tasteIds = [1,3])      ← tasteIds 単一フィールド
    │
    ▼
ProductRepository.buildSearchParams()
  params.put("tasteIds", [1,3])
  hasDeskFilter = true  ← tasteIds が非空なので
    │
    ▼
ProductMapper.xml / DeskAttributeFilter
  AND da.taste_id IN (1, 3)   ← tastes テーブルへの FK で解決
```

### 3.2 テイスト選択肢取得フロー（カテゴリ一覧画面）

```
変更前:
  loadOptionsBundle()
    → findActiveDeskTasteOptions()   → SELECT FROM desk_tastes
    → findActiveChairTasteOptions()  → SELECT FROM chair_tastes
    → findActiveStorageTasteOptions()→ SELECT FROM storage_tastes

変更後:
  loadOptionsBundle()
    → findActiveTasteOptions()       → SELECT FROM tastes  （1クエリ）
```

### 3.3 テイスト絞込フロー（検索結果画面・変更なし）

```
ブラウザ: GET /products/search?taste=ナチュラル
    │
    ▼  ← 変更なし（文字列ベースの検索）
CatalogController.searchResults()
    │
    ▼
ProductFilterOptionService.loadUnifiedSearchTasteOptions()
  → findUnifiedSearchTasteOptions()
    → SELECT FROM tastes  ← UNION ALL から単純 SELECT に SQL 変更
    │
    ▼
ProductMapper.xml / SearchTasteFilter
  JOIN tastes t ON da.taste_id = t.taste_id  ← 旧テーブルから tastes に変更
```

---

## 4. テイストフィルタの役割整理

| 系統 | 使用画面 | パラメータ | 型 | 絞込キー |
|------|---------|-----------|-----|---------|
| カテゴリ別 | デスク・チェア・収納 一覧 | `?taste=` (integer) | `List<Integer>` | `tastes.taste_id` |
| 横断検索 | 検索結果 | `?taste=` (string) | `List<String>` | `tastes.display_name` |

> **注意:** 両画面とも URL パラメータ名は `taste` だが、受け取る型が異なる（Integer vs String）。
> これは変更前から同様の設計であり、本改修では変更しない。

---

## 5. テイスト ID の型について

`ProductCategoryFilter.tasteIds` の型は既存の実装に合わせて **`List<Integer>`** を維持する。
`tastes.taste_id` は `BIGINT` だが、実運用値は Integer 範囲内に収まるため問題ない。

---

## 6. 変更影響の波及範囲

### 6.1 `ProductCategoryFilter` の record フィールド変更

3 フィールド（`deskTasteIds` / `chairTasteIds` / `storageTasteIds`）を 1 フィールド（`tasteIds`）に変更するため、
`ProductCategoryFilter` のコンストラクタを呼び出している以下の箇所も変更が必要になる。

| 呼び出し元 | 箇所 |
|-----------|------|
| `ProductFilterOptionService.buildDeskFilter()` | 戻り値生成時 |
| `ProductFilterOptionService.buildChairFilter()` | 戻り値生成時 |
| `ProductFilterOptionService.buildStorageFilter()` | 戻り値生成時 |
| `ProductCategoryFilter.empty()` | 静的ファクトリメソッド |
| `ProductCategoryFilter.normalize()` | 自己参照 |
| `ProductService.normalize()` ※要確認 | 内部で normalize() 呼び出しの可能性 |

### 6.2 `ProductFilterOptionsBundle` の record フィールド変更

3 フィールド → 1 フィールドへの変更により、コンストラクタ呼び出し（`loadOptionsBundle()`）も変更が必要。

---

## 7. 未決事項

なし（要件定義書の全 UQ が解決済み）

---

## 8. 参照設計書

| 設計書 | 内容 |
|--------|------|
| `docs/design/FEAT-001-kai1-sql-design.md` | スキーマ変更・マイグレーション SQL・Mapper XML 変更 |
| `docs/design/FEAT-001-kai1-class-design.md` | Java クラス変更詳細 |
