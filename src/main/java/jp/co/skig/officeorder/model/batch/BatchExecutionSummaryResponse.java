package jp.co.skig.officeorder.model.batch;

import java.time.OffsetDateTime;

/**
 * バッチ実行履歴の1件分を表す応答モデル。
 */
public record BatchExecutionSummaryResponse(
        String executionId,
        String jobName,
        String status,
        String exitCode,
        OffsetDateTime createTime,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String trigger
) {
}
