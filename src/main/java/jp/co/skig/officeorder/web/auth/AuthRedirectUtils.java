package jp.co.skig.officeorder.web.auth;

import org.springframework.util.StringUtils;

/**
 * ログイン後戻り先の安全性を検証するユーティリティ。
 */
public final class AuthRedirectUtils {

    private AuthRedirectUtils() {
    }

    /**
     * アプリ内相対パスのみをログイン後戻り先として許可する。
     *
     * @param rawRedirectPath 入力された戻り先
     * @return 利用可能な戻り先。無効な場合は {@code null}
     */
    public static String sanitizeRedirectPath(String rawRedirectPath) {
        if (!StringUtils.hasText(rawRedirectPath)) {
            return null;
        }
        String path = rawRedirectPath.trim();
        if (!path.startsWith("/")) {
            return null;
        }
        if (path.startsWith("//")) {
            return null;
        }
        if (path.contains("://")) {
            return null;
        }
        if (path.startsWith("/login") || path.startsWith("/logout")) {
            return null;
        }
        return path;
    }
}
