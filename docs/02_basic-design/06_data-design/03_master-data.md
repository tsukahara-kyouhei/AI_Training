# マスタデータ定義

## 1. マスタテーブル一覧

| テーブル名 | 日本語名 | 参照元 |
|---|---|---|
| desk_top_shapes | デスク天板形状 | product_desk_attributes.top_shape_id |
| desk_tastes | デスクテイスト | product_desk_attributes.taste_id |
| chair_functions | チェア機能 | product_chair_attributes.function_id |
| chair_materials | チェア素材 | product_chair_attributes.material_id |
| chair_tastes | チェアテイスト | product_chair_attributes.taste_id |
| storage_usages | 収納家具用途 | product_storage_attributes.usage_id |
| storage_tastes | 収納家具テイスト | product_storage_attributes.taste_id |

---

## 2. 各マスタテーブル定義

### 2.1 desk_top_shapes（デスク天板形状）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| top_shape_id | BIGINT | NOT NULL | PK |
| shape_name | VARCHAR(60) | NOT NULL | 形状名（例: 長方形、L字型） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.2 desk_tastes（デスクテイスト）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| taste_id | BIGINT | NOT NULL | PK |
| taste_name | VARCHAR(60) | NOT NULL | テイスト名（例: ナチュラル、モダン） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.3 chair_functions（チェア機能）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| function_id | BIGINT | NOT NULL | PK |
| function_name | VARCHAR(60) | NOT NULL | 機能名（例: ハイバック、メッシュ） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.4 chair_materials（チェア素材）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| material_id | BIGINT | NOT NULL | PK |
| material_name | VARCHAR(60) | NOT NULL | 素材名（例: ファブリック、レザー） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.5 chair_tastes（チェアテイスト）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| taste_id | BIGINT | NOT NULL | PK |
| taste_name | VARCHAR(60) | NOT NULL | テイスト名（例: ナチュラル、モダン） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.6 storage_usages（収納家具用途）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| usage_id | BIGINT | NOT NULL | PK |
| usage_name | VARCHAR(60) | NOT NULL | 用途名（例: 書類収納、ロッカー） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

### 2.7 storage_tastes（収納家具テイスト）

| 列名 | 型 | NULL | 説明 |
|---|---|---|---|
| taste_id | BIGINT | NOT NULL | PK |
| taste_name | VARCHAR(60) | NOT NULL | テイスト名（例: ナチュラル、モダン） |
| sort_order | INTEGER | NOT NULL | 表示順 |
| created_at | TIMESTAMPTZ | NOT NULL | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新日時 |

---

## 3. 備考

- 全マスタテーブルのデータは管理画面（本システム外）で管理
- 削除は論理削除でなく物理削除（ただし参照されている場合はRESTRICT制約で防止）
- 初期データは `sql/seed/masters.sql` で投入
