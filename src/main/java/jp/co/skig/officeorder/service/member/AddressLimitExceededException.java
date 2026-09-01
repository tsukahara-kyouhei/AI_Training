package jp.co.skig.officeorder.service.member;

/**
 * 追加お届け先の登録上限を超えた場合の例外。
 */
public class AddressLimitExceededException extends RuntimeException {

    /**
     * 追加お届け先上限超過例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public AddressLimitExceededException(String message) {
        super(message);
    }
}
