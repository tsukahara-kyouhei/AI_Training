package jp.co.skig.officeorder.model.batch;

/**
 * バッチAPIの異常応答を表すモデル。
 */
public record BatchErrorResponse(
        String code,
        String message
) {
}

