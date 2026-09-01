package jp.co.skig.officeorder.service.member;

/**
 * お気に入り登録上限を超えた場合の例外。
 */
public class FavoritesLimitExceededException extends RuntimeException {

    /**
     * お気に入り上限超過例外を生成する。
     *
     * @param message 利用者向けメッセージ
     */
    public FavoritesLimitExceededException(String message) {
        super(message);
    }
}
