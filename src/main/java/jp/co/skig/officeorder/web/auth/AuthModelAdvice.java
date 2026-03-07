package jp.co.skig.officeorder.web.auth;

import java.util.Optional;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全Controller共通で認証状態とヘッダ表示情報をモデルへ積む Advice。
 */
@ControllerAdvice(annotations = Controller.class)
public class AuthModelAdvice {

    /** 会員セッションサービス。 */
    private final MemberSessionService memberSessionService;
    /** カート件数取得サービス。 */
    private final CartService cartService;
    /** ヘッダお知らせ取得サービス。 */
    private final AnnouncementService announcementService;

    /**
     * 認証状態ModelAdviceを生成する。
     *
     * @param memberSessionService 会員セッションサービス
     * @param cartService カートサービス
     * @param announcementService お知らせサービス
     */
    public AuthModelAdvice(MemberSessionService memberSessionService,
                           CartService cartService,
                           AnnouncementService announcementService) {
        this.memberSessionService = memberSessionService;
        this.cartService = cartService;
        this.announcementService = announcementService;
    }

    /**
     * 全画面で使う認証状態・カート件数・ヘッダお知らせを設定する。
     *
     * @param model 画面モデル
     * @param request 現在リクエスト
     */
    @ModelAttribute
    public void bindAuthState(Model model, HttpServletRequest request) {
        if (isErrorRequest(request)) {
            return;
        }
        HttpSession session = request.getSession(false);
        Optional<MemberSessionUser> member = memberSessionService.currentMember(session);
        model.addAttribute("isLoggedIn", member.isPresent());
        member.ifPresent(m -> model.addAttribute("loginMember", m));
        model.addAttribute("cartItemCount", cartService.countTotalQuantity(request));
        model.addAttribute("headerAnnouncements", announcementService.findHeaderAnnouncements());
    }

    /**
     * エラーページ描画中か判定する。
     *
     * <p>エラー描画時は DB や Cookie に依存する共通ヘッダ情報の構築を避け、
     * システム障害時でも専用エラーページを安定して返せるようにする。
     *
     * @param request 現在リクエスト
     * @return エラー描画中なら {@code true}
     */
    private boolean isErrorRequest(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ERROR
                || request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) != null
                || "/error".equals(request.getRequestURI());
    }
}

