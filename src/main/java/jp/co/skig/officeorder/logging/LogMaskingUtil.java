package jp.co.skig.officeorder.logging;

import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * ログ出力時の機密情報マスキングを行うユーティリティ。
 */
public final class LogMaskingUtil {

    private LogMaskingUtil() {
    }

    /**
     * メールアドレスを部分マスクする。
     *
     * @param rawEmail 元メールアドレス
     * @return マスク済みメールアドレス
     */
    public static String maskEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            return "unknown";
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0 || at == normalized.length() - 1) {
            return "***";
        }
        String localPart = normalized.substring(0, at);
        String domain = normalized.substring(at);
        if (localPart.length() == 1) {
            return "*" + domain;
        }
        return localPart.charAt(0) + "***" + domain;
    }

    /**
     * 電話番号を下4桁のみ残してマスクする。
     *
     * @param rawPhone 元電話番号
     * @return マスク済み電話番号
     */
    public static String maskPhone(String rawPhone) {
        if (!StringUtils.hasText(rawPhone)) {
            return "unknown";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        String suffix = digits.substring(digits.length() - 4);
        return "*".repeat(Math.max(0, digits.length() - 4)) + suffix;
    }

    /**
     * 郵便番号を先頭3桁以外マスクする。
     *
     * @param rawPostalCode 元郵便番号
     * @return マスク済み郵便番号
     */
    public static String maskPostalCode(String rawPostalCode) {
        if (!StringUtils.hasText(rawPostalCode)) {
            return "unknown";
        }
        String digits = rawPostalCode.replaceAll("[^0-9]", "");
        if (digits.length() < 3) {
            return "***";
        }
        return digits.substring(0, 3) + "*".repeat(Math.max(0, digits.length() - 3));
    }

    /**
     * セッションIDを直接出さずにハッシュ値へ変換する。
     *
     * @param sessionId セッションID
     * @return ハッシュ化済みセッション識別子
     */
    public static String hashSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return "unknown";
        }
        String normalized = sessionId.trim();
        int hash = normalized.hashCode();
        return String.format("%08x", hash);
    }
}
