package jp.co.skig.officeorder.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.order.OrderService;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.web.auth.LoginEmailCookieService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * カート画面と購入フロー前半を担当するController。
 *
 * <p>
 * カート表示・更新に加え、購入方法選択、注文情報入力、確認、完了画面までを扱う。
 */

@Controller
public class CartController {

    /** カート・購入フロー画面ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    /** 注文情報入力フォームのセッション保持キー。 */
    private static final String CHECKOUT_FORM_SESSION_KEY = "checkout_input_form";
    /** 注文確認画面のワンタイムトークン保持キー。 */
    private static final String CHECKOUT_CONFIRM_TOKEN_SESSION_KEY = "checkout_confirm_token";

    /** カート操作サービス。 */
    private final CartService cartService;
    /** 会員セッションサービス。 */
    private final MemberSessionService memberSessionService;
    /** ログイン画面のメールアドレス記憶Cookieサービス。 */
    private final LoginEmailCookieService loginEmailCookieService;
    /** 会員関連サービス。 */
    private final MemberService memberService;
    /** 注文サービス。 */
    private final OrderService orderService;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * カートControllerを生成する。
     *
     * @param cartService             カートサービス
     * @param memberSessionService    会員セッションサービス
     * @param loginEmailCookieService ログイン画面メールアドレス記憶Cookieサービス
     * @param memberService           会員サービス
     * @param orderService            注文サービス
     * @param messageSource           利用者向けメッセージ取得元
     */
    public CartController(CartService cartService,
            MemberSessionService memberSessionService,
            LoginEmailCookieService loginEmailCookieService,
            MemberService memberService,
            OrderService orderService,
            MessageSource messageSource) {
        this.cartService = cartService;
        this.memberSessionService = memberSessionService;
        this.loginEmailCookieService = loginEmailCookieService;
        this.memberService = memberService;
        this.orderService = orderService;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * カート画面を表示する。
     *
     * @param request  現在リクエスト
     * @param response 現在レスポンス
     * @param model    画面モデル
     * @return カート画面
     */
    @GetMapping("/cart")
    public String cart(HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        CartView cart = cartService.getCart(request, response);

        model.addAttribute("cart", cart);
        model.addAttribute("isCartEmpty", cart.isEmpty());

        return "pages/cart";
    }

    /**
     * 商品をカートへ追加する。
     *
     * @param productVariantId   商品バリアントID
     * @param quantity           追加数量
     * @param assemblyRequested  組立指定
     * @param redirect           追加後戻り先
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param redirectAttributes フラッシュ属性
     * @return リダイレクト先
     */
    @PostMapping("/cart/items")
    public String addItem(@RequestParam("productVariantId") long productVariantId,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            @RequestParam(name = "assemblyRequested", required = false) String[] assemblyRequestedValues,
            @RequestParam(name = "redirect", required = false) String redirect,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        String redirectPath = cartService.sanitizeRedirectPath(redirect);
        if (redirectPath == null) {
            redirectPath = "/cart";
        }
        try {
            Boolean assemblyRequested = resolveAssemblyRequested(assemblyRequestedValues);
            cartService.addItem(request, response, productVariantId, quantity, assemblyRequested);
            redirectAttributes.addFlashAttribute("cartMessage", message("flash.cart.added"));
        } catch (IllegalArgumentException ex) {
            log.warn("event={} productVariantId={} quantity={} reason={}",
                    LogEvent.CART_ADD_FAILED.value(),
                    productVariantId,
                    quantity,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("cartError", ex.getMessage());
        }
        return "redirect:" + redirectPath;
    }

    /**
     * カート明細の数量・組立指定を更新する。
     *
     * @param productVariantId   商品バリアントID
     * @param quantity           更新数量
     * @param assemblyRequested  組立指定
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param redirectAttributes フラッシュ属性
     * @return カート画面へのリダイレクト
     */
    @PostMapping("/cart/items/{productVariantId}/update")
    public String updateItem(@PathVariable long productVariantId,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            @RequestParam(name = "assemblyRequested", required = false) String[] assemblyRequestedValues,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        try {
            Boolean assemblyRequested = resolveAssemblyRequested(assemblyRequestedValues);
            cartService.updateItem(request, response, productVariantId, quantity, assemblyRequested);
        } catch (IllegalArgumentException ex) {
            log.warn("event={} productVariantId={} quantity={} reason={}",
                    LogEvent.CART_UPDATE_FAILED.value(),
                    productVariantId,
                    quantity,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("cartError", ex.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * クーポンを適用する。
     *
     * @param couponCode         クーポンコード
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param redirectAttributes フラッシュ属性
     * @return カート画面へのリダイレクト
     */

    /**
     * リクエストに複数の組立指定値が含まれていても、最後に送られた値を優先して解釈する。
     *
     * @param values 組立指定のリクエスト値一覧
     * @return 解釈済み組立指定。未指定時は {@code null}
     */
    private Boolean resolveAssemblyRequested(String[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (int i = values.length - 1; i >= 0; i--) {
            String value = values[i];
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                return Boolean.parseBoolean(trimmed);
            }
        }
        return null;
    }

    /**
     * カート明細を削除する。
     *
     * @param productVariantId 商品バリアントID
     * @param request          現在リクエスト
     * @param response         現在レスポンス
     * @return カート画面へのリダイレクト
     */
    @PostMapping("/cart/items/{productVariantId}/delete")
    public String deleteItem(@PathVariable long productVariantId,
            HttpServletRequest request,
            HttpServletResponse response) {
        cartService.removeItem(request, response, productVariantId);
        return "redirect:/cart";
    }

    /**
     * カートを空にする。
     *
     * @param request  現在リクエスト
     * @param response 現在レスポンス
     * @return カート画面へのリダイレクト
     */
    @PostMapping("/cart/clear")
    public String clearCart(HttpServletRequest request,
            HttpServletResponse response) {
        cartService.clear(request, response);
        return "redirect:/cart";
    }

    /**
     * クーポンを適用する。
     *
     * @param couponCode         入力されたクーポンコード
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param redirectAttributes フラッシュ属性
     * @return カート画面へのリダイレクト
     */
    @PostMapping("/cart/coupon/apply")
    public String applyCoupon(
            @RequestParam("couponCode") String couponCode,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        try {
            CartView cart = cartService.getCart(request, response);

            cartService.applyCoupon(
                    couponCode,
                    cart,
                    request,
                    response);

            redirectAttributes.addFlashAttribute(
                    "couponSuccess",
                    "クーポンを適用しました。");

        } catch (IllegalArgumentException ex) {
            log.warn(
                    "event=CART_COUPO_APPLY_FAILED reason={}",
                    ex.getMessage());

            redirectAttributes.addFlashAttribute(
                    "couponError",
                    ex.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * 適用中のクーポンを解除する。
     *
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param redirectAttributes フラッシュ属性
     * @return カート画面へのリダイレクト
     */
    @PostMapping("/cart/coupon/remove")
    public String removeCoupon(
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        Cookie cookie = new Cookie("APPLIED_COUPON", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);

        response.addCookie(cookie);

        redirectAttributes.addFlashAttribute(
                "couponSuccess",
                "クーポンを解除しました。");

        return "redirect:/cart";
    }

    /**
     * 購入方法選択画面を表示する。
     *
     * <p>
     * ログイン補助用に、記憶済みメールアドレスCookieがあれば左パネルのログインフォームへ反映する。
     *
     * @param request  現在リクエスト
     * @param response 現在レスポンス
     * @param model    画面モデル
     * @return 購入方法選択画面
     */
    @GetMapping("/checkout/method")
    public String checkoutMethod(HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        CartView cart = cartService.getCart(request, response);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        bindRememberedLoginEmail(request, model);
        model.addAttribute("checkoutRedirectPath", "/checkout/input");
        return "pages/checkout-method";
    }

    /**
     * 注文情報入力画面を表示する。
     *
     * @param request  現在リクエスト
     * @param response 現在レスポンス
     * @param session  現在セッション
     * @param model    画面モデル
     * @return 注文情報入力画面
     */
    @GetMapping("/checkout/input")
    public String checkoutInput(HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model) {
        CartView cart = cartService.getCart(request, response);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        if (!model.containsAttribute("checkoutForm")) {
            CheckoutInputForm sessionForm = (CheckoutInputForm) session.getAttribute(CHECKOUT_FORM_SESSION_KEY);
            if (sessionForm != null) {
                model.addAttribute("checkoutForm", sessionForm);
            } else {
                Optional<MemberSessionUser> member = memberSessionService.currentMember(session);
                model.addAttribute("checkoutForm", orderService.createInitialForm(member));
            }
        }
        model.addAttribute("cart", cart);
        bindCheckoutAddressOptions(session, model);
        return "pages/checkout-input";
    }

    /**
     * 注文情報入力を検証し、確認画面へ送る。
     *
     * @param checkoutForm       入力フォーム
     * @param bindingResult      バリデーション結果
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param session            現在セッション
     * @param model              画面モデル
     * @param redirectAttributes フラッシュ属性
     * @return 遷移先
     */
    @PostMapping("/checkout/input")
    public String checkoutInputSubmit(@Valid @ModelAttribute("checkoutForm") CheckoutInputForm checkoutForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        CartView cart = cartService.getCart(request, response);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        CheckoutInputForm normalized = checkoutForm.normalize();
        validateCheckoutFormBusinessRules(normalized, bindingResult);
        if (bindingResult.hasErrors()) {
            log.warn("event={} fieldErrorCount={}",
                    LogEvent.CHECKOUT_INPUT_INVALID.value(),
                    bindingResult.getFieldErrorCount());
            model.addAttribute("cart", cart);
            bindCheckoutAddressOptions(session, model);
            return "pages/checkout-input";
        }
        session.setAttribute(CHECKOUT_FORM_SESSION_KEY, normalized);
        session.setAttribute(CHECKOUT_CONFIRM_TOKEN_SESSION_KEY, UUID.randomUUID().toString());
        redirectAttributes.addFlashAttribute("checkoutForm", normalized);
        return "redirect:/checkout/confirm";
    }

    /**
     * 注文確認画面を表示する。
     *
     * @param request  現在リクエスト
     * @param response 現在レスポンス
     * @param session  現在セッション
     * @param model    画面モデル
     * @return 注文確認画面
     */
    @GetMapping("/checkout/confirm")
    public String checkoutConfirm(HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model) {
        CartView cart = cartService.getCart(request, response);
        if (cart.isEmpty()) {
            clearCheckoutSession(session);
            return "redirect:/cart";
        }
        CheckoutInputForm sessionForm = (CheckoutInputForm) session.getAttribute(CHECKOUT_FORM_SESSION_KEY);
        if (sessionForm == null) {
            return "redirect:/checkout/input";
        }
        String token = (String) session.getAttribute(CHECKOUT_CONFIRM_TOKEN_SESSION_KEY);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CHECKOUT_CONFIRM_TOKEN_SESSION_KEY, token);
        }
        model.addAttribute("checkoutForm", sessionForm);
        model.addAttribute("cart", cart);
        model.addAttribute("checkoutConfirmToken", token);
        return "pages/checkout-confirm";
    }

    /**
     * 注文確定処理を実行する。
     *
     * @param token              ワンタイムトークン
     * @param request            現在リクエスト
     * @param response           現在レスポンス
     * @param session            現在セッション
     * @param redirectAttributes フラッシュ属性
     * @return 遷移先
     */
    @PostMapping("/checkout/confirm")
    public String placeOrder(@RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        CartView cart = cartService.getCart(request, response);
        if (cart.isEmpty()) {
            clearCheckoutSession(session);
            return "redirect:/cart";
        }
        CheckoutInputForm sessionForm = (CheckoutInputForm) session.getAttribute(CHECKOUT_FORM_SESSION_KEY);
        if (sessionForm == null) {
            redirectAttributes.addFlashAttribute("checkoutError", message("flash.checkout.inputRetry"));
            return "redirect:/checkout/input";
        }
        String sessionToken = (String) session.getAttribute(CHECKOUT_CONFIRM_TOKEN_SESSION_KEY);
        if (sessionToken == null || !sessionToken.equals(token)) {
            log.warn("event={} reason=token_mismatch", LogEvent.CHECKOUT_TOKEN_MISMATCH.value());
            clearCheckoutSession(session);
            redirectAttributes.addFlashAttribute("checkoutError", message("flash.checkout.tokenMismatch"));
            return "redirect:/checkout/input";
        }
        try {
            Long memberId = memberSessionService.currentMember(session).map(MemberSessionUser::memberId).orElse(null);
            String orderNumber = orderService.placeOrder(memberId, sessionForm, cart);
            clearCheckoutSession(session);
            cartService.clear(request, response);
            log.info("event={} orderNumber={} memberId={}",
                    LogEvent.CHECKOUT_ORDER_ACCEPTED.value(),
                    orderNumber,
                    memberId);
            return "redirect:/checkout/complete/" + orderNumber;
        } catch (IllegalArgumentException ex) {
            log.warn("event={} reason={}", LogEvent.CHECKOUT_ORDER_REJECTED.value(), ex.getMessage());
            redirectAttributes.addFlashAttribute("checkoutError", ex.getMessage());
            return "redirect:/checkout/input";
        }
    }

    /**
     * 注文完了URL直アクセス時のフォールバック。
     *
     * @return 入力画面へのリダイレクト
     */
    @GetMapping("/checkout/complete")
    public String checkoutCompleteFallback() {
        return "redirect:/checkout/input";
    }

    /**
     * 注文完了画面を表示する。
     *
     * @param orderNumber 注文番号
     * @param model       画面モデル
     * @return 注文完了画面
     */
    @GetMapping("/checkout/complete/{orderNumber}")
    public String checkoutComplete(@PathVariable String orderNumber,
            Model model) {
        OrderCompleteView order = orderService.findOrderCompleteView(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("order", order);
        return "pages/checkout-complete";
    }

    /**
     * Bean Validationでは表現しづらい業務ルールを追加検証する。
     *
     * @param form          正規化済みフォーム
     * @param bindingResult 検証結果
     */
    private void validateCheckoutFormBusinessRules(CheckoutInputForm form, BindingResult bindingResult) {
        if (form.isCorporate() && (form.getCompanyName() == null || form.getCompanyName().isBlank())) {
            bindingResult.rejectValue("companyName", "validation.companyName.corporateRequired");
        }
    }

    /**
     * 購入フロー中のセッション保持情報を削除する。
     *
     * @param session 現在セッション
     */
    private void clearCheckoutSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(CHECKOUT_FORM_SESSION_KEY);
        session.removeAttribute(CHECKOUT_CONFIRM_TOKEN_SESSION_KEY);
    }

    /**
     * 会員注文時に選択可能な追加お届け先一覧をモデルへ積む。
     *
     * @param session 現在セッション
     * @param model   画面モデル
     */
    private void bindCheckoutAddressOptions(HttpSession session, Model model) {
        Optional<MemberSessionUser> member = memberSessionService.currentMember(session);
        model.addAttribute("isMemberCheckout", member.isPresent());
        if (member.isEmpty()) {
            model.addAttribute("checkoutAdditionalAddresses", List.of());
            model.addAttribute("hasCheckoutAdditionalAddresses", false);
            return;
        }
        List<MemberAdditionalAddressView> addresses = memberService
                .findAdditionalAddresses(member.get().memberId(), 1)
                .items();
        model.addAttribute("checkoutAdditionalAddresses", addresses);
        model.addAttribute("hasCheckoutAdditionalAddresses", !addresses.isEmpty());
    }

    /**
     * 購入方法選択画面のログインフォームへ記憶済みメールアドレスを反映する。
     *
     * @param request 現在リクエスト
     * @param model   画面モデル
     */
    private void bindRememberedLoginEmail(HttpServletRequest request, Model model) {
        Optional<String> rememberedEmail = loginEmailCookieService.findRememberedEmail(request);
        model.addAttribute("rememberedLoginEmail", rememberedEmail.orElse(""));
        model.addAttribute("rememberLoginEmail", rememberedEmail.isPresent());
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
