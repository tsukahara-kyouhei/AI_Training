package jp.co.skig.officeorder.service.member;

/**
 * 会員メールアドレスが重複した場合の例外。
 */
public class DuplicateEmailException extends RuntimeException {

    /**
     * メールアドレス重複例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public DuplicateEmailException(String message) {
        super(message);
    }
}

