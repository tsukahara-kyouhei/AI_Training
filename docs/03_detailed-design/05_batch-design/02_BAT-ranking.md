# 人気ランキング計算バッチ設計

## 1. 概要

| 項目 | 内容 |
|---|---|
| ジョブ名 | `rankingCalculationJob` |
| ステップ名 | `productSalesAggregationStep` |
| 実装クラス | `BatchJobConfiguration`, `BatchJobService` |
| スケジュール | `0 0 * * * *`（毎時0分） |

---

## 2. 処理フロー

```
1. BatchRepository.selectPopularRankingCandidates(targetMonth)
   - 対象期間: 実行日の1ヶ月前から実行日まで
   - 進行条件: order_status NOT IN ('cancelled')（キャンセル以外全て）
   - 集計: product_idごとにSUM(quantity)
   - 順位付け: 販売数順 DESC LIMIT 10

2. popular_product_rankingsテーブルを漏終
   - ranking_date = todayのレコードをDELETE

3. 計算結果をpopular_product_rankingsにINSERT
   - ranking_date = today
   - rank = 1〜10
   - product_id, sold_quantity_1m

4. ジョブ完了ログ出力
```

---

## 3. 入力/出力テーブル

| 区分 | テーブル | 機能 |
|---|---|---|
| 入力 | `orders` | 注文ステータスで絞り込み |
| 入力 | `order_items` | 商品コード・数量集計 |
| 入力 | `products` | product_idの存在確認 |
| 出力 | `popular_product_rankings` | 集計結果を书き込み |

---

## 4. 実行パラメータ

| パラメータ | 型 | 説明 |
|---|---|---|
| `rankingDate` | LocalDate | 集計対象日（当日） |
