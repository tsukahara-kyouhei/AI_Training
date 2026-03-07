package jp.co.skig.officeorder.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.service.member.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * マイページアクセス時に会員がまだ有効かを都度検証するInterceptor。
 */
public class MemberActiveValidationInterceptor implements HandlerInterceptor {

    /** 会員状態検証ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MemberActiveValidationInterceptor.class);

    /** 有効会員確認サービス。 */
    private final MemberService memberService;
    /** 会員セッション破棄サービス。 */
    private final MemberSessionService memberSessionService;

    /**
     * 会員有効性検証Interceptorを生成する。
     *
     * @param memberService 会員サービス
     * @param memberSessionService 会員セッションサービス
     */
    public MemberActiveValidationInterceptor(MemberService memberService,
                                             MemberSessionService memberSessionService) {
        this.memberService = memberService;
        this.memberSessionService = memberSessionService;
    }

    /**
     * 認証済み会員が退会済みなどで無効化されていないかを確認する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param handler ハンドラ
     * @return 処理継続可否
     * @throws Exception リダイレクト失敗時
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MemberPrincipal memberPrincipal)) {
            return true;
        }
        long memberId = memberPrincipal.getMemberId();
        if (memberService.findActiveById(memberId).isPresent()) {
            return true;
        }
        log.warn("event={} memberId={} reason=member_inactive_mypage",
                LogEvent.SESSION_MEMBER_INVALIDATED.value(),
                memberId);
        HttpSession session = request.getSession(false);
        memberSessionService.clear(session);
        response.sendRedirect(request.getContextPath() + "/login?expired=true");
        return false;
    }
}
