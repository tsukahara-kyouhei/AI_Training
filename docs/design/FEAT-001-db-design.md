# FEAT-001 DB設計書

| 項目 | 内容 |
|------|------|
| Issue ID | FEAT-001 |
| ドキュメント種別 | DB設計書 |
| 作成日 | 2026-04-24 |

---

## 1. 変更概要

テイストマスタを3テーブル（`desk_tastes` / `chair_tastes` / `storage_tastes`）から
1テーブル（`tastes`）に統合する。

| 区分 | 対象 | 内容 |
|------|------|------|
| 新設 | `tastes` | テイスト統合マスタ |
| FK変更 | `product_desk_attributes.taste_id` | `desk_tastes` → `tastes` |
| FK変更 | `product_chair_attributes.taste_id` | `chair_tastes` → `tastes` |
| FK変更 | `product_storage_attributes.taste_id` | `storage_tastes` → `tastes` |
| 廃止 | `desk_tastes` | DROP |
| 廃止 | `chair_tastes` | DROP |
| 廃止 | `storage_tastes` | DROP |

---

## 2. 新設テーブル定義

### `tastes`（テイスト統合マスタ）

| カラム | 型 | 制約 | 説明 |
|--------|----|------|------|
| `taste_id` | `BIGINT` | PK, GENERATED ALWAYS AS IDENTITY | テイストID |
| `display_name` | `VARCHAR(100)` | NOT NULL, UNIQUE | 表示名 |
| `sort_order` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK >= 0 | 表示順 |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | 有効フラグ |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**インデックス**

| インデックス名 | カラム | 説明 |
|--------------|--------|------|
| `idx_tastes_active_sort` | `(is_active, sort_order)` | 一覧取得時の絞込・ソート用 |

---

## 3. データ移行方針

既存3テーブルの `display_name` を `UNION` して重複排除し、新 `tastes` テーブルへ投入する。
同名エントリの `sort_order` は `MIN` を採用する。

現行シードデータでは3テーブルすべて同一の5件を持つため、移行後の `tastes` テーブルは以下になる。

| taste_id（採番後） | display_name | sort_order |
|---|---|---|
| 1 | ベーシック | 1 |
| 2 | カジュアル | 2 |
| 3 | シンプル | 3 |
| 4 | モダン | 4 |
| 5 | ナチュラル | 5 |

カテゴリ属性テーブルの `taste_id` は `display_name` で突き合わせて新 `tastes.taste_id` に更新する。
現行データでは旧テーブルの `taste_id` と新テーブルの `taste_id` が一致するが、
スクリプトは `display_name` ベースの突き合わせで行い、採番結果に依存しない実装とする。

---

## 4. マイグレーション SQL（`V2__unify_taste_master.sql`）

手動実行。アプリケーションコードのデプロイと同時に適用する。

```sql
-- ============================================================
-- FEAT-001: テイストマスタ統合
-- 実行タイミング: アプリデプロイと同時
-- ============================================================

-- 1. 統合テイストマスタ テーブル新設
CREATE TABLE tastes (
    taste_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order   INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tastes_active_sort ON tastes (is_active, sort_order);

COMMENT ON TABLE  tastes              IS 'テイスト統合マスタ';
COMMENT ON COLUMN tastes.taste_id    IS 'テイストID';
COMMENT ON COLUMN tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN tastes.sort_order  IS '表示順';
COMMENT ON COLUMN tastes.is_active   IS '有効フラグ';
COMMENT ON COLUMN tastes.created_at  IS '作成日時';
COMMENT ON COLUMN tastes.updated_at  IS '更新日時';

-- 2. 旧3テーブルを UNION して重複排除し tastes へ移行
INSERT INTO tastes (display_name, sort_order, is_active)
SELECT
    display_name,
    MIN(sort_order) AS sort_order,
    BOOL_OR(is_active) AS is_active
FROM (
    SELECT display_name, sort_order, is_active FROM desk_tastes
    UNION ALL
    SELECT display_name, sort_order, is_active FROM chair_tastes
    UNION ALL
    SELECT display_name, sort_order, is_active FROM storage_tastes
) combined
GROUP BY display_name
ORDER BY MIN(sort_order);

-- 3. product_desk_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_desk_attributes pda
SET taste_id = t.taste_id
FROM desk_tastes dt
JOIN tastes t ON t.display_name = dt.display_name
WHERE pda.taste_id = dt.taste_id;

-- 4. product_chair_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_chair_attributes pca
SET taste_id = t.taste_id
FROM chair_tastes ct
JOIN tastes t ON t.display_name = ct.display_name
WHERE pca.taste_id = ct.taste_id;

-- 5. product_storage_attributes.taste_id を新 tastes.taste_id へ更新
UPDATE product_storage_attributes psa
SET taste_id = t.taste_id
FROM storage_tastes st
JOIN tastes t ON t.display_name = st.display_name
WHERE psa.taste_id = st.taste_id;

-- 6. product_desk_attributes の FK を tastes へ張り替え
ALTER TABLE product_desk_attributes
    DROP CONSTRAINT fk_product_desk_attributes_taste;
ALTER TABLE product_desk_attributes
    ADD CONSTRAINT fk_product_desk_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 7. product_chair_attributes の FK を tastes へ張り替え
ALTER TABLE product_chair_attributes
    DROP CONSTRAINT fk_product_chair_attributes_taste;
ALTER TABLE product_chair_attributes
    ADD CONSTRAINT fk_product_chair_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 8. product_storage_attributes の FK を tastes へ張り替え
ALTER TABLE product_storage_attributes
    DROP CONSTRAINT fk_product_storage_attributes_taste;
ALTER TABLE product_storage_attributes
    ADD CONSTRAINT fk_product_storage_attributes_taste
    FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- 9. 旧テーブル DROP（FK 参照がなくなった後に実行）
DROP TABLE desk_tastes;
DROP TABLE chair_tastes;
DROP TABLE storage_tastes;
```

---

## 5. シードデータ変更方針

以下のファイルを `V2__unify_taste_master.sql` 適用後の状態に合わせて更新する。

### `sql/seed/masters.sql`

削除対象（旧テーブルへの INSERT）：
```sql
-- 削除
INSERT INTO desk_tastes ...
INSERT INTO chair_tastes ...
INSERT INTO storage_tastes ...
```

追加（新テーブルへの INSERT）：
```sql
INSERT INTO tastes (display_name, sort_order, is_active)
VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル',   3, TRUE),
  ('モダン',     4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;
```

### `sql/seed/test-data/` 配下

テイストIDを直接参照している INSERT 文がある場合は、新 `tastes.taste_id` に合わせて確認・更新する。
（現行データでは旧 taste_id と新 taste_id が一致する想定だが、要確認）

---

## 6. ロールバック方針

本マイグレーションは `DROP TABLE` を含むため、適用後の自動ロールバックはできない。
問題発生時はアプリケーションを旧バージョンに戻したうえで、
バックアップから手動リストアを行う（本番環境ではデプロイ前にスナップショット取得を推奨）。
