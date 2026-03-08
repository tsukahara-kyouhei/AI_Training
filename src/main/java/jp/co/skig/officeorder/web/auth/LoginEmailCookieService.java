package jp.co.skig.officeorder.web.auth;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ログイン画面のメールアドレス入力補助Cookieを扱うサービス。
 *
 * <p>「次回から入力を省略する」が選択された場合にメールアドレスだけをCookieへ保存し、
 * 次回のログイン画面・購入方法選択画面へ初期表示する。
 */
@Service
public class LoginEmailCookieService {

    /** メールアドレス記憶用Cookie名。 */
    static final String COOKIE_NAME = "office_order_login_email";
    /** Cookie保持期間。 */
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    /**
     * Cookie から記憶済みメールアドレスを取得する。
     *
     * @param request 現在リクエスト
     * @return 記憶済みメールアドレス
     */
    public Optional<String> findRememberedEmail(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName()) || !StringUtils.hasText(cookie.getValue())) {
                continue;
            }
            return Optional.of(URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    /**
     * メールアドレスをCookieへ保存する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param email 保存対象メールアドレス
     */
    public void rememberEmail(HttpServletRequest request, HttpServletResponse response, String email) {
        Cookie cookie = new Cookie(COOKIE_NAME, URLEncoder.encode(email.trim(), StandardCharsets.UTF_8));
        cookie.setPath(resolveCookiePath(request));
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    /**
     * 記憶済みメールアドレスCookieを削除する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     */
    public void clearRememberedEmail(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath(resolveCookiePath(request));
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    /**
     * アプリの配下パスに合わせたCookieパスを返す。
     *
     * @param request 現在リクエスト
     * @return Cookieパス
     */
    private String resolveCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return StringUtils.hasText(contextPath) ? contextPath : "/";
    }
}
