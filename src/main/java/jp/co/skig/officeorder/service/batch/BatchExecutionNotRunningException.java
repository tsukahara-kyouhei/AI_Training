package jp.co.skig.officeorder.service.batch;

/**
 * 停止対象が実行中でない場合に送出する例外。
 */
public class BatchExecutionNotRunningException extends RuntimeException {

    /**
     * 実行中ではないジョブ停止要求例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public BatchExecutionNotRunningException(String message) {
        super(message);
    }
}
