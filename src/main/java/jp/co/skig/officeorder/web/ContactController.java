package jp.co.skig.officeorder.web;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.service.contact.ContactService;
import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * お問い合わせ画面表示と送信を担当するController。
 */
@Controller
public class ContactController {

    /** お問い合わせ画面ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    /** お問い合わせユースケースサービス。 */
    private final ContactService contactService;
    /** ログイン会員取得サービス。 */
    private final MemberSessionService memberSessionService;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * お問い合わせControllerを生成する。
     *
     * @param contactService       お問い合わせサービス
     * @param memberSessionService セッション会員サービス
     * @param messageSource        利用者向けメッセージ取得元
     */
    public ContactController(ContactService contactService,
            MemberSessionService memberSessionService,
            MessageSource messageSource) {
        this.contactService = contactService;
        this.memberSessionService = memberSessionService;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * お問い合わせ画面を表示する。
     *
     * @param session 現在セッション
     * @param model   画面モデル
     * @return お問い合わせ画面
     */
    @GetMapping("/contact")
    public String contact(HttpSession session, Model model) {
        if (!model.containsAttribute("contactForm")) {
            Optional<MemberSessionUser> member = memberSessionService.currentMember(session);
            model.addAttribute("contactForm", contactService.createInitialForm(member));
        }
        return "pages/contact";
    }

    /**
     * お問い合わせを受け付ける。
     *
     * @param form               入力フォーム
     * @param bindingResult      バリデーション結果
     * @param session            現在セッション
     * @param redirectAttributes リダイレクト時メッセージ格納先
     * @param model              画面モデル
     * @return 遷移先
     */
    @PostMapping("/contact")
    public String submit(@Valid @ModelAttribute("contactForm") ContactForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            log.warn("event={} fieldErrorCount={}",
                    LogEvent.CONTACT_INPUT_INVALID.value(),
                    bindingResult.getFieldErrorCount());
            return "pages/contact";
        }
        Long memberId = memberSessionService.currentMember(session)
                .map(MemberSessionUser::memberId)
                .orElse(null);
        try {
            long inquiryId = contactService.submit(memberId, form);
            redirectAttributes.addFlashAttribute("contactSuccessMessage", message("flash.contact.accepted"));
            redirectAttributes.addFlashAttribute("contactInquiryNumber", formatInquiryNumber(inquiryId));
            return "redirect:/contact";
        } catch (IllegalStateException ex) {
            log.error("event={} memberId={} reason=save_failed",
                    LogEvent.CONTACT_SUBMIT_FAILED.value(),
                    memberId,
                    ex);
            model.addAttribute("contactError", ex.getMessage());
            return "pages/contact";
        }
    }

    /**
     * 問い合わせIDを画面表示用番号へ整形する。
     *
     * @param inquiryId 問い合わせID
     * @return 問い合わせ番号
     */
    private String formatInquiryNumber(long inquiryId) {
        return String.format("INQ%08d", inquiryId);
    }

    /**
     * 利用者向けメッセージを取得する。
     *
     * @param code メッセージコード
     * @param args 埋め込み引数
     * @return 解決済みメッセージ
     */
    private String message(String code, Object... args) {
        return messages.getMessage(code, args);
    }
}
