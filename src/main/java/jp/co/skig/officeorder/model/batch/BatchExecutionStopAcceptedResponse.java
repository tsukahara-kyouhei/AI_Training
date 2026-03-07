package jp.co.skig.officeorder.model.batch;

import java.time.OffsetDateTime;

/**
 * 実行中バッチの停止要求受付時に返す応答モデル。
 */
public record BatchExecutionStopAcceptedResponse(
        String executionId,
        OffsetDateTime requestedAt,
        String message
) {
}
