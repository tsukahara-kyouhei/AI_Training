package jp.co.skig.officeorder.web.auth;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.logging.LogMaskingUtil;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Spring Security とアプリ独自会員モデルの橋渡しを行うサービス。
 *
 * <p>現在ログイン会員の取得、登録直後の疑似ログイン、セッション破棄を担当する。
 */
@Service
public class MemberSessionService {

    /** セッション操作ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MemberSessionService.class);

    /**
     * 現在ログイン中の会員を取得する。
     *
     * @param session 現在セッション
     * @return 会員セッション情報
     */
    public Optional<MemberSessionUser> currentMember(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MemberPrincipal memberPrincipal)) {
            return Optional.empty();
        }
        return Optional.of(new MemberSessionUser(
                memberPrincipal.getMemberId(),
                memberPrincipal.getUsername(),
                memberPrincipal.getLastName(),
                memberPrincipal.getFirstName()
        ));
    }

    /**
     * 会員登録直後などにアプリ側からログイン状態を作る。
     *
     * @param request 現在リクエスト
     * @param member ログインさせる会員
     */
    public void login(HttpServletRequest request, MemberSessionUser member) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        MemberPrincipal principal = new MemberPrincipal(
                member.memberId(),
                member.email(),
                member.lastName(),
                member.firstName(),
                ""
        );
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        log.info("event={} memberId={} sessionIdHash={}",
                LogEvent.SESSION_CREATED.value(),
                member.memberId(),
                LogMaskingUtil.hashSessionId(session.getId()));
    }

    /**
     * セッションとSecurityContextを破棄する。
     *
     * @param session 現在セッション
     */
    public void clear(HttpSession session) {
        SecurityContextHolder.clearContext();
        if (session == null) {
            return;
        }
        log.info("event={} sessionIdHash={}",
                LogEvent.SESSION_CLEARED.value(),
                LogMaskingUtil.hashSessionId(session.getId()));
        session.invalidate();
    }

    /**
     * ログイン後戻り先を安全な相対パスへ正規化する。
     *
     * @param rawRedirectPath 入力された戻り先
     * @return 利用可能な戻り先
     */
    public String sanitizeRedirectPath(String rawRedirectPath) {
        return AuthRedirectUtils.sanitizeRedirectPath(rawRedirectPath);
    }
}

