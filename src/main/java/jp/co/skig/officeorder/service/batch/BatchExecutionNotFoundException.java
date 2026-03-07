package jp.co.skig.officeorder.service.batch;

/**
 * 指定された実行IDに対応するバッチ実行が存在しない場合の例外。
 */
public class BatchExecutionNotFoundException extends RuntimeException {

    /**
     * 実行ID未検出例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public BatchExecutionNotFoundException(String message) {
        super(message);
    }
}
