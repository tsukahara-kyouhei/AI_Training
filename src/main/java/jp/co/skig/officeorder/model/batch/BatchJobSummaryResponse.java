package jp.co.skig.officeorder.model.batch;

import java.time.OffsetDateTime;

/**
 * ジョブ単位の最新実行状況を返す応答モデル。
 */
public record BatchJobSummaryResponse(
                String jobName,
                boolean running,
                String latestStatus,
                String latestExecutionId,
                OffsetDateTime latestStartTime,
                OffsetDateTime latestEndTime) {
}
