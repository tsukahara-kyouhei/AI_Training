# お知らせテーブル定義

## 1. announcements（お知らせ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | announcement_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | お知らせID |
| 2 | title | VARCHAR(255) | NOT NULL | - | タイトル |
| 3 | body | TEXT | NOT NULL | - | 本文 |
| 4 | publish_start_at | TIMESTAMPTZ | NOT NULL | - | 掲載開始日時 |
| 5 | publish_end_at | TIMESTAMPTZ | NULL | publish_start_at より後 | 掲載終了日時 |
| 6 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 7 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**表示条件:**  
`publish_start_at <= 現在日時` かつ `publish_end_at IS NULL OR publish_end_at > 現在日時`
