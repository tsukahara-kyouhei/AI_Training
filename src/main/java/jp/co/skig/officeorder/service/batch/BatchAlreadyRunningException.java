package jp.co.skig.officeorder.service.batch;

/**
 * 同一ジョブの二重起動要求を拒否するときの例外。
 */
public class BatchAlreadyRunningException extends RuntimeException {

    /**
     * 実行中ジョブ例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public BatchAlreadyRunningException(String message) {
        super(message);
    }
}

