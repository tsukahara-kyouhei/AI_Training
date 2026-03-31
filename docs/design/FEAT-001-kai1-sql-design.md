# FEAT-001 改修1: テイストマスタ統合 SQL 詳細設計書

> 要件定義書: `docs/issues/FEAT-001-requirements_kai1.md`
> 作成日: 2026-03-31

---

## 1. スキーマ変更

### 1.1 新規テーブル追加（`sql/schema/masters.sql` に追記）

```sql
-- テイストマスタ（統合）
CREATE TABLE IF NOT EXISTS tastes (
    taste_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL UNIQUE,
    sort_order   INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tastes_active_sort ON tastes (is_active, sort_order);

COMMENT ON TABLE  tastes              IS 'テイストマスタ（統合）';
COMMENT ON COLUMN tastes.taste_id     IS 'テイストID';
COMMENT ON COLUMN tastes.display_name IS 'テイスト名';
COMMENT ON COLUMN tastes.sort_order   IS '表示順';
COMMENT ON COLUMN tastes.is_active    IS '有効フラグ';
COMMENT ON COLUMN tastes.created_at   IS '作成日時';
COMMENT ON COLUMN tastes.updated_at   IS '更新日時';
```

---

## 2. データマイグレーション（`sql/schema/products.sql` または専用マイグレーションファイルに追記）

以下の SQL を順序通りに実行する。冪等性のために `IF NOT EXISTS` / `ON CONFLICT` を活用する。

### 手順 1: `tastes` テーブルへのデータ投入

```sql
-- desk_tastes のデータを tastes に INSERT（重複時は先着優先）
INSERT INTO tastes (display_name, sort_order, is_active)
SELECT display_name, sort_order, is_active
FROM desk_tastes
ON CONFLICT (display_name) DO NOTHING;

-- chair_tastes のデータを tastes に INSERT（重複時はスキップ）
INSERT INTO tastes (display_name, sort_order, is_active)
SELECT display_name, sort_order, is_active
FROM chair_tastes
ON CONFLICT (display_name) DO NOTHING;

-- storage_tastes のデータを tastes に INSERT（重複時はスキップ）
INSERT INTO tastes (display_name, sort_order, is_active)
SELECT display_name, sort_order, is_active
FROM storage_tastes
ON CONFLICT (display_name) DO NOTHING;
```

> **備考:** 3 テーブルのデータは同一内容（ベーシック〜ナチュラルの 5 件）のため、
> desk_tastes の INSERT 後、残り 2 テーブルは `ON CONFLICT DO NOTHING` で全件スキップされる。

### 手順 2: 属性テーブルの `taste_id` を新 `tastes.taste_id` に更新

```sql
-- product_desk_attributes の taste_id を更新
UPDATE product_desk_attributes da
SET taste_id = t.taste_id
FROM desk_tastes dt
JOIN tastes t ON t.display_name = dt.display_name
WHERE da.taste_id = dt.taste_id;

-- product_chair_attributes の taste_id を更新
UPDATE product_chair_attributes ca
SET taste_id = t.taste_id
FROM chair_tastes ct
JOIN tastes t ON t.display_name = ct.display_name
WHERE ca.taste_id = ct.taste_id;

-- product_storage_attributes の taste_id を更新
UPDATE product_storage_attributes sa
SET taste_id = t.taste_id
FROM storage_tastes st
JOIN tastes t ON t.display_name = st.display_name
WHERE sa.taste_id = st.taste_id;
```

### 手順 3: 旧 FK 制約の削除と新 FK 制約の追加

```sql
-- product_desk_attributes
ALTER TABLE product_desk_attributes
    DROP CONSTRAINT fk_product_desk_attributes_taste;
ALTER TABLE product_desk_attributes
    ADD CONSTRAINT fk_product_desk_attributes_taste
        FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- product_chair_attributes
ALTER TABLE product_chair_attributes
    DROP CONSTRAINT fk_product_chair_attributes_taste;
ALTER TABLE product_chair_attributes
    ADD CONSTRAINT fk_product_chair_attributes_taste
        FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;

-- product_storage_attributes
ALTER TABLE product_storage_attributes
    DROP CONSTRAINT fk_product_storage_attributes_taste;
ALTER TABLE product_storage_attributes
    ADD CONSTRAINT fk_product_storage_attributes_taste
        FOREIGN KEY (taste_id) REFERENCES tastes (taste_id) ON DELETE RESTRICT;
```

### 手順 4: 旧テーブルの削除

```sql
DROP TABLE IF EXISTS desk_tastes;
DROP TABLE IF EXISTS chair_tastes;
DROP TABLE IF EXISTS storage_tastes;
```

---

## 3. シードデータ変更（`sql/seed/masters.sql`）

### 変更前

```sql
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
```

### 変更後

```sql
INSERT INTO tastes (display_name, sort_order, is_active) VALUES
  ('ベーシック', 1, TRUE),
  ('カジュアル', 2, TRUE),
  ('シンプル',   3, TRUE),
  ('モダン',     4, TRUE),
  ('ナチュラル', 5, TRUE)
ON CONFLICT DO NOTHING;
```

---

## 4. `ProductMapper.xml` 変更

### 4.1 `DeskAttributeFilter` フラグメントの変更

テイスト条件のパラメータ名を `deskTasteIds` → `tasteIds` に変更する。

```xml
<!-- 変更前 -->
<if test="deskTasteIds != null and deskTasteIds.size() &gt; 0">
    AND da.taste_id IN
    <foreach collection="deskTasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>

<!-- 変更後 -->
<if test="tasteIds != null and tasteIds.size() &gt; 0">
    AND da.taste_id IN
    <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>
```

### 4.2 `ChairAttributeFilter` フラグメントの変更

テイスト条件のパラメータ名を `chairTasteIds` → `tasteIds` に変更する。

```xml
<!-- 変更前 -->
<if test="chairTasteIds != null and chairTasteIds.size() &gt; 0">
    AND ca.taste_id IN
    <foreach collection="chairTasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>

<!-- 変更後 -->
<if test="tasteIds != null and tasteIds.size() &gt; 0">
    AND ca.taste_id IN
    <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>
```

### 4.3 `StorageAttributeFilter` フラグメントの変更

テイスト条件のパラメータ名を `storageTasteIds` → `tasteIds` に変更する。

```xml
<!-- 変更前 -->
<if test="storageTasteIds != null and storageTasteIds.size() &gt; 0">
    AND sa.taste_id IN
    <foreach collection="storageTasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>

<!-- 変更後 -->
<if test="tasteIds != null and tasteIds.size() &gt; 0">
    AND sa.taste_id IN
    <foreach collection="tasteIds" item="value" open="(" close=")" separator=",">
        #{value}
    </foreach>
</if>
```

> **注意:** `DeskAttributeFilter` / `ChairAttributeFilter` / `StorageAttributeFilter` はそれぞれ独立した
> `EXISTS` 句で囲まれているため、`tasteIds` パラメータ名が重複しても MyBatis の `Map` 参照として問題なし。

### 4.4 `SearchTasteFilter` フラグメントの変更

`desk_tastes` / `chair_tastes` / `storage_tastes` への JOIN を `tastes` への JOIN に変更する。

```xml
<!-- 変更前 -->
<sql id="SearchTasteFilter">
  <if test="hasSearchTasteFilter">
    AND (
      EXISTS (
        SELECT 1 FROM product_desk_attributes da
        JOIN desk_tastes dt ON da.taste_id = dt.taste_id
        WHERE da.product_id = p.product_id
          AND dt.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1 FROM product_chair_attributes ca
        JOIN chair_tastes ct ON ca.taste_id = ct.taste_id
        WHERE ca.product_id = p.product_id
          AND ct.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1 FROM product_storage_attributes sa
        JOIN storage_tastes st ON sa.taste_id = st.taste_id
        WHERE sa.product_id = p.product_id
          AND st.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
    )
  </if>
</sql>

<!-- 変更後 -->
<sql id="SearchTasteFilter">
  <if test="hasSearchTasteFilter">
    AND (
      EXISTS (
        SELECT 1 FROM product_desk_attributes da
        JOIN tastes t ON da.taste_id = t.taste_id
        WHERE da.product_id = p.product_id
          AND t.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1 FROM product_chair_attributes ca
        JOIN tastes t ON ca.taste_id = t.taste_id
        WHERE ca.product_id = p.product_id
          AND t.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
      OR EXISTS (
        SELECT 1 FROM product_storage_attributes sa
        JOIN tastes t ON sa.taste_id = t.taste_id
        WHERE sa.product_id = p.product_id
          AND t.display_name IN
          <foreach collection="searchTasteNames" item="name" open="(" separator="," close=")">
            #{name}
          </foreach>
      )
    )
  </if>
</sql>
```

### 4.5 `selectActiveTasteOptions` クエリの追加

カテゴリ別一覧画面用のテイスト選択肢取得クエリを追加する。

```xml
<!-- 新規追加 -->
<select id="selectActiveTasteOptions" resultMap="ProductFilterOptionRowMap">
    SELECT
    taste_id AS option_id,
    display_name
    FROM tastes
    WHERE is_active = TRUE
    ORDER BY sort_order ASC, taste_id ASC
</select>
```

### 4.6 `selectActiveDeskTasteOptions` / `selectActiveChairTasteOptions` / `selectActiveStorageTasteOptions` の削除

以下の 3 クエリを削除する。

```xml
<!-- 削除対象 -->
<select id="selectActiveDeskTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM desk_tastes ...
</select>

<select id="selectActiveChairTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM chair_tastes ...
</select>

<select id="selectActiveStorageTasteOptions" ...>
    SELECT taste_id AS option_id, display_name FROM storage_tastes ...
</select>
```

### 4.7 `selectUnifiedSearchTasteOptions` クエリの変更

UNION ALL → `tastes` テーブルへの単純 SELECT に変更する。

```xml
<!-- 変更前 -->
<select id="selectUnifiedSearchTasteOptions" resultMap="ProductFilterOptionRowMap">
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
</select>

<!-- 変更後 -->
<select id="selectUnifiedSearchTasteOptions" resultMap="ProductFilterOptionRowMap">
    SELECT
    NULL AS option_id,
    display_name
    FROM tastes
    WHERE is_active = TRUE
    ORDER BY sort_order ASC, taste_id ASC
</select>
```

---

## 5. `ProductMapper.xml` 変更箇所まとめ

| 変更箇所 | 種別 | 内容 |
|---------|------|------|
| `DeskAttributeFilter` | 変更 | `deskTasteIds` → `tasteIds` |
| `ChairAttributeFilter` | 変更 | `chairTasteIds` → `tasteIds` |
| `StorageAttributeFilter` | 変更 | `storageTasteIds` → `tasteIds` |
| `SearchTasteFilter` | 変更 | JOIN 先を `tastes` に変更 |
| `selectActiveTasteOptions` | **新規追加** | `tastes` テーブルから選択肢取得 |
| `selectActiveDeskTasteOptions` | **削除** | `desk_tastes` 参照 → 廃止 |
| `selectActiveChairTasteOptions` | **削除** | `chair_tastes` 参照 → 廃止 |
| `selectActiveStorageTasteOptions` | **削除** | `storage_tastes` 参照 → 廃止 |
| `selectUnifiedSearchTasteOptions` | 変更 | UNION ALL → `tastes` 直接 SELECT |

---

## 6. マイグレーション実行順序

```
1. tastes テーブル作成（§1.1）
2. tastes ヘのデータ投入（§2 手順1）
3. 属性テーブルの taste_id 更新（§2 手順2）
4. FK 制約の差し替え（§2 手順3）
5. 旧テーブル DROP（§2 手順4）
6. アプリ・Mapper 変更のデプロイ
```

> **重要:** 手順 1〜5 は DB 先行適用（アプリデプロイ前に実施）。
> アプリはデプロイ後に旧テーブルにアクセスしないため、ダウンタイムなし移行が可能。
