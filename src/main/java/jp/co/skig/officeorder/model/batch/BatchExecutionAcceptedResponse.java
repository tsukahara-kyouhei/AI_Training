package jp.co.skig.officeorder.model.batch;

import java.time.OffsetDateTime;

/**
 * バッチ実行受付時に返す応答モデル。
 */
public record BatchExecutionAcceptedResponse(
                String executionId,
                String jobName,
                OffsetDateTime acceptedAt,
                String message) {
}
