package jp.co.skig.officeorder.logging;

/**
 * ログMDCに保持するキー定義。
 */
public final class LoggingMdcKeys {

    /** リクエストID。 */
    public static final String REQUEST_ID = "requestId";
    /** ログイン会員ID。 */
    public static final String MEMBER_ID = "memberId";
    /** リクエストパス。 */
    public static final String PATH = "path";
    /** HTTPメソッド。 */
    public static final String METHOD = "method";

    private LoggingMdcKeys() {
    }
}
