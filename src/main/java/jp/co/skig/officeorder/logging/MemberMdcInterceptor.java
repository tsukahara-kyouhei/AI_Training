package jp.co.skig.officeorder.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ログイン会員IDをMDCへ設定するInterceptor。
 */
public class MemberMdcInterceptor implements HandlerInterceptor {

    /**
     * リクエスト開始時に会員IDをMDCへ設定する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param handler ハンドラ
     * @return 処理継続可否
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            MDC.remove(LoggingMdcKeys.MEMBER_ID);
            return true;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MemberPrincipal memberPrincipal) {
            MDC.put(LoggingMdcKeys.MEMBER_ID, String.valueOf(memberPrincipal.getMemberId()));
        } else {
            MDC.remove(LoggingMdcKeys.MEMBER_ID);
        }
        return true;
    }

    /**
     * リクエスト完了時に会員IDをMDCから除去する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param handler ハンドラ
     * @param ex 例外
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(LoggingMdcKeys.MEMBER_ID);
    }
}
