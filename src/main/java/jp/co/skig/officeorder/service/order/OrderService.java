package jp.co.skig.officeorder.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponConditions;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.model.coupon.CouponValidationResult;
import jp.co.skig.officeorder.repository.CouponRepository;
import jp.co.skig.officeorder.service.coupon.CouponValidationException;
import jp.co.skig.officeorder.service.coupon.CouponValidationService;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import jp.co.skig.officeorder.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 注文確定と購入履歴参照を扱うサービス。
 *
 * <p>注文情報入力画面の初期化、注文確定時の受注保存、
 * 注文番号採番、注文完了メール送信予約、購入履歴取得をここに集約する。
 */
@Service
public class OrderService {

    /** 注文処理ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** 日次連番として許容する最大シーケンス値。 */
    private static final int ORDER_NUMBER_MAX_SEQUENCE = 999_999;
    /** 注文番号の日付部フォーマット。 */
    private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    /** 支払案内JSONの日付文字列フォーマット。 */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    /** 購入履歴一覧の1ページ件数。 */
    private static final int ORDER_HISTORY_PAGE_SIZE = 10;
    /** 税率が取得できない場合に適用するデフォルト税率(%)。 */
    private static final BigDecimal DEFAULT_TAX_RATE_PERCENT = BigDecimal.TEN;

    /** 受注データ永続化の窓口。 */
    private final OrderRepository orderRepository;
    /** クーポン利用実績の確定・検証窓口。 */
    private final CouponRepository couponRepository;
    /** クーポン適用可否判定サービス。 */
    private final CouponValidationService couponValidationService;
    /** 支払案内JSONのシリアライズに使う ObjectMapper。 */
    private final ObjectMapper objectMapper;
    /** 注文完了メール送信サービス。 */
    private final NotificationMailService notificationMailService;
    /** 注文日時と支払期限生成に使う Clock。 */
    private final Clock appClock;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * 注文サービスを生成する。
     *
     * @param orderRepository 受注リポジトリ
     * @param objectMapper JSONシリアライザ
     * @param notificationMailService 注文完了メール送信サービス
     * @param appClock 注文日時生成に使う Clock
     * @param messageSource 利用者向けメッセージ取得元
     */
    public OrderService(OrderRepository orderRepository,
                        CouponRepository couponRepository,
                        CouponValidationService couponValidationService,
                        ObjectMapper objectMapper,
                        NotificationMailService notificationMailService,
                        Clock appClock,
                        MessageSource messageSource) {
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.couponValidationService = couponValidationService;
        this.objectMapper = objectMapper;
        this.notificationMailService = notificationMailService;
        this.appClock = appClock;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 注文情報入力画面の初期フォームを生成する。
     *
     * <p>ログイン会員の場合は会員情報とデフォルトお届け先情報を初期値へ反映する。
     *
     * @param member ログイン会員
     * @return 初期フォーム
     */
    public CheckoutInputForm createInitialForm(Optional<MemberSessionUser> member) {
        CheckoutInputForm form = new CheckoutInputForm();
        if (member.isEmpty()) {
            return form;
        }
        Optional<CheckoutMemberPrefill> prefill = orderRepository.findCheckoutMemberPrefill(member.get().memberId());
        if (prefill.isEmpty()) {
            return form;
        }
        CheckoutMemberPrefill source = prefill.get();
        form.setPersonalOrCorporate(source.personalOrCorporate());
        form.setLastName(source.lastName());
        form.setFirstName(source.firstName());
        form.setLastNameKana(source.lastNameKana());
        form.setFirstNameKana(source.firstNameKana());
        form.setCompanyName(source.companyName());
        form.setDepartmentName(source.departmentName());
        form.setEmail(source.email());
        form.setDaytimePhone(source.daytimePhone());
        form.setFax(source.fax());
        if (source.postalCode() != null && source.postalCode().length() == 7) {
            form.setPostalCodePart1(source.postalCode().substring(0, 3));
            form.setPostalCodePart2(source.postalCode().substring(3));
        }
        form.setPrefecture(source.prefecture());
        form.setCity(source.city());
        form.setAddressLine(source.addressLine());
        form.setDeliveryFloor(toFloorString(source.deliveryFloor()));
        form.setHasElevator(source.hasElevator());
        return form;
    }

    /**
     * カート内容と入力フォームから注文を確定する。
     *
     * <p>入力値正規化、業務バリデーション、在庫再確認、注文番号採番、
     * 注文・注文明細・ステータス履歴保存、注文完了メール送信予約を順に行う。
     *
     * @param memberId ログイン会員ID。ゲスト時は {@code null}
     * @param rawForm 注文情報入力フォーム
     * @param cart 注文対象カート
     * @return 発行した注文番号
     */
    @Transactional
    public String placeOrder(Long memberId, CheckoutInputForm rawForm, CartView cart) {
        return placeOrder(memberId, rawForm, cart, null);
    }

    /**
     * 注文確定と入力フォームから注文を確定する（クーポン適用あり）。
     *
     * <p>クーポンコードが指定された場合は、トランザクション内で
     * FOR UPDATE ロック取得 → 最終再バリデーション → 使用回数確定
     * （全体・会員別のインクリメント）を行い、注文スナップショットに
     * クーポン情報を保存する。再検証NG時は例外をスローしてロールバックする。
     *
     * @param memberId ログイン会員ID。ゲスト時は {@code null}
     * @param rawForm 注文情報入力フォーム
     * @param cart 注文対象カート
     * @param appliedCouponCode 適用中のクーポンコード。未適用は {@code null}
     * @return 発行した注文番号
     */
    @Transactional
    public String placeOrder(Long memberId, CheckoutInputForm rawForm, CartView cart, String appliedCouponCode) {
        if (cart == null || cart.isEmpty()) {
            log.warn("event={} memberId={} reason=cart_empty",
                    LogEvent.ORDER_PLACE_REJECTED.value(),
                    memberId);
            throw new IllegalArgumentException(message("business.order.cartEmpty"));
        }
        CheckoutInputForm form = rawForm.normalize();
        log.info("event={} memberId={} customerType={} itemCount={} totalAmount={}",
                LogEvent.ORDER_PLACE_START.value(),
                memberId,
                memberId == null ? "guest" : "member",
                cart.items().size(),
                cart.summary().totalAmount());
        validateBusinessRules(form);
        revalidateCartStock(cart);
        Integer shippingFloor = parseFloor(form.getDeliveryFloor());

        // 金額再計算前に一度検証し、ロック取得後にも最新状態で再検証する。
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        String appliedCouponCodeSnapshot = null;
        String appliedCouponNameSnapshot = null;
        Long appliedCouponIdSnapshot = null;
        if (StringUtils.hasText(appliedCouponCode)) {
            Coupon coupon = couponRepository.findByCode(appliedCouponCode.trim())
                    .orElseThrow(() -> couponValidationException(CouponErrorCode.COUPON_NOT_FOUND));
            CouponValidationResult precheck = couponValidationService.validateCoupon(coupon, cart, memberId);
            if (!precheck.valid()) {
                throw couponValidationException(precheck.errorCode());
            }
            couponDiscountAmount = precheck.discountAmount();
        }

        String customerType = memberId == null ? "guest" : "member";
        String shippingMethod = cart.items().stream().anyMatch(CartLineView::assemblyRequested) ? "assembly" : "normal";
        BigDecimal taxRate = orderRepository.findCurrentTaxRatePercent();
        if (taxRate == null) {
            // 税率が取得できない場合は規定のデフォルト税率(10%)を適用する
            taxRate = DEFAULT_TAX_RATE_PERCENT;
        }
        String paymentInstructionJson = buildPaymentInstructionJson(form.getPaymentMethod());
        OffsetDateTime orderDatetime = OffsetDateTime.now(appClock);
        String orderNumber = generateOrderNumber(orderDatetime);
        List<OrderCompleteMailPayload.OrderItemLine> mailOrderItems = new ArrayList<>();

        // クーポン割引反映後の金額を再計算（税額は割引後の課税対象小計から算出、送料は割引前の値を維持）
        BigDecimal discountedProductSubtotal = cart.summary().productSubtotal()
                .subtract(couponDiscountAmount)
                .max(BigDecimal.ZERO);
        BigDecimal taxableSubtotal = discountedProductSubtotal.add(cart.summary().assemblyFeeTotal());
        BigDecimal orderTaxAmount = taxableSubtotal
                .multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        BigDecimal orderTotalAmount = taxableSubtotal.add(orderTaxAmount).add(cart.summary().shippingFee());

        CouponValidationResult confirmedCoupon = null;
        if (StringUtils.hasText(appliedCouponCode)) {
            confirmedCoupon = lockAndValidateCouponUsage(memberId, appliedCouponCode.trim(), cart);
            couponDiscountAmount = confirmedCoupon.discountAmount();
            appliedCouponCodeSnapshot = confirmedCoupon.coupon().code();
            appliedCouponNameSnapshot = confirmedCoupon.coupon().name();
            appliedCouponIdSnapshot = confirmedCoupon.coupon().id();

            discountedProductSubtotal = cart.summary().productSubtotal()
                    .subtract(couponDiscountAmount)
                    .max(BigDecimal.ZERO);
            taxableSubtotal = discountedProductSubtotal.add(cart.summary().assemblyFeeTotal());
            orderTaxAmount = taxableSubtotal
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            orderTotalAmount = taxableSubtotal.add(orderTaxAmount).add(cart.summary().shippingFee());
        }

        Map<String, Object> orderParams = new LinkedHashMap<>();
        orderParams.put("orderNumber", orderNumber);
        orderParams.put("orderDatetime", orderDatetime);
        orderParams.put("customerType", customerType);
        orderParams.put("memberId", memberId);
        orderParams.put("personalOrCorporate", form.getPersonalOrCorporate());
        orderParams.put("customerLastName", form.getLastName());
        orderParams.put("customerFirstName", form.getFirstName());
        orderParams.put("customerLastNameKana", form.getLastNameKana());
        orderParams.put("customerFirstNameKana", form.getFirstNameKana());
        orderParams.put("companyName", form.getCompanyName());
        orderParams.put("departmentName", form.getDepartmentName());
        orderParams.put("customerEmail", form.getEmail());
        orderParams.put("daytimePhone", form.getDaytimePhone());
        orderParams.put("shippingFax", form.getFax());
        orderParams.put("shippingPostalCode", form.getPostalCode());
        orderParams.put("shippingPrefecture", form.getPrefecture());
        orderParams.put("shippingCity", form.getCity());
        orderParams.put("shippingAddressLine", form.getAddressLine());
        orderParams.put("shippingFloor", shippingFloor);
        orderParams.put("shippingHasElevator", form.getHasElevator());
        orderParams.put("paymentMethod", form.getPaymentMethod());
        orderParams.put("paymentInstructionJson", paymentInstructionJson);
        orderParams.put("shippingMethod", shippingMethod);
        orderParams.put("shippingFee", cart.summary().shippingFee());
        orderParams.put("assemblyFeeTotal", cart.summary().assemblyFeeTotal());
        orderParams.put("subtotalAmount", cart.summary().productSubtotal());
        orderParams.put("taxAmount", orderTaxAmount);
        orderParams.put("totalAmount", orderTotalAmount);
        orderParams.put("appliedCouponCode", appliedCouponCodeSnapshot);
        orderParams.put("appliedCouponName", appliedCouponNameSnapshot);
        orderParams.put("couponDiscountAmount", couponDiscountAmount);
        orderParams.put("appliedCouponId", appliedCouponIdSnapshot);

        long orderId = orderRepository.insertOrder(orderParams);
        List<BigDecimal> lineCouponDiscounts = confirmedCoupon == null
                ? cart.items().stream().map(line -> BigDecimal.ZERO).toList()
                : allocateCouponDiscount(cart, confirmedCoupon.coupon(), couponDiscountAmount);
        for (int i = 0; i < cart.items().size(); i++) {
            CartLineView line = cart.items().get(i);
            BigDecimal lineSubtotal = line.lineSubtotalBeforeTax();
            BigDecimal lineCouponDiscountAmount = lineCouponDiscounts.get(i);
            BigDecimal lineTaxableSubtotal = lineSubtotal.subtract(lineCouponDiscountAmount);
            BigDecimal lineTaxAmount = lineTaxableSubtotal
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            BigDecimal lineTotal = lineTaxableSubtotal.add(lineTaxAmount);
            BigDecimal assemblyFee = (line.assemblyAvailable() && line.assemblyRequested())
                    ? line.assemblyFeePerUnit()
                    : BigDecimal.ZERO;

            Map<String, Object> itemParams = new LinkedHashMap<>();
            itemParams.put("orderId", orderId);
            itemParams.put("productCode", line.productCode());
            itemParams.put("productName", line.productName());
            itemParams.put("colorName", line.colorName());
            itemParams.put("unitPrice", line.unitPrice());
            itemParams.put("quantity", line.quantity());
            itemParams.put("lineSubtotal", lineSubtotal);
            itemParams.put("lineCouponDiscountAmount", lineCouponDiscountAmount);
            itemParams.put("lineTaxAmount", lineTaxAmount);
            itemParams.put("lineTotalAmount", lineTotal);
            itemParams.put("assemblyAvailable", line.assemblyAvailable());
            itemParams.put("assemblyFee", assemblyFee);
            orderRepository.insertOrderItem(itemParams);
            mailOrderItems.add(new OrderCompleteMailPayload.OrderItemLine(
                    line.productName(),
                    line.colorName(),
                    line.quantity(),
                    line.unitPrice(),
                    lineSubtotal
            ));
        }

        Map<String, Object> statusParams = new LinkedHashMap<>();
        statusParams.put("orderId", orderId);
        statusParams.put("status", "received");
        statusParams.put("changedBySystem", "office_order_web");
        orderRepository.insertOrderStatusHistory(statusParams);

        if (confirmedCoupon != null) {
            confirmCouponUsage(memberId, confirmedCoupon.coupon().id());
        }

        OrderCompleteMailPayload mailPayload = new OrderCompleteMailPayload(
                orderNumber,
                orderDatetime,
                form.getLastName(),
                form.getFirstName(),
                form.getEmail(),
                form.getPostalCode(),
                form.getPrefecture(),
                form.getCity(),
                form.getAddressLine(),
                shippingFloor,
                form.getHasElevator(),
                form.getDaytimePhone(),
                cart.summary().productSubtotal(),
                cart.summary().assemblyFeeTotal(),
                cart.summary().shippingFee(),
                orderTaxAmount,
                orderTotalAmount,
                List.copyOf(mailOrderItems)
        );
        sendOrderCompleteMailAfterCommit(mailPayload, orderNumber, memberId);
        log.info("event={} memberId={} orderId={} orderNumber={} itemCount={}",
                LogEvent.ORDER_PLACE_END.value(),
                memberId,
                orderId,
                orderNumber,
                cart.items().size());
        return orderNumber;
    }

    private List<BigDecimal> allocateCouponDiscount(CartView cart, Coupon coupon, BigDecimal discountAmount) {
        List<BigDecimal> allocations = new ArrayList<>(
                cart.items().stream().map(line -> BigDecimal.ZERO).toList());
        BigDecimal normalizedDiscount = discountAmount.setScale(0, RoundingMode.UNNECESSARY);
        if (normalizedDiscount.signum() == 0) {
            return List.copyOf(allocations);
        }

        CouponConditions conditions = couponValidationService.parseTargetConditions(coupon.targetConditions());
        List<Integer> targetIndexes = new ArrayList<>();
        BigDecimal targetSubtotal = BigDecimal.ZERO;
        for (int i = 0; i < cart.items().size(); i++) {
            CartLineView line = cart.items().get(i);
            if (conditions.allowedCategoryIds().contains(line.categoryId())
                    || conditions.allowedProductIds().contains(line.productId())) {
                targetIndexes.add(i);
                targetSubtotal = targetSubtotal.add(line.lineProductSubtotal());
            }
        }
        if (targetSubtotal.signum() <= 0 || normalizedDiscount.compareTo(targetSubtotal) > 0) {
            throw new IllegalStateException("Coupon discount must not exceed the targeted product subtotal");
        }

        List<DiscountRemainder> remainders = new ArrayList<>();
        BigDecimal allocatedSubtotal = BigDecimal.ZERO;
        for (int index : targetIndexes) {
            BigDecimal numerator = cart.items().get(index).lineProductSubtotal().multiply(normalizedDiscount);
            BigDecimal[] quotientAndRemainder = numerator.divideAndRemainder(targetSubtotal);
            allocations.set(index, quotientAndRemainder[0]);
            allocatedSubtotal = allocatedSubtotal.add(quotientAndRemainder[0]);
            remainders.add(new DiscountRemainder(index, quotientAndRemainder[1]));
        }

        int remainingYen = normalizedDiscount.subtract(allocatedSubtotal).intValueExact();
        remainders.sort(Comparator.comparing(DiscountRemainder::remainder).reversed()
                .thenComparingInt(DiscountRemainder::lineIndex));
        if (remainingYen > remainders.size()) {
            throw new IllegalStateException("Coupon discount allocation remainder exceeds target line count");
        }
        for (int i = 0; i < remainingYen; i++) {
            int index = remainders.get(i).lineIndex();
            allocations.set(index, allocations.get(index).add(BigDecimal.ONE));
        }
        return List.copyOf(allocations);
    }

    private record DiscountRemainder(int lineIndex, BigDecimal remainder) {
    }

    /**
     * 注文完了メールをコミット後に送るよう登録する。
     *
     * <p>DB保存が失敗した注文に対してメールだけ送ることを避けるため、
     * トランザクション成功後のフックで送信する。
     *
     * @param payload 注文完了メール情報
     * @param orderNumber 注文番号
     * @param memberId 会員ID
     */
    private void sendOrderCompleteMailAfterCommit(OrderCompleteMailPayload payload,
                                                  String orderNumber,
                                                  Long memberId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.error("event={} orderNumber={} memberId={} reason=transaction_synchronization_not_active",
                    LogEvent.ORDER_PLACE_REJECTED.value(),
                    orderNumber,
                    memberId);
            throw new IllegalStateException("注文完了メール送信のトランザクション同期に失敗しました。");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationMailService.sendOrderCompleteMail(payload);
            }
        });
    }

    /**
     * 注文完了画面表示用の注文情報を取得する。
     *
     * @param orderNumber 注文番号
     * @return 注文完了画面情報
     */
    public Optional<OrderCompleteView> findOrderCompleteView(String orderNumber) {
        if (!StringUtils.hasText(orderNumber)) {
            return Optional.empty();
        }
        return orderRepository.findOrderCompleteByOrderNumber(orderNumber.trim());
    }

    /**
     * 会員の購入履歴一覧を取得する。
     *
     * @param memberId 会員ID
     * @param page ページ番号
     * @return 購入履歴ページ
     */
    public MemberOrderHistoryPage findMemberOrderHistories(long memberId, int page) {
        int normalizedPage = Math.max(page, 1);
        return orderRepository.findMemberOrders(memberId, normalizedPage, ORDER_HISTORY_PAGE_SIZE);
    }

    /**
     * 会員の注文詳細を取得する。
     *
     * @param memberId 会員ID
     * @param orderNumber 注文番号
     * @return 注文詳細
     */
    public Optional<MemberOrderDetailView> findMemberOrderDetail(long memberId, String orderNumber) {
        if (!StringUtils.hasText(orderNumber)) {
            return Optional.empty();
        }
        return orderRepository.findMemberOrderDetail(memberId, orderNumber.trim());
    }

    /**
     * 再購入用に注文内商品を取得する。
     *
     * @param memberId 会員ID
     * @param orderNumber 注文番号
     * @return 再購入対象商品一覧
     */
    public List<OrderReorderItem> findReorderItems(long memberId, String orderNumber) {
        if (!StringUtils.hasText(orderNumber)) {
            return List.of();
        }
        return orderRepository.findReorderItems(memberId, orderNumber.trim());
    }

    /**
     * 当日日次連番で注文番号を採番する。
     *
     * @param orderDatetime 注文日時
     * @return 注文番号
     */
    private String generateOrderNumber(OffsetDateTime orderDatetime) {
        LocalDate orderDate = orderDatetime.toLocalDate();
        int sequence = orderRepository.nextOrderSequence(orderDate);
        if (sequence > ORDER_NUMBER_MAX_SEQUENCE) {
            throw new IllegalStateException("注文番号の日次連番が上限を超過しました。");
        }
        String datePart = orderDate.format(ORDER_NUMBER_DATE);
        return "ORD" + datePart + "-" + String.format("%06d", sequence);
    }

    /**
     * 支払方法に応じた支払案内JSONを生成する。
     *
     * <p>現時点ではコンビニ決済のみ案内情報を保持し、
     * 他決済では {@code null} を返す。
     *
     * @param paymentMethod 支払方法
     * @return 支払案内JSON
     */
    private String buildPaymentInstructionJson(String paymentMethod) {
        if (!"convenience_store".equals(paymentMethod)) {
            return null;
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("payment_number", String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000)));
        payload.put("payment_due_date", LocalDate.now(appClock).plusDays(3).format(DATE_FORMAT));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("event={} paymentMethod={} reason=serialize_failed",
                    LogEvent.ORDER_PLACE_REJECTED.value(),
                    paymentMethod,
                    ex);
            throw new IllegalStateException("コンビニ決済情報の生成に失敗しました。", ex);
        }
    }

    /**
     * 注文入力に対する業務バリデーションを行う。
     *
     * @param form 正規化済み注文フォーム
     */
        private CouponValidationResult lockAndValidateCouponUsage(Long memberId,
                                                                   String couponCode,
                                                                   CartView cart) {
            Coupon coupon = couponRepository.findByCodeForUpdate(couponCode)
                    .orElseThrow(() -> couponValidationException(CouponErrorCode.COUPON_NOT_FOUND));
            if (memberId != null) {
                couponRepository.ensureUserUsage(memberId, coupon.id());
            }
        CouponValidationResult result = couponValidationService.validateCoupon(coupon, cart, memberId);
        if (!result.valid()) {
            log.warn("event={} couponCode={} couponId={} memberId={} reason={}",
                    LogEvent.CHECKOUT_ORDER_REJECTED.value(),
                    couponCode,
                    coupon.id(),
                    memberId,
                    result.errorCode().name());
            throw couponValidationException(result.errorCode());
        }
        return result;
    }

    private void confirmCouponUsage(Long memberId, long couponId) {
        if (!couponRepository.incrementTotalUsage(couponId)) {
            throw couponValidationException(CouponErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }
        if (memberId != null) {
            couponRepository.incrementUserUsage(memberId, couponId);
        }
        log.info("event={} couponId={} memberId={}",
                LogEvent.COUPON_USAGE_CONFIRMED.value(),
                couponId,
                memberId);
    }

    private CouponValidationException couponValidationException(CouponErrorCode errorCode) {
        return new CouponValidationException(errorCode, couponValidationService.message(errorCode));
    }

    /**
     * 注文入力に対する業務バリデーションを行う。
     *
     * @param form 正規化済み注文フォーム
     */
    private void validateBusinessRules(CheckoutInputForm form) {
        if (form.isCorporate() && !StringUtils.hasText(form.getCompanyName())) {
            log.warn("event={} reason=company_name_required_for_corporate",
                    LogEvent.ORDER_PLACE_REJECTED.value());
            throw new IllegalArgumentException(message("business.order.companyRequired"));
        }
    }

    /**
     * 注文確定直前にカート内商品の在庫を再確認する。
     *
     * @param cart 注文対象カート
     */
    private void revalidateCartStock(CartView cart) {
        for (CartLineView line : cart.items()) {
            if (line.stockQuantity() <= 0 || line.quantity() > line.stockQuantity()) {
                log.warn("event={} productCode={} quantity={} stock={} reason=stock_shortage",
                        LogEvent.ORDER_PLACE_REJECTED.value(),
                        line.productCode(),
                        line.quantity(),
                        line.stockQuantity());
                throw new IllegalArgumentException(message("business.stockShortage"));
            }
        }
    }

    /**
     * 画面入力された階数文字列を整数へ変換する。
     *
     * @param rawFloor 階数文字列
     * @return 階数
     */
    private Integer parseFloor(String rawFloor) {
        if (!StringUtils.hasText(rawFloor)) {
            throw new IllegalArgumentException(message("business.order.deliveryFloorRequired"));
        }
        try {
            return Integer.valueOf(rawFloor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message("business.order.deliveryFloorInteger"), ex);
        }
    }

    /**
     * DBから取得した階数を入力欄用文字列へ変換する。
     *
     * @param floor 階数
     * @return 画面入力用文字列
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
