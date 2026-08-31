package jp.co.skig.officeorder.web;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import jp.co.skig.officeorder.service.member.DuplicateEmailException;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.web.auth.AuthSessionKeys;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 会員登録画面の入力・確認・完了を担当するController。
 */
@Controller
public class MemberRegistrationController {

    /** 会員登録画面ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MemberRegistrationController.class);

    /** 会員登録サービス。 */
    private final MemberService memberService;
    /** 会員セッション制御サービス。 */
    private final MemberSessionService memberSessionService;
    /** 会員登録完了メール送信サービス。 */
    private final NotificationMailService notificationMailService;

    /**
     * 会員登録Controllerを生成する。
     *
     * @param memberService           会員サービス
     * @param memberSessionService    会員セッションサービス
     * @param notificationMailService 通知メールサービス
     */
    public MemberRegistrationController(MemberService memberService,
            MemberSessionService memberSessionService,
            NotificationMailService notificationMailService) {
        this.memberService = memberService;
        this.memberSessionService = memberSessionService;
        this.notificationMailService = notificationMailService;
    }

    /**
     * 会員登録入力画面を表示する。
     *
     * @param model   画面モデル
     * @param session 現在セッション
     * @return 会員登録入力画面
     */
    @GetMapping("/members/register")
    public String showForm(Model model, HttpSession session) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", pendingForm(session).orElseGet(MemberRegisterForm::new));
        }
        return "pages/member-register";
    }

    /**
     * 会員登録入力内容を確認画面へ送る。
     *
     * @param rawForm       入力フォーム
     * @param bindingResult バリデーション結果
     * @param model         画面モデル
     * @param session       現在セッション
     * @return 遷移先
     */
    @PostMapping("/members/register/confirm")
    public String confirm(@Valid @ModelAttribute("registerForm") MemberRegisterForm rawForm,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {
        MemberRegisterForm form = rawForm.normalize();
        validateConditionalRules(form, bindingResult);
        if (!bindingResult.hasFieldErrors("email") && memberService.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "validation.email.duplicate");
        }
        if (bindingResult.hasErrors()) {
            log.warn("event={} fieldErrorCount={}",
                    LogEvent.MEMBER_REGISTER_INPUT_INVALID.value(),
                    bindingResult.getFieldErrorCount());
            return "pages/member-register";
        }
        session.setAttribute(AuthSessionKeys.PENDING_REGISTER_FORM, form);
        model.addAttribute("registerForm", form);
        return "pages/member-register-confirm";
    }

    /**
     * 会員登録を確定し、ログイン状態へ遷移させる。
     *
     * @param request            現在リクエスト
     * @param session            現在セッション
     * @param model              画面モデル
     * @param redirectAttributes リダイレクトメッセージ格納先
     * @return 遷移先
     */
    @PostMapping("/members/register")
    public String register(HttpServletRequest request,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<MemberRegisterForm> pending = pendingForm(session);
        if (pending.isEmpty()) {
            return "redirect:/members/register";
        }

        MemberRegisterForm form = pending.get();
        MemberSessionUser created;
        try {
            created = memberService.register(form);
        } catch (DuplicateEmailException ex) {
            log.warn("event={} reason=email_duplicate", LogEvent.MEMBER_REGISTER_REJECTED.value());
            BindingResult result = new BeanPropertyBindingResult(form, "registerForm");
            result.rejectValue("email", "validation.email.duplicate");
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registerForm", result);
            model.addAttribute("registerForm", form);
            return "pages/member-register";
        }

        session.removeAttribute(AuthSessionKeys.PENDING_REGISTER_FORM);
        memberSessionService.login(request, created);
        notificationMailService.sendMemberRegistrationCompleteMail(created);
        log.info("event={} memberId={}", LogEvent.MEMBER_REGISTER_END.value(), created.memberId());
        redirectAttributes.addFlashAttribute("registeredMemberCode", formatMemberCode(created.memberId()));
        return "redirect:/members/register/complete";
    }

    /**
     * 会員登録完了画面を表示する。
     *
     * @param model 画面モデル
     * @return 会員登録完了画面
     */
    @GetMapping("/members/register/complete")
    public String complete(Model model) {
        if (!model.containsAttribute("registeredMemberCode")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "pages/member-register-complete";
    }

    /**
     * 会員種別に応じた入力必須条件を追加で検証する。
     *
     * @param form          正規化済みフォーム
     * @param bindingResult 検証結果
     */
    private void validateConditionalRules(MemberRegisterForm form, BindingResult bindingResult) {
        if (form.isCorporate() && (form.getCompanyName() == null || form.getCompanyName().isBlank())) {
            bindingResult.rejectValue("companyName", "validation.companyName.corporateRequired");
        }
    }

    /**
     * セッションに保持した確認待ち会員登録フォームを取得する。
     *
     * @param session 現在セッション
     * @return 確認待ちフォーム
     */
    private Optional<MemberRegisterForm> pendingForm(HttpSession session) {
        Object form = session.getAttribute(AuthSessionKeys.PENDING_REGISTER_FORM);
        if (form instanceof MemberRegisterForm registerForm) {
            return Optional.of(registerForm);
        }
        return Optional.empty();
    }

    /**
     * 会員IDを画面表示用会員コードへ変換する。
     *
     * @param memberId 会員ID
     * @return 会員コード
     */
    private String formatMemberCode(long memberId) {
        return String.format("MEM%07d", memberId);
    }
}
