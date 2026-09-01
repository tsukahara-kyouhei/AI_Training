package jp.co.skig.officeorder.web;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.member.LoginForm;
import jp.co.skig.officeorder.web.auth.LoginEmailCookieService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ログイン画面表示を担当するController。
 */
@Controller
public class AuthController {

    /** 認証画面ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** 現在会員判定とログイン後戻り先正規化を担当するサービス。 */
    private final MemberSessionService memberSessionService;

    /** ログイン画面のメールアドレス記憶Cookieサービス。 */
    private final LoginEmailCookieService loginEmailCookieService;

    /**
     * 認証Controllerを生成する。
     *
     * @param memberSessionService    セッション会員サービス
     * @param loginEmailCookieService ログイン画面メールアドレス記憶Cookieサービス
     */
    public AuthController(MemberSessionService memberSessionService,
            LoginEmailCookieService loginEmailCookieService) {
        this.memberSessionService = memberSessionService;
        this.loginEmailCookieService = loginEmailCookieService;
    }

    /**
     * ログイン画面を表示する。
     *
     * <p>
     * 既ログイン時はマイページへ戻し、未ログイン時は戻り先とセッション切れ表示、
     * 記憶済みメールアドレスCookieによる初期値をモデルへ反映する。
     *
     * @param redirectPath ログイン後戻り先
     * @param expired      セッション切れフラグ
     * @param session      現在セッション
     * @param model        画面モデル
     * @return 遷移先テンプレート
     */
    @GetMapping("/login")
    public String login(@RequestParam(name = "redirect", required = false) String redirectPath,
            @RequestParam(name = "expired", defaultValue = "false") boolean expired,
            HttpServletRequest request,
            HttpSession session,
            Model model) {
        if (memberSessionService.currentMember(session).isPresent()) {
            log.info("event={} reason=already_authenticated", LogEvent.AUTH_LOGIN_PAGE_REDIRECT.value());
            return "redirect:/mypage/orders";
        }

        Optional<String> rememberedEmail = loginEmailCookieService.findRememberedEmail(request);

        if (!model.containsAttribute("loginForm")) {
            LoginForm form = new LoginForm();
            form.setRedirectPath(memberSessionService.sanitizeRedirectPath(redirectPath));
            rememberedEmail.ifPresent(form::setEmail);
            model.addAttribute("loginForm", form);
        }

        model.addAttribute("rememberLoginEmail", rememberedEmail.isPresent());

        if (expired) {
            log.info("event={} reason=session_expired", LogEvent.AUTH_LOGIN_PAGE_REDIRECT.value());
        }

        model.addAttribute("sessionExpired", expired);

        return "pages/login";
    }
}
