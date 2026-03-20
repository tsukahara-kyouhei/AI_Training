# レコメンド計算バッチ設計

## 1. 概要

| 項目 | 内容 |
|---|---|
| ジョブ名 | `recommendationCalculationJob` |
| ステップ名 | `relatedProductCalculationStep` |
| 実装クラス | `BatchJobConfiguration`, `BatchJobService` |
| スケジュール | `0 0 * * * *`（毎時0分） |

---

## 2. 処理フロー

```
1. BatchRepository.selectOrderProductOccurrences(targetMonth)
   - 対象期間: 過去3ヶ月の注文
   - 同一注文内で同時購入された商品ペアを集計
   - co-occurrence数を類似度スコアとして利用

2. recommended_related_productsテーブルを漏終
   - recommendation_date = todayのレコードをDELETE（当日分のみ）

3. 商品ごとに上位4件を選出しrecommended_related_productsにINSERT
   - recommendation_date = today
   - source_product_id, recommended_product_id
   - score = co-occurrence数
   - rank = 1〜4

4. ジョブ完了ログ出力
```

---

## 3. 入力/出力テーブル

| 区分 | テーブル | 機能 |
|---|---|---|
| 入力 | `orders` | 注文ステータスで絞り込み |
| 入力 | `order_items` | 商品ペアを集計 |
| 出力 | `recommended_related_products` | 関連商品上位4件を書き込み |

---

## 4. 表示件数

- 商品詳細画面の「この商品を見た人はこんな商品も購入しています」エリアに上位4件を表示
- データがない場合はカード領域自体を表示しない
