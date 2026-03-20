# 税率テーブル定義

## 1. tax_rates（消費税率マスタ）

| # | 列名 | 型 | NULL | 制約 | 日本語名 |
|---|---|---|---|---|---|
| 1 | tax_rate_id | BIGINT | NOT NULL | PK, GENERATED ALWAYS AS IDENTITY | 税率ID |
| 2 | rate | NUMERIC | NOT NULL | CHECK > 0 | 税率（小数。例: 0.10 = 10%） |
| 3 | effective_from | DATE | NOT NULL | - | 適用開始日 |
| 4 | effective_to | DATE | NULL | effective_from より後 | 適用終了日（NULL = 現在有効） |
| 5 | created_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| 6 | updated_at | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP | 更新日時 |

**有効税率の取得条件:**  
`effective_from <= 注文日` かつ `effective_to IS NULL OR effective_to > 注文日`  

**利用タイミング:** 注文確定時に有効な税率を取得して、消費税額を計算・注文レコードに保存する。
