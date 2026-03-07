package jp.co.skig.officeorder.web.auth;

/**
 * 認証・会員登録フローで使うセッションキー定義。
 */
public final class AuthSessionKeys {

    /** 会員登録確認待ちフォームの保持キー。 */
    public static final String PENDING_REGISTER_FORM = "PENDING_REGISTER_FORM";

    private AuthSessionKeys() {
    }
}
