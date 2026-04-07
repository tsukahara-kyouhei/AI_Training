# 注文確定時システムエラー - テスト計画

> **ステータス**: ✅ テスト完了
> **前提**: 修正計画（03_fix-plan.md）に基づく実装が完了済みであること

---

## 1. テスト方針

以下の3区分でテストを実施する。

1. **修正確認テスト**：`sql/seed/test-data/orders.sql` の追加INSERT文が `order_number_counters` を正しく更新することを確認する
2. **デグレード確認テスト**：カウンターが既にシード値（100）より大きい場合に既存値が上書きされないことを確認する
3. **データ整合性確認**：`order_number` の重複がなく、`order_number_counters` の値が `orders` テーブルと整合していることを確認する

---

## 2. テスト環境

| 項目 | 内容 |
|---|---|
| テスト環境 | ローカル（Docker: office-order-db） |
| テストデータ | 既存シードデータを活用 |
| 実施者 | - |
| 実施期間 | 2026-04-07 |

---

## 3. テストケース

### 3.1 修正確認テスト（障害事象の解消確認）

| # | テストケース | 手順 | 期待結果 | 結果 | 実施日 | 備考 |
|---|---|---|---|---|---|---|
| TC-001 | シード実行後にカウンターが正しく設定される | `order_number_counters` から当日エントリを削除し、修正済みSQLの末尾INSERT文を再実行する | `order_date = CURRENT_DATE`, `last_sequence = 100` のレコードが存在する | ✅ OK | 2026-04-07 | last_sequence=100 を確認 |

**TC-001 実行SQL**
```sql
-- 準備: バグ再現状態を作る
DELETE FROM order_number_counters WHERE order_date = CURRENT_DATE;
-- 修正SQL適用
INSERT INTO order_number_counters (order_date, last_sequence)
VALUES (CURRENT_DATE, 100)
ON CONFLICT (order_date) DO UPDATE SET last_sequence = GREATEST(order_number_counters.last_sequence, 100);
-- 確認
SELECT * FROM order_number_counters WHERE order_date = CURRENT_DATE;
-- 結果: order_date=2026-04-07, last_sequence=100
```

### 3.2 デグレード確認テスト（修正による影響範囲の確認）

| # | テストケース | 手順 | 期待結果 | 結果 | 実施日 | 備考 |
|---|---|---|---|---|---|---|
| TC-002 | カウンターが 100 超のとき修正SQLを再実行しても既存値が保持される | `last_sequence = 250` に更新後、修正SQLの末尾INSERT文を実行する | `last_sequence` が 250 のまま変わらない | ✅ OK | 2026-04-07 | GREATEST(250, 100)=250 を確認 |

**TC-002 実行SQL**
```sql
-- 準備: カウンターを 250 に設定（アプリ経由で注文が多く発生しているシナリオ）
UPDATE order_number_counters SET last_sequence = 250 WHERE order_date = CURRENT_DATE;
-- 修正SQL適用
INSERT INTO order_number_counters (order_date, last_sequence)
VALUES (CURRENT_DATE, 100)
ON CONFLICT (order_date) DO UPDATE SET last_sequence = GREATEST(order_number_counters.last_sequence, 100);
-- 確認
SELECT last_sequence FROM order_number_counters WHERE order_date = CURRENT_DATE;
-- 結果: last_sequence=250（既存値が保護されている）
```

### 3.3 データ整合性確認

| # | 確認内容 | 確認SQL / 手順 | 期待結果 | 結果 | 実施日 |
|---|---|---|---|---|---|
| DC-001 | `order_number` の重複がないこと | `SELECT order_number, COUNT(*) FROM orders GROUP BY order_number HAVING COUNT(*) > 1;` | 0件（重複なし） | ✅ OK | 2026-04-07 |
| DC-002 | `order_number_counters` の `last_sequence` が当日注文件数以上であること | `last_sequence` と当日 `orders` 件数を比較 | `last_sequence(100) >= actual_order_count(4)` → OK | ✅ OK | 2026-04-07 |

**DC-002 実行SQL**
```sql
SELECT
  oc.order_date,
  oc.last_sequence AS counter_value,
  COUNT(o.order_id) AS actual_order_count,
  CASE WHEN oc.last_sequence >= COUNT(o.order_id) THEN 'OK' ELSE 'NG' END AS check_result
FROM order_number_counters oc
LEFT JOIN orders o
  ON o.order_number LIKE 'ORD' || TO_CHAR(oc.order_date, 'YYYYMMDD') || '%'
GROUP BY oc.order_date, oc.last_sequence;
-- 結果: order_date=2026-04-07, counter_value=100, actual_order_count=4, check_result=OK
```

---

## 4. テスト結果サマリ

| 区分 | 総件数 | OK | NG | 未実施 |
|---|---|---|---|---|
| 修正確認テスト | 1 | 1 | 0 | 0 |
| デグレード確認テスト | 1 | 1 | 0 | 0 |
| データ整合性確認 | 2 | 2 | 0 | 0 |
| **合計** | **4** | **4** | **0** | **0** |

---

## 5. 問題点・対応事項

なし

---

## 6. テスト完了判定

> **判定**: ✅ 合格
> **判定日**: 2026-04-07
> **判定者**: -

全テストケース（4件）がOKであり、NGチケットなし。

---

## 7. 変更履歴

| 日付 | 更新内容 | 更新者 |
|---|---|---|
| 2026-04-07 | 初版作成・テスト実施・完了判定 | - |
