## 1. 機能概要

- ローカル開発環境で、利用可能なバッチジョブと各ジョブの実行履歴を確認できるようにする。
- 開発者がバッチジョブを手動で起動し、実行状況を検証できるようにする。
- 実行中のバッチに停止を要求し、バッチ処理の動作確認や運用補助を行えるようにする。
- ジョブや実行状態に応じたエラー情報を返し、操作結果の把握を支援する。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | 実行プロファイル `local` | この内部 API は `local` プロファイルでのみ有効。 |
| 入力 | HTTP GET リクエスト `/internal/batch/jobs` | 対応するバッチジョブ一覧を要求する。 |
| 入力 | HTTP パス変数 `jobName` | 実行履歴の参照または起動対象となるジョブ名。 |
| 入力 | HTTP GET リクエスト `/internal/batch/jobs/{jobName}/executions` | 指定したジョブの実行履歴を要求する。 |
| 入力 | クエリパラメータ `limit` | 実行履歴の返却件数上限。省略時は `20`。 |
| 入力 | HTTP POST リクエスト `/internal/batch/jobs/{jobName}/executions` | 指定したバッチジョブの手動起動を要求する。 |
| 入力 | HTTP パス変数 `executionId` | 停止要求対象のバッチ実行 ID。 |
| 入力 | HTTP POST リクエスト `/internal/batch/executions/{executionId}/stop` | 指定した実行中ジョブの停止を要求する。 |
| 出力 | ジョブ一覧 JSON | 各ジョブの名前、実行中状態、最新実行の状態・ID・開始時刻・終了時刻を返す。 |
| 出力 | 実行履歴 JSON | 各実行の ID、ジョブ名、状態、終了コード、作成・開始・終了時刻、起動契機を返す。 |
| 出力 | 起動受付 JSON（HTTP 202） | 実行 ID、ジョブ名、受付時刻、受付メッセージを返す。 |
| 出力 | 停止受付 JSON（HTTP 202） | 実行 ID、停止要求時刻、受付メッセージを返す。 |
| 出力 | エラー JSON | `code` と `message` を返す。未知のジョブ／実行は 404、実行中・停止状態の競合は 409、起動・停止処理の予期しない失敗は 500。 |

## 3. 処理フロー

### `listJobs`（GET `/internal/batch/jobs`）

1. `BatchExecutionService` を呼び出してサポート対象のジョブ一覧を取得する。
2. ジョブ一覧を JSON で返す。

### `listExecutions`（GET `/internal/batch/jobs/{jobName}/executions`）

1. パスからジョブ名、クエリパラメータから取得件数上限 `limit` を受け取る（省略時は `20`）。
2. `BatchExecutionService` を呼び出して指定ジョブの実行履歴を取得する。
3. 取得に成功した場合、実行履歴を HTTP 200 の JSON で返す。
4. ジョブが存在しない場合：
   1. 要求拒否を警告ログに記録する。
   2. `BATCH_JOB_NOT_FOUND` と例外メッセージを含むエラー JSON を HTTP 404 で返す。

### `startJob`（POST `/internal/batch/jobs/{jobName}/executions`）

1. パスから起動対象のジョブ名を受け取る。
2. `BatchExecutionService` を呼び出してジョブの起動を要求する。
3. 起動要求が受け付けられた場合、受付情報を HTTP 202 の JSON で返す。
4. ジョブがすでに実行中の場合：
   1. 要求拒否を警告ログに記録する。
   2. `BATCH_ALREADY_RUNNING` と例外メッセージを含むエラー JSON を HTTP 409 で返す。
5. ジョブが存在しない場合：
   1. 要求拒否を警告ログに記録する。
   2. `BATCH_JOB_NOT_FOUND` と例外メッセージを含むエラー JSON を HTTP 404 で返す。
6. その他の実行時例外が発生した場合：
   1. 起動失敗をエラーログに記録する。
   2. `SERVER_ERROR` と起動失敗メッセージを含むエラー JSON を HTTP 500 で返す。

### `stopExecution`（POST `/internal/batch/executions/{executionId}/stop`）

1. パスから停止対象の実行 ID を受け取る。
2. `BatchExecutionService` を呼び出して対象実行の停止を要求する。
3. 停止要求が受け付けられた場合、受付情報を HTTP 202 の JSON で返す。
4. 実行 ID が見つからない場合：
   1. 要求拒否を警告ログに記録する。
   2. `BATCH_EXECUTION_NOT_FOUND` と例外メッセージを含むエラー JSON を HTTP 404 で返す。
5. 対象の実行が停止要求を受け付けられる状態でない場合：
   1. 要求拒否を警告ログに記録する。
   2. `BATCH_EXECUTION_NOT_RUNNING` と例外メッセージを含むエラー JSON を HTTP 409 で返す。
6. その他の実行時例外が発生した場合：
   1. 停止失敗をエラーログに記録する。
   2. `SERVER_ERROR` と停止失敗メッセージを含むエラー JSON を HTTP 500 で返す。
