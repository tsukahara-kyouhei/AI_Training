# バッチ処理一覧

## 1. 概要

Spring Batchを利用し、`BatchScheduler`（`@Component`）により毎時自動実行と手動実行の両方に対応。

---

## 2. バッチジョブ一覧

| ジョブ名 | ステップ | 処理内容 | スケジュール | 実行時間目安 |
|---|---|---|---|---|
| `rankingCalculationJob` | `productSalesAggregationStep` | 過去1ヶ月の販売集計と `popular_product_rankings` 更新 | 毎時0分 | 数分 |
| `recommendationCalculationJob` | `relatedProductCalculationStep` | 商品間の類似度計算と `recommended_related_products` 更新 | 毎時0分 | 数分 |

---

## 3. 実行方法

### 3.1 自動実行（スケジューラー）

`BatchScheduler` に `@Scheduled(cron = "0 0 * * * *")` を設定。  
毎正時に両ジョブを順次非同期実行。

### 3.2 手動実行（内部API）

```http
POST /internal/batch/jobs/{jobName}/executions
```

`InternalBatchController` が `BatchJobService.execute*()` を呼び出す。  
`BatchExecutionConfig` の `AsyncJobLauncher` 経由で非同期実行。

---

## 4. 関連テーブル

| テーブル | 用途 |
|---|---|
| `popular_product_rankings` | 売れ筋ランキング計算結果を更新 |
| `recommended_related_products` | 関連商品計算結果を更新 |
| Spring Batchインフラテーブル | 実行履歴管理 (`batch_job_instance`等) |

---

## 5. バッチ実行メタデータ

Spring Batchが自動管理する6テーブル:

| テーブル | 内容 |
|---|---|
| `batch_job_instance` | ジョブ定義 |
| `batch_job_execution` | ジョブ実行履歴 |
| `batch_job_execution_params` | ジョブ実行パラメータ |
| `batch_job_execution_context` | ジョブ実行コンテキスト |
| `batch_step_execution` | ステップ実行履歴 |
| `batch_step_execution_context` | ステップ実行コンテキスト |
