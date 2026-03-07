package jp.co.skig.officeorder.service.batch;

/**
 * サポート対象外のジョブ名が指定された場合の例外。
 */
public class UnknownBatchJobException extends RuntimeException {

    /**
     * 未知ジョブ名例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public UnknownBatchJobException(String message) {
        super(message);
    }
}
