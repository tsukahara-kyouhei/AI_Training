# FEAT-001 改修1: テイストマスタの統合

> 作成日: 2026-03-31
> ブランチ: `ebihara.toshikazu`
> 前提: FEAT-001 本実装済み（`docs/issues/FEAT-001-TODO.md` 参照）

---

## 1. 背景・目的

### 1.1 現状の問題点

FEAT-001 の初期実装では、テイストマスタが商品カテゴリ別に 3 テーブルに分散している。

| テーブル名 | スキーマファイル | 参照元 |
|---|---|---|
| `desk_tastes` | `sql/schema/masters.sql` | `product_desk_attributes.taste_id` |
| `chair_tastes` | `sql/schema/masters.sql` | `product_chair_attributes.taste_id` |
| `storage_tastes` | `sql/schema/masters.sql` | `product_storage_attributes.taste_id` |

この構造により以下の問題が発生している。

1. **テイスト選択肢取得の複雑さ**
   検索結果画面の絞込条件としてテイスト一覧を取得するには、3テーブルを `UNION ALL` して重複排除する必要がある（`selectUnifiedSearchTasteOptions` クエリ）。

2. **検索フィルタの冗長さ**
   テイスト絞り込みSQL（`SearchTasteFilter`）はデスク・チェア・収納それぞれに対して `EXISTS` サブクエリを 3 本発行し、`OR` で結合している。新規カテゴリ追加時に漏れが生じやすい。

3. **マスタ管理の分散**
   テイストデータの追加・変更・削除が各カテゴリのマスタテーブルに対して個別に必要となり、管理コストが高い。

### 1.2 目的

テイストマスタを単一テーブル `tastes` に統合することで、以下を実現する。

- テイスト選択肢取得クエリの単純化（UNION 不要）
- 検索フィルタ SQL の単純化（複数 EXISTS の除去）
- マスタデータの一元管理

---

## 2. 要件

### 2.1 機能要件

#### FR-1: テイストマスタの統合

- `desk_tastes`、`chair_tastes`、`storage_tastes` の 3 テーブルの情報を集約した新テーブル `tastes` を作成する。
- `tastes` テーブルのカラム構成は既存 3 テーブルの共通構造に準拠する（`taste_id`、`display_name`、`sort_order`、`is_active`、`created_at`、`updated_at`）。
- 既存 3 テーブルのデータを `tastes` テーブルへマイグレーションする。マイグレーション後の `display_name` は重複を許さず、重複エントリが存在する場合は `sort_order` の小さいものを優先して取り込む。
- 既存の `desk_tastes`、`chair_tastes`、`storage_tastes` テーブルは、`product_*_attributes` テーブルの外部キー参照を `tastes` テーブルへ切り替えた後に削除する。

#### FR-2: 商品属性テーブルの外部キー変更

- `product_desk_attributes.taste_id` の外部キー参照先を `desk_tastes` から `tastes` に変更する。
- `product_chair_attributes.taste_id` の外部キー参照先を `chair_tastes` から `tastes` に変更する。
- `product_storage_attributes.taste_id` の外部キー参照先を `storage_tastes` から `tastes` に変更する。

#### FR-3: テイスト絞込選択肢の取得変更

- 検索結果画面の絞込条件「テイスト」の選択肢は、`tastes` テーブルから直接取得するよう変更する。
- 取得条件: `is_active = TRUE`
- 並び順: `sort_order ASC`、`taste_id ASC`

#### FR-4: テイスト絞込検索の変更

- 検索結果画面でテイスト絞り込みを行う場合、`product_*_attributes` テーブルから `tastes` テーブルへ直接 JOIN して `display_name` で絞り込む。
- デスク・チェア・収納のいずれかにマッチすれば検索対象と見なす（カテゴリ横断の OR 結合は維持）。

#### FR-5: カテゴリ別テイストフィルタの集約

- カテゴリ詳細絞込で使用しているテイスト ID パラメータ `deskTasteIds`、`chairTasteIds`、`storageTasteIds` を廃止し、単一の `tasteIds` に集約する。
- `DeskAttributeFilter`・`ChairAttributeFilter`・`StorageAttributeFilter` 内のテイスト条件はいずれも `tastes.taste_id IN (tasteIds)` を参照するよう変更する。
- カテゴリ別テイスト選択肢取得クエリ（`selectActiveDeskTasteOptions`・`selectActiveChairTasteOptions`・`selectActiveStorageTasteOptions`）は、単一の `selectActiveTasteOptions` クエリに統合し、`tastes` テーブルを直接参照する。
- `ProductSearchCondition`・`ProductRepository`・`ProductFilterOptionRepository`・`ProductFilterOptionService`・`CatalogController` を含むアプリケーション層も合わせて変更する。

### 2.2 非機能要件

- データマイグレーションはスキーマファイルへの SQL 追記で表現し、冪等性（`CREATE TABLE IF NOT EXISTS`、`INSERT ... ON CONFLICT DO NOTHING` 等）を確保する。
- 検索結果画面の外部仕様（UI・`taste` パラメータ名・検索動作）は変更しない。

---

## 3. 変更対象

### 3.1 DB（SQLスキーマ・シードデータ）

| ファイル | 変更内容 |
|---|---|
| `sql/schema/masters.sql` | `tastes` テーブルの追加、既存テイスト 3 テーブルの削除または廃止コメント追加 |
| `sql/schema/products.sql` | `product_desk_attributes`・`product_chair_attributes`・`product_storage_attributes` の `taste_id` FK 変更先を `tastes` に更新 |
| `sql/seed/masters.sql` | `desk_tastes`・`chair_tastes`・`storage_tastes` への INSERT 3 件を廃止し、`tastes` への INSERT 1 件に統合 |

### 3.2 Mapper XML

| ファイル | 変更内容 |
|---|---|
| `src/main/resources/mappers/ProductMapper.xml` | `selectUnifiedSearchTasteOptions`: UNION ALL クエリを `tastes` テーブルへの単純 SELECT に差し替え |
| 同上 | `SearchTasteFilter`: 3本の EXISTS サブクエリを、各属性テーブルから `tastes` への JOIN に変更 |
| 同上 | `DeskAttributeFilter`・`ChairAttributeFilter`・`StorageAttributeFilter`: `deskTasteIds`/`chairTasteIds`/`storageTasteIds` 条件を `tasteIds` を参照する統一条件に変更 |
| 同上 | `selectActiveDeskTasteOptions`・`selectActiveChairTasteOptions`・`selectActiveStorageTasteOptions`: 3クエリを廃止し、`tastes` を参照する `selectActiveTasteOptions` に統合 |

### 3.3 アプリケーションコード

| ファイル | 変更内容 |
|---|---|
| `src/main/java/.../model/product/ProductSearchCondition.java` | `deskTasteIds`・`chairTasteIds`・`storageTasteIds` フィールドを廃止し、`List<Long> tasteIds` に集約 |
| `src/main/java/.../repository/ProductRepository.java` | `buildSearchParams()` 内の 3 系統テイスト ID 設定を `tasteIds` 単一設定に変更 |
| `src/main/java/.../repository/ProductFilterOptionRepository.java` | `findActiveDeskTasteOptions()`・`findActiveChairTasteOptions()`・`findActiveStorageTasteOptions()` を廃止し、`findActiveTasteOptions()` に統合 |
| `src/main/java/.../service/product/ProductFilterOptionService.java` | 3 系統テイスト選択肢取得メソッドを廃止し、`loadActiveTasteOptions()` に統合 |
| `src/main/java/.../service/product/ProductListSearchService.java` | `buildCondition()` の `tasteIds` 引数変更に合わせてシグネチャを更新 |
| `src/main/java/.../web/CatalogController.java` | テイスト絞込パラメータを `tasteIds`（`List<Long>`）として受け取るよう変更 |

---

## 4. データモデル変更

### 4.1 新規テーブル: `tastes`

```sql
CREATE TABLE IF NOT EXISTS tastes (
    taste_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order   INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 廃止テーブル（FK切り替え後に削除）

- `desk_tastes`
- `chair_tastes`
- `storage_tastes`

### 4.3 FK 変更後のリレーション

```
products (product_id)
    ├─ product_desk_attributes    (taste_id → tastes.taste_id)
    ├─ product_chair_attributes   (taste_id → tastes.taste_id)
    └─ product_storage_attributes (taste_id → tastes.taste_id)
```

### 4.4 データマイグレーション方針

1. `tastes` テーブルを新規作成する。
2. `desk_tastes`、`chair_tastes`、`storage_tastes` の各データを `tastes` に INSERT する。`display_name` が重複する場合は `ON CONFLICT (display_name) DO NOTHING` で先着優先とする。
3. 各属性テーブルの `taste_id` を、旧テーブルの `display_name` を経由して新 `tastes.taste_id` に UPDATE する。
4. 旧テーブルへの外部キー制約を削除し、`tastes` への外部キー制約を追加する。
5. 旧 3 テーブルを DROP する。

---

## 5. SQL 変更イメージ

### 5.1 変更後の `selectUnifiedSearchTasteOptions`（Mapper XML）

```sql
-- 変更前（UNION ALL）
SELECT NULL AS option_id, display_name
FROM (
  SELECT display_name, sort_order FROM desk_tastes    WHERE is_active = TRUE
  UNION ALL
  SELECT display_name, sort_order FROM chair_tastes   WHERE is_active = TRUE
  UNION ALL
  SELECT display_name, sort_order FROM storage_tastes WHERE is_active = TRUE
) t
GROUP BY display_name
ORDER BY MIN(sort_order) ASC

-- 変更後（tastes 直接参照）
SELECT NULL AS option_id, display_name
FROM tastes
WHERE is_active = TRUE
ORDER BY sort_order ASC, taste_id ASC
```

### 5.2 変更後の `SearchTasteFilter`（Mapper XML）

```sql
-- 変更前（3本の EXISTS を OR）
AND (
  EXISTS (
    SELECT 1 FROM product_desk_attributes da
    JOIN desk_tastes dt ON da.taste_id = dt.taste_id
    WHERE da.product_id = p.product_id
      AND dt.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1 FROM product_chair_attributes ca
    JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
    WHERE ca.product_id = p.product_id
      AND ct.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1 FROM product_storage_attributes sa
    JOIN storage_tastes st ON sa.taste_id = st.taste_id
    WHERE sa.product_id = p.product_id
      AND st.display_name IN (...)
  )
)

-- 変更後（各属性テーブルから tastes へ JOIN）
AND (
  EXISTS (
    SELECT 1 FROM product_desk_attributes da
    JOIN tastes t ON da.taste_id = t.taste_id
    WHERE da.product_id = p.product_id
      AND t.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1 FROM product_chair_attributes ca
    JOIN tastes t ON ca.taste_id = t.taste_id
    WHERE ca.product_id = p.product_id
      AND t.display_name IN (...)
  )
  OR EXISTS (
    SELECT 1 FROM product_storage_attributes sa
    JOIN tastes t ON sa.taste_id = t.taste_id
    WHERE sa.product_id = p.product_id
      AND t.display_name IN (...)
  )
)
```

### 5.3 変更後のカテゴリ別テイストフィルタ（`DeskAttributeFilter` 等）

```sql
-- 変更前（各属性フィルタ内でカテゴリ別 taste テーブルを参照）
-- DeskAttributeFilter 内
<if test="deskTasteIds != null and deskTasteIds.size() > 0">
    AND da.taste_id IN (#{deskTasteIds[0]}, ...)
</if>
-- ChairAttributeFilter 内
<if test="chairTasteIds != null and chairTasteIds.size() > 0">
    AND ca.taste_id IN (#{chairTasteIds[0]}, ...)
</if>
-- StorageAttributeFilter 内
<if test="storageTasteIds != null and storageTasteIds.size() > 0">
    AND sa.taste_id IN (#{storageTasteIds[0]}, ...)
</if>

-- 変更後（共通の tasteIds を tastes テーブル経由で参照）
-- DeskAttributeFilter 内
<if test="tasteIds != null and tasteIds.size() > 0">
    AND da.taste_id IN (#{tasteIds[0]}, ...)
</if>
-- ChairAttributeFilter 内
<if test="tasteIds != null and tasteIds.size() > 0">
    AND ca.taste_id IN (#{tasteIds[0]}, ...)
</if>
-- StorageAttributeFilter 内
<if test="tasteIds != null and tasteIds.size() > 0">
    AND sa.taste_id IN (#{tasteIds[0]}, ...)
</if>
```

### 5.4 変更後のテイスト選択肢取得クエリ

```sql
-- 変更前（カテゴリ別に3クエリ存在）
-- selectActiveDeskTasteOptions
SELECT taste_id AS option_id, display_name FROM desk_tastes
WHERE is_active = TRUE ORDER BY sort_order ASC, taste_id ASC

-- selectActiveChairTasteOptions
SELECT taste_id AS option_id, display_name FROM chair_tastes
WHERE is_active = TRUE ORDER BY sort_order ASC, taste_id ASC

-- selectActiveStorageTasteOptions
SELECT taste_id AS option_id, display_name FROM storage_tastes
WHERE is_active = TRUE ORDER BY sort_order ASC, taste_id ASC

-- 変更後（tastes テーブルを参照する単一クエリ）
-- selectActiveTasteOptions
SELECT taste_id AS option_id, display_name
FROM tastes
WHERE is_active = TRUE
ORDER BY sort_order ASC, taste_id ASC
```

### 5.5 変更後のシードデータ（`sql/seed/masters.sql`）

```sql
-- 変更前（カテゴリ別に3件の INSERT）
INSERT INTO desk_tastes (display_name, sort_order, is_active) VALUES
  ('ベーシック', 1, TRUE), ('カジュアル', 2, TRUE), ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE), ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO chair_tastes (display_name, sort_order, is_active) VALUES
  ('ベーシック', 1, TRUE), ('カジュアル', 2, TRUE), ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE), ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO storage_tastes (display_name, sort_order, is_active) VALUES
  ('ベーシック', 1, TRUE), ('カジュアル', 2, TRUE), ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE), ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;

-- 変更後（tastes テーブルへの単一 INSERT）
INSERT INTO tastes (display_name, sort_order, is_active) VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル', 3, TRUE),
  ('モダン', 4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;
```

---

## 6. 受け入れ条件

| No. | 観点 | 確認内容 |
|---|---|---|
| AC-1 | DB | `tastes` テーブルが作成されていること |
| AC-2 | DB | `tastes` に旧 3 テーブルのデータが漏れなく集約されていること（重複は単一エントリ） |
| AC-3 | DB | `product_desk_attributes`・`product_chair_attributes`・`product_storage_attributes` の `taste_id` FK が `tastes` を参照していること |
| AC-4 | DB | `desk_tastes`・`chair_tastes`・`storage_tastes` テーブルが削除されていること |
| AC-5 | 機能 | 検索結果画面のテイスト絞込選択肢が `tastes` テーブルから取得されること |
| AC-6 | 機能 | テイストを選択して検索した場合、カテゴリ横断で該当商品が表示されること |
| AC-7 | 機能 | テイストを選択しない場合、絞込なしで全商品が表示されること（既存動作維持） |
| AC-8 | 機能 | カテゴリ別テイストフィルタ（デスク・チェア・収納の個別絞込）が `tasteIds` で動作すること |
| AC-9 | 品質 | 既存の単体テスト（`ProductFilterOptionServiceTest` 等）がパスすること |
| AC-10 | 品質 | `deskTasteIds`・`chairTasteIds`・`storageTasteIds` に関連するコードがすべて削除されていること |
| AC-11 | DB | `sql/seed/masters.sql` の旧 3 テーブルへの INSERT が削除され、`tastes` への INSERT に置き換えられていること |

---

## 7. 影響調査が必要な箇所

以下は本要件定義の範囲で変更不要と判断しているが、実装前に再確認すること。

- テンプレート（`product-list-search-results.html` を除く各カテゴリ絞込画面）でテイスト絞込フォームを生成している箇所があれば、`tasteIds` パラメータに統一されていることを確認すること。

---

## 8. 未決事項

なし
