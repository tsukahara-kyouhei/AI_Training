# お知らせテーブル定義

## 1. announcements（お知らせ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | announcement_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | お知らせID |
| 2 | title | VARCHAR(200) | NOT NULL | - | タイトル |
| 3 | summary | VARCHAR(255) | NOT NULL | - | 要約 |
| 4 | body | TEXT | NULL | - | 本文 |
| 5 | published_start_at | TIMESTAMPTZ | NOT NULL | - | 掲載開始日時 |
| 6 | published_end_at | TIMESTAMPTZ | NULL | published_start_at より後 | 掲載終了日時 |
| 7 | is_active | BOOLEAN | NOT NULL | DEFAULT TRUE | 有効フラグ |
| 8 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 9 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**表示条件:**  
`is_active = TRUE` かつ `published_start_at <= 現在日時` かつ `published_end_at IS NULL OR published_end_at > 現在日時`
