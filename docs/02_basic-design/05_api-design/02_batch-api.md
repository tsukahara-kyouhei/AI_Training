# バッチAPI定義

## 1. 概要

バッチ処理は内部管理用エンドポイントで公開する。  
アクセスは `ROLE_INTERNAL_API` を持つ認証済みユーザーのみ可能である。

| 属性 | 内容 |
|---|---|
| Base URL | `/internal` |
| 認証 | Spring Security セッション認証（`ROLE_INTERNAL_API` 必須） |
| 実装クラス | `InternalBatchController` |

---

## 2. エンドポイント一覧

### 2.1 GET `/internal/batch/jobs`

**機能:** 登録済みバッチジョブ一覧取得

**リクエスト:**

```http
GET /internal/batch/jobs
```

**レスポンス（HTML）:** バッチジョブ一覧画面返却

| バッチジョブ名 | 説明 |
|---|---|
| `rankingCalculationJob` | 売れ筋ランキング計算 |
| `recommendationCalculationJob` | おすすめ関連商品計算 |

---

### 2.2 GET `/internal/batch/jobs/{jobName}/executions`

**機能:** 指定ジョブの実行履歴一覧取得

**リクエスト:**

| パラメータ | 種別 | 型 | 必須 | 説明 |
|---|---|---|---|---|
| jobName | Path | String | 必須 | バッチジョブ名 |

```http
GET /internal/batch/jobs/rankingCalculationJob/executions
```

**レスポンス（HTML）:** 実行履歴一覧画面返却

---

### 2.3 POST `/internal/batch/jobs/{jobName}/executions`

**機能:** 指定バッチジョブの手動実行開始

**リクエスト:**

| パラメータ | 種別 | 型 | 必須 | 説明 |
|---|---|---|---|---|
| jobName | Path | String | 必須 | バッチジョブ名 |

```http
POST /internal/batch/jobs/rankingCalculationJob/executions
```

**レスポンス:**

| シナリオ | HTTPステータス | 内容 |
|---|---|---|
| 実行開始成功 | 302 Redirect | 実行履歴一覧画面へリダイレクト |
| ジョブ不存在 | 302 Redirect | エラーメッセージ付きリダイレクト |

---

### 2.4 POST `/internal/batch/executions/{executionId}/stop`

**機能:** 実行中のバッチジョブ停止

**リクエスト:**

| パラメータ | 種別 | 型 | 必須 | 説明 |
|---|---|---|---|---|
| executionId | Path | Long | 必須 | Spring Batch実行 ID |

```http
POST /internal/batch/executions/42/stop
```

**レスポンス:**

| シナリオ | HTTPステータス | 内容 |
|---|---|---|
| 停止リクエスト成功 | 302 Redirect | 実行履歴一覧へリダイレクト |
| 実行不存在 | 302 Redirect | エラーメッセージ付きリダイレクト |

---

## 3. バッチジョブ詳細

### 3.1 rankingCalculationJob

| 項目 | 内容 |
|---|---|
| ジョブ名 | `rankingCalculationJob` |
| Step | `productSalesAggregationStep` |
| 処理内容 | 過去1ヶ月の注文実績を集計し、`popular_product_rankings`テーブルを更新 |
| スケジュール | `0 0 * * * *`（毎時クロン） |
| ランク作成件数 | 上位10位 |

### 3.2 recommendationCalculationJob

| 項目 | 内容 |
|---|---|
| ジョブ名 | `recommendationCalculationJob` |
| Step | `relatedProductCalculationStep` |
| 処理内容 | 商品間の同時購入実績を元に類似度を計算し、`recommended_related_products`テーブルを更新 |
| スケジュール | `0 0 * * * *`（毎時クロン） |
| 関連商品件数 | 商品ごとに上位4件 |
