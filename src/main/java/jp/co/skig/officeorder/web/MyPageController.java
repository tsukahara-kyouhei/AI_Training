package jp.co.skig.officeorder.web;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.AddressLimitExceededException;
import jp.co.skig.officeorder.service.member.DuplicateEmailException;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.order.OrderService;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.member.MemberType;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
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

/**
 * マイページ配下の会員機能を担当するController。
 */
@Controller
public class MyPageController {

    /** マイページログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MyPageController.class);

    /** 会員サービス。 */
    private final MemberService memberService;
    /** 注文サービス。 */
    private final OrderService orderService;
    /** カートサービス。 */
    private final CartService cartService;
    /** 会員セッションサービス。 */
    private final MemberSessionService memberSessionService;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * マイページControllerを生成する。
     *
     * @param memberService 会員サービス
     * @param orderService 注文サービス
     * @param cartService カートサービス
     * @param memberSessionService 会員セッションサービス
     * @param messageSource 利用者向けメッセージ取得元
     */
    public MyPageController(MemberService memberService,
                            OrderService orderService,
                            CartService cartService,
                            MemberSessionService memberSessionService,
                            MessageSource messageSource) {
        this.memberService = memberService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.memberSessionService = memberSessionService;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * マイページトップアクセス時の既定遷移先を返す。
     */
    @GetMapping("/mypage")
    public String mypageTop() {
        return "redirect:/mypage/orders";
    }

    /**
     * 購入履歴一覧を表示する。
     */
    @GetMapping("/mypage/orders")
    public String orders(@RequestParam(name = "page", defaultValue = "1") int page,
                         HttpSession session,
                         Model model) {
        MemberSessionUser member = requireLoginMember(session);
        MemberOrderHistoryPage result = orderService.findMemberOrderHistories(member.memberId(), page);
        int correctedPage = result.page();
        int totalPages = result.totalPages();
        if (result.totalCount() > 0 && result.items().isEmpty() && correctedPage > totalPages) {
            result = orderService.findMemberOrderHistories(member.memberId(), totalPages);
            correctedPage = result.page();
            totalPages = result.totalPages();
        }
        model.addAttribute("orders", result.items());
        model.addAttribute("currentPage", correctedPage);
        model.addAttribute("totalPages", totalPages);
        return "pages/mypage-orders-list";
    }

    /**
     * 購入履歴詳細を表示する。
     */
    @GetMapping("/mypage/orders/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber,
                              HttpSession session,
                              Model model) {
        MemberSessionUser member = requireLoginMember(session);
        Optional<MemberOrderDetailView> orderDetail = orderService.findMemberOrderDetail(member.memberId(), orderNumber);
        if (orderDetail.isEmpty()) {
            log.warn("event={} memberId={} orderNumber={} reason=not_found",
                    LogEvent.MYPAGE_ORDER_DETAIL_MISSED.value(),
                    member.memberId(),
                    orderNumber);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("orderDetail", orderDetail.get());
        return "pages/mypage-order-detail";
    }

    /**
     * 購入履歴から再購入商品をカートへ追加する。
     */
    @PostMapping("/mypage/orders/{orderNumber}/reorder")
    public String reorder(@PathVariable String orderNumber,
                          HttpSession session,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          RedirectAttributes redirectAttributes) {
        MemberSessionUser member = requireLoginMember(session);
        if (orderService.findMemberOrderDetail(member.memberId(), orderNumber).isEmpty()) {
            log.warn("event={} memberId={} orderNumber={} reason=not_found",
                    LogEvent.MYPAGE_REORDER_REJECTED.value(),
                    member.memberId(),
                    orderNumber);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<OrderReorderItem> items = orderService.findReorderItems(member.memberId(), orderNumber);
        int addedCount = 0;
        List<String> failedProductCodes = new ArrayList<>();
        for (OrderReorderItem item : items) {
            if (item.productVariantId() == null || item.quantity() <= 0) {
                failedProductCodes.add(item.productCode() == null ? "-" : item.productCode());
                continue;
            }
            try {
                cartService.addItem(
                        request,
                        response,
                        item.productVariantId(),
                        item.quantity(),
                        item.assemblyRequested()
                );
                addedCount++;
            } catch (IllegalArgumentException ex) {
                failedProductCodes.add(item.productCode() == null ? "-" : item.productCode());
            }
        }
        if (addedCount == 0) {
            redirectAttributes.addFlashAttribute("cartError", message("flash.mypage.reorder.none"));
            return "redirect:/cart";
        }
        redirectAttributes.addFlashAttribute("cartMessage", message("flash.mypage.reorder.success"));
        if (!failedProductCodes.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "cartError",
                    message("flash.mypage.reorder.partialFailure", String.join(", ", failedProductCodes))
            );
        }
        return "redirect:/cart";
    }

    /**
     * お気に入り一覧を表示する。
     */
    @GetMapping("/mypage/favorites")
    public String favorites(@RequestParam(name = "page", defaultValue = "1") int page,
                            HttpSession session,
                            Model model) {
        MemberSessionUser member = requireLoginMember(session);
        MemberFavoritePage result = memberService.findFavorites(member.memberId(), page);
        int correctedPage = result.page();
        int totalPages = result.totalPages();
        if (result.totalCount() > 0 && result.items().isEmpty() && correctedPage > totalPages) {
            result = memberService.findFavorites(member.memberId(), totalPages);
            correctedPage = result.page();
            totalPages = result.totalPages();
        }

        model.addAttribute("favorites", result.items());
        model.addAttribute("favoriteCount", result.totalCount());
        model.addAttribute("favoriteLimit", MemberService.FAVORITES_LIMIT);
        model.addAttribute("currentPage", correctedPage);
        model.addAttribute("totalPages", totalPages);
        return "pages/mypage-favorites";
    }

    /**
     * 会員情報変更画面を表示する。
     */
    @GetMapping("/mypage/profile")
    public String profileEdit(HttpSession session, Model model) {
        MemberSessionUser member = requireLoginMember(session);
        if (!model.containsAttribute("profileForm")) {
            MemberProfileEditForm form = memberService.findProfileByMemberId(member.memberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("profileForm", form);
        }
        return "pages/mypage-profile-edit";
    }

    /**
     * 会員情報変更を受け付ける。
     */
    @PostMapping("/mypage/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") MemberProfileEditForm rawForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        MemberSessionUser member = requireLoginMember(session);
        MemberProfileEditForm form = rawForm.normalize();
        validateProfileFormForMemberType(form, bindingResult);
        if (bindingResult.hasErrors()) {
            log.warn("event={} memberId={} fieldErrorCount={} reason=input_invalid",
                    LogEvent.MEMBER_PROFILE_UPDATE_REJECTED.value(),
                    member.memberId(),
                    bindingResult.getFieldErrorCount());
            return "pages/mypage-profile-edit";
        }
        try {
            memberService.updateProfile(member.memberId(), form);
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "validation.email.duplicate");
            return "pages/mypage-profile-edit";
        }
        memberService.findActiveById(member.memberId())
                .ifPresent(updatedMember -> memberSessionService.login(request, updatedMember));
        redirectAttributes.addFlashAttribute("profileUpdatedMessage", message("flash.mypage.profile.updated"));
        return "redirect:/mypage/profile";
    }

    /**
     * 追加お届け先一覧を表示する。
     */
    @GetMapping("/mypage/addresses")
    public String addresses(@RequestParam(name = "page", defaultValue = "1") int page,
                            HttpSession session,
                            Model model) {
        MemberSessionUser member = requireLoginMember(session);
        MemberAdditionalAddressPage result = memberService.findAdditionalAddresses(member.memberId(), page);
        int correctedPage = result.page();
        int totalPages = result.totalPages();
        if (result.totalCount() > 0 && result.items().isEmpty() && correctedPage > totalPages) {
            result = memberService.findAdditionalAddresses(member.memberId(), totalPages);
            correctedPage = result.page();
            totalPages = result.totalPages();
        }

        boolean limitReached = result.totalCount() >= MemberService.ADDITIONAL_ADDRESS_LIMIT;
        model.addAttribute("addresses", result.items());
        model.addAttribute("addressCount", result.totalCount());
        model.addAttribute("addressLimit", MemberService.ADDITIONAL_ADDRESS_LIMIT);
        model.addAttribute("addressLimitReached", limitReached);
        model.addAttribute("currentPage", correctedPage);
        model.addAttribute("totalPages", totalPages);
        return "pages/mypage-addresses";
    }

    /**
     * 追加お届け先新規登録画面を表示する。
     */
    @GetMapping("/mypage/addresses/new")
    public String addressNewForm(HttpSession session, Model model) {
        MemberSessionUser member = requireLoginMember(session);
        if (memberService.isAddressLimitReached(member.memberId())) {
            log.warn("event={} memberId={} reason=limit_reached",
                    LogEvent.MEMBER_ADDRESS_CREATE_REJECTED.value(),
                    member.memberId());
            model.addAttribute("addressLimitError", message("business.member.addressLimitExceeded"));
        }
        if (!model.containsAttribute("addressForm")) {
            model.addAttribute("addressForm", new MemberAdditionalAddressForm());
        }
        applyAddressFormModel(model, "/mypage/addresses", false);
        return "pages/mypage-address-form";
    }

    /**
     * 追加お届け先編集画面を表示する。
     */
    @GetMapping("/mypage/addresses/{memberAddressId}/edit")
    public String addressEditForm(@PathVariable long memberAddressId,
                                  HttpSession session,
                                  Model model) {
        MemberSessionUser member = requireLoginMember(session);
        Optional<MemberAdditionalAddressView> address = memberService.findAdditionalAddressById(member.memberId(), memberAddressId);
        if (address.isEmpty()) {
            log.warn("event={} memberId={} memberAddressId={} reason=not_found",
                    LogEvent.MEMBER_ADDRESS_UPDATE_MISSED.value(),
                    member.memberId(),
                    memberAddressId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!model.containsAttribute("addressForm")) {
            model.addAttribute("addressForm", toForm(address.get()));
        }
        applyAddressFormModel(model, "/mypage/addresses/" + memberAddressId, true);
        return "pages/mypage-address-form";
    }

    /**
     * 追加お届け先を新規登録する。
     */
    @PostMapping("/mypage/addresses")
    public String createAddress(@Valid @ModelAttribute("addressForm") MemberAdditionalAddressForm rawForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        MemberSessionUser member = requireLoginMember(session);
        MemberAdditionalAddressForm normalizedForm = rawForm.normalize();
        validateAddressFormForMemberType(member.memberId(), normalizedForm, bindingResult);
        if (bindingResult.hasErrors()) {
            log.warn("event={} memberId={} fieldErrorCount={} reason=input_invalid",
                    LogEvent.MEMBER_ADDRESS_CREATE_REJECTED.value(),
                    member.memberId(),
                    bindingResult.getFieldErrorCount());
            applyAddressFormModel(model, "/mypage/addresses", false);
            return "pages/mypage-address-form";
        }
        try {
            memberService.createAdditionalAddress(member.memberId(), normalizedForm);
        } catch (AddressLimitExceededException ex) {
            log.warn("event={} memberId={} reason=limit_exceeded",
                    LogEvent.MEMBER_ADDRESS_CREATE_REJECTED.value(),
                    member.memberId());
            redirectAttributes.addFlashAttribute("addressLimitError", ex.getMessage());
            return "redirect:/mypage/addresses";
        }
        return "redirect:/mypage/addresses";
    }

    /**
     * 追加お届け先を更新する。
     */
    @PostMapping("/mypage/addresses/{memberAddressId}")
    public String updateAddress(@PathVariable long memberAddressId,
                                @Valid @ModelAttribute("addressForm") MemberAdditionalAddressForm rawForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model) {
        MemberSessionUser member = requireLoginMember(session);
        if (memberService.findAdditionalAddressById(member.memberId(), memberAddressId).isEmpty()) {
            log.warn("event={} memberId={} memberAddressId={} reason=not_found",
                    LogEvent.MEMBER_ADDRESS_UPDATE_MISSED.value(),
                    member.memberId(),
                    memberAddressId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        MemberAdditionalAddressForm normalizedForm = rawForm.normalize();
        validateAddressFormForMemberType(member.memberId(), normalizedForm, bindingResult);
        if (bindingResult.hasErrors()) {
            log.warn("event={} memberId={} memberAddressId={} fieldErrorCount={} reason=input_invalid",
                    LogEvent.MEMBER_ADDRESS_UPDATE_MISSED.value(),
                    member.memberId(),
                    memberAddressId,
                    bindingResult.getFieldErrorCount());
            applyAddressFormModel(model, "/mypage/addresses/" + memberAddressId, true);
            return "pages/mypage-address-form";
        }
        boolean updated = memberService.updateAdditionalAddress(member.memberId(), memberAddressId, normalizedForm);
        if (!updated) {
            log.warn("event={} memberId={} memberAddressId={} reason=not_found_on_update",
                    LogEvent.MEMBER_ADDRESS_UPDATE_MISSED.value(),
                    member.memberId(),
                    memberAddressId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/mypage/addresses";
    }

    /**
     * 追加お届け先を削除する。
     */
    @PostMapping("/mypage/addresses/{memberAddressId}/delete")
    public String deleteAddress(@PathVariable long memberAddressId,
                                HttpSession session) {
        MemberSessionUser member = requireLoginMember(session);
        memberService.deleteAdditionalAddress(member.memberId(), memberAddressId);
        return "redirect:/mypage/addresses";
    }

    /**
     * 退会確認画面を表示する。
     */
    @GetMapping("/mypage/withdraw")
    public String withdraw(HttpSession session) {
        requireLoginMember(session);
        return "pages/mypage-withdraw";
    }

    /**
     * 退会処理を実行し、セッションを破棄する。
     */
    @PostMapping("/mypage/withdraw")
    public String withdrawExecute(HttpSession session) {
        MemberSessionUser member = requireLoginMember(session);
        memberService.withdraw(member.memberId());
        memberSessionService.clear(session);
        return "redirect:/login";
    }

    /**
     * ログイン済み会員を取得し、未ログインなら401を返す。
     */
    private MemberSessionUser requireLoginMember(HttpSession session) {
        return memberSessionService.currentMember(session)
                .orElseThrow(() -> {
                    log.warn("event={} reason=unauthorized_session", LogEvent.AUTH_REQUIRED_REDIRECT.value());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                });
    }

    /**
     * 追加お届け先入力の法人必須条件を検証する。
     */
    private void validateAddressFormForMemberType(long memberId,
                                                  MemberAdditionalAddressForm form,
                                                  BindingResult bindingResult) {
        MemberType memberType = memberService.findMemberTypeById(memberId);
        if (memberType == MemberType.CORPORATE && (form.getCompanyName() == null || form.getCompanyName().isBlank())) {
            bindingResult.rejectValue("companyName", "validation.companyName.corporateRequired");
        }
    }

    /**
     * 会員情報変更入力の法人必須条件を検証する。
     */
    private void validateProfileFormForMemberType(MemberProfileEditForm form,
                                                  BindingResult bindingResult) {
        if (form.isCorporate() && (form.getCompanyName() == null || form.getCompanyName().isBlank())) {
            bindingResult.rejectValue("companyName", "validation.companyName.corporateRequired");
        }
    }

    /**
     * 追加お届け先入力画面共通のモデル属性を設定する。
     */
    private void applyAddressFormModel(Model model, String formAction, boolean editMode) {
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEditMode", editMode);
        model.addAttribute("cancelUrl", "/mypage/addresses");
    }

    /**
     * 追加お届け先表示モデルを入力フォームへ変換する。
     */
    private MemberAdditionalAddressForm toForm(MemberAdditionalAddressView view) {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName(view.lastName());
        form.setFirstName(view.firstName());
        form.setLastNameKana(view.lastNameKana());
        form.setFirstNameKana(view.firstNameKana());
        form.setCompanyName(view.companyName());
        form.setDepartmentName(view.departmentName());
        if (view.postalCode() != null && view.postalCode().length() == 7) {
            form.setPostalCodePart1(view.postalCode().substring(0, 3));
            form.setPostalCodePart2(view.postalCode().substring(3));
        }
        form.setPrefecture(view.prefecture());
        form.setCity(view.city());
        form.setAddressLine(view.addressLine());
        form.setDeliveryFloor(toFloorString(view.deliveryFloor()));
        form.setHasElevator(view.hasElevator());
        form.setDaytimePhone(view.daytimePhone());
        form.setFax(view.fax());
        return form;
    }

    /**
     * 階数を入力欄用文字列へ変換する。
     */
    private String toFloorString(Integer floor) {
        return floor == null ? null : String.valueOf(floor);
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
