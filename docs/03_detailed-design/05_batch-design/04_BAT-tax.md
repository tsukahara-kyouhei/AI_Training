# 税率更新設計書

## 1. 概要

`tax_rates` テーブルで有効期間を管理する。消費税率の変更はアプリの再起動なしに `tax_rates` へのレコード追加だけで対応できる。

---

## 2. tax_rates テーブル構造

| カラム | 型 | 説明 |
|---|---|---|
| `tax_rate_id` | BIGSERIAL PK | 税率ID |
| `tax_rate_percent` | NUMERIC(5,2) | 税率（例: 10.00） |
| `effective_start_at` | TIMESTAMPTZ | 適用開始日時 |
| `effective_end_at` | TIMESTAMPTZ NULL | 適用終了日時（NULL=無期限） |
| `is_active` | BOOLEAN | 有効フラグ（DEFAULT TRUE） |
| `created_at` | TIMESTAMPTZ | 登録日時 |
| `updated_at` | TIMESTAMPTZ | 更新日時 |

---

## 3. 税率履歴管理方针

税率変更時は **新レコードをINSERT** するだけでよい。

```sql
-- 旧税率の終了日時を設定（必要に応じて）
UPDATE tax_rates
SET effective_end_at = '2031-09-30 23:59:59+09',
    updated_at = CURRENT_TIMESTAMP
WHERE tax_rate_id = 1;

-- 新税率を登録
INSERT INTO tax_rates (tax_rate_percent, effective_start_at, effective_end_at, is_active, created_at, updated_at)
VALUES (12.00, '2031-10-01 00:00:00+09', NULL, TRUE, NOW(), NOW());
```

---

## 4. 現在税率取得ロジック

`ProductMapper`, `CartMapper`, `OrderMapper` の `selectCurrentTaxRatePercent` メソッドで共通利用：

```sql
SELECT tax_rate_percent
FROM tax_rates
WHERE is_active = TRUE
  AND effective_start_at <= #{now}
  AND (effective_end_at IS NULL OR effective_end_at > #{now})
ORDER BY effective_start_at DESC
LIMIT 1
```

---

## 5. 初期データ

`sql/seed/masters.sql` に初期税率を含む：

```sql
INSERT INTO tax_rates (tax_rate_percent, effective_start_at, effective_end_at, is_active, created_at, updated_at)
VALUES (10.00, '2019-10-01 00:00:00+09', NULL, TRUE, NOW(), NOW());
```

---

## 6. バッチジョブの要否

税率はテーブルのレコード追加だけで対応できるため、**定期バッチジョブは不要**。税率変更時はデータベース操作のみで対応する設計。
