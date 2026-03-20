# 税率テーブル定義

## 1. tax_rates（消費税率マスタ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | tax_rate_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 税率ID |
| 2 | tax_rate_percent | NUMERIC(5,2) | NOT NULL | CHECK > 0 AND <= 100 | 税率（%表記。例: 10.00 = 10%） |
| 3 | effective_start_at | TIMESTAMPTZ | NOT NULL | - | 適用開始日時 |
| 4 | effective_end_at | TIMESTAMPTZ | NULL | effective_start_at より後 | 適用終了日時（NULL = 無期限） |
| 5 | is_active | BOOLEAN | NOT NULL | DEFAULT TRUE | 有効フラグ |
| 6 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 7 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**有効税率の取得条件:**  
`is_active = TRUE` かつ `effective_start_at <= 対象日時` かつ `effective_end_at IS NULL OR effective_end_at > 対象日時`  
`ORDER BY effective_start_at DESC LIMIT 1`

**利用タイミング:** 注文確定時・カート表示時に有効な税率を取得して、消費税額を計算・表示する。
