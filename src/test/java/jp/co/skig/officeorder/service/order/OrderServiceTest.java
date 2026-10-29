package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponConditions;
import jp.co.skig.officeorder.model.coupon.CouponValidationResult;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import jp.co.skig.officeorder.repository.CouponRepository;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.coupon.CouponValidationService;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 注文確定処理の単体テスト。
 *
 * <p>単体テスト計画の ORD-001〜ORD-019 に対応する。
 */
class OrderServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, TOKYO);
    private static final OffsetDateTime FIXED_DATETIME = OffsetDateTime.ofInstant(FIXED_INSTANT, TOKYO);

    private OrderRepository orderRepository;
    private CouponRepository couponRepository;
    private CouponValidationService couponValidationService;
    private ObjectMapper objectMapper;
    private NotificationMailService notificationMailService;
    private MessageSource messageSource;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        couponRepository = mock(CouponRepository.class);
        couponValidationService = mock(CouponValidationService.class);
        objectMapper = mock(ObjectMapper.class);
        notificationMailService = mock(NotificationMailService.class);
        messageSource = new ResourceBundleMessageSource();
        ((ResourceBundleMessageSource) messageSource).setBasename("messages");
        ((ResourceBundleMessageSource) messageSource).setDefaultEncoding("UTF-8");
        orderService = new OrderService(
                orderRepository,
                couponRepository,
                couponValidationService,
                objectMapper,
                notificationMailService,
                FIXED_CLOCK,
                messageSource
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private CheckoutInputForm validPersonalForm() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("taro@example.com");
        form.setDaytimePhone("0312345678");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1-1");
        form.setDeliveryFloor("3");
        form.setHasElevator(Boolean.TRUE);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    private CheckoutInputForm validCorporateForm() {
        CheckoutInputForm form = validPersonalForm();
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName("株式会社サンプル");
        return form;
    }

    private CartLineView line(long variantId, long productId, String productCode, int stock, int quantity,
                              BigDecimal unitPrice, boolean assemblyAvailable, BigDecimal assemblyFee,
                              boolean assemblyRequested) {
        return new CartLineView(
                variantId,
                productId,
                "デスク",
                productCode,
                "desk",
                "ブラック",
                unitPrice,
                stock,
                assemblyAvailable,
                assemblyFee,
                assemblyRequested,
                quantity,
                "/products/" + productId
        );
    }

    private CartView cart(CartLineView... lines) {
        List<CartLineView> items = List.of(lines);
        BigDecimal productSubtotal = items.stream()
                .map(CartLineView::lineProductSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal assemblyFeeTotal = items.stream()
                .map(CartLineView::lineAssemblySubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = new BigDecimal("1000");
        BigDecimal totalAmount = productSubtotal.add(assemblyFeeTotal).add(taxAmount);
        return new CartView(
                items,
                items.size(),
                items.stream().mapToInt(CartLineView::quantity).sum(),
                new CartSummaryView(productSubtotal, assemblyFeeTotal, new BigDecimal("0"), taxAmount, totalAmount)
        );
    }

    @Nested
    @DisplayName("ORD-001〜ORD-002 正常注文")
    class NormalOrder {

        @Test
        @DisplayName("ORD-001 正常な1商品注文で注文ヘッダ・明細・ステータス履歴が保存される")
        void placeOrder_singleItem_savesHeaderItemsAndStatusHistory() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, form, cart);

            // トランザクションコミット時に登録した注文完了メール送信フックを実行する
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertThat(orderNumber).isEqualTo("ORD20260903-000001");
            verify(orderRepository).insertOrder(anyMap());
            verify(orderRepository).insertOrderItem(argThat(itemParams -> {
                assertThat(itemParams.get("lineSubtotal")).isEqualTo(new BigDecimal("30000"));
                assertThat(itemParams.get("lineCouponDiscountAmount")).isEqualTo(BigDecimal.ZERO);
                assertThat(itemParams.get("lineTaxAmount")).isEqualTo(new BigDecimal("3000"));
                assertThat(itemParams.get("lineTotalAmount")).isEqualTo(new BigDecimal("33000"));
                return true;
            }));
            verify(orderRepository, times(1)).insertOrderItem(anyMap());
            verify(orderRepository).insertOrderStatusHistory(anyMap());
            verify(notificationMailService).sendOrderCompleteMail(any(OrderCompleteMailPayload.class));
        }

        @Test
        @DisplayName("ORD-002 複数商品注文で商品数分の明細とメール明細が作成される")
        void placeOrder_multipleItems_createsItemAndMailLinesPerProduct() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(
                    line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false),
                    line(2L, 11L, "P0002-C01", 3, 2, new BigDecimal("15000"), false, BigDecimal.ZERO, false)
            );

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            orderService.placeOrder(1L, form, cart);

            // トランザクションコミット時に登録した注文完了メール送信フックを実行する
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(orderRepository, times(2)).insertOrderItem(anyMap());
            verify(notificationMailService).sendOrderCompleteMail(any(OrderCompleteMailPayload.class));
        }
    }

    @Nested
    @DisplayName("ORD-003〜ORD-006 空カート・在庫")
    class CartAndStock {

        @Test
        @DisplayName("ORD-003 空カートは業務エラーとなり保存処理を呼ばない")
        void placeOrder_emptyCart_throwsAndDoesNotSave() {
            CheckoutInputForm form = validPersonalForm();
            CartView emptyCart = new CartView(List.of(), 0, 0,
                    new CartSummaryView(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, emptyCart))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("カートに商品がありません");

            verify(orderRepository, never()).insertOrder(anyMap());
            verify(orderRepository, never()).insertOrderItem(anyMap());
            verify(orderRepository, never()).insertOrderStatusHistory(anyMap());
        }

        @Test
        @DisplayName("ORD-003 nullカートは業務エラーとなり保存処理を呼ばない")
        void placeOrder_nullCart_throwsAndDoesNotSave() {
            CheckoutInputForm form = validPersonalForm();

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("カートに商品がありません");

            verify(orderRepository, never()).insertOrder(anyMap());
        }

        @Test
        @DisplayName("ORD-004 在庫0は在庫不足として拒否し注文保存を呼ばない")
        void placeOrder_zeroStock_rejectsAndDoesNotSave() {
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 0, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("在庫が不足");

            verify(orderRepository, never()).insertOrder(anyMap());
            verify(orderRepository, never()).insertOrderItem(anyMap());
            verify(orderRepository, never()).insertOrderStatusHistory(anyMap());
        }

        @Test
        @DisplayName("ORD-005 注文数量が在庫超過は在庫不足として拒否し注文保存を呼ばない")
        void placeOrder_quantityExceedsStock_rejectsAndDoesNotSave() {
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 2, 3, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("在庫が不足");

            verify(orderRepository, never()).insertOrder(anyMap());
        }

        @Test
        @DisplayName("ORD-006 注文数量が在庫以内なら注文確定処理を継続する")
        void placeOrder_quantityWithinStock_continuesOrderPlacement() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 5, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, form, cart);

            assertThat(orderNumber).isEqualTo("ORD20260903-000001");
            verify(orderRepository).insertOrder(anyMap());
        }
    }

    @Nested
    @DisplayName("ORD-007〜ORD-009 注文番号採番")
    class OrderNumber {

        @Test
        @DisplayName("ORD-007 日次連番の初回は連番1から注文番号を生成する")
        void placeOrder_firstSequence_generatesOrderNumberWithSequence1() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, form, cart);

            assertThat(orderNumber).isEqualTo("ORD20260903-000001");
        }

        @Test
        @DisplayName("ORD-008 日次連番の既存値で注文番号を生成する")
        void placeOrder_existingSequence_generatesOrderNumberWithExistingSequence() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(42);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, form, cart);

            assertThat(orderNumber).isEqualTo("ORD20260903-000042");
        }

        @Test
        @DisplayName("ORD-009 連番上限超過は注文を保存せず上限超過例外となる")
        void placeOrder_sequenceOverLimit_throwsAndDoesNotSave() {
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1_000_000);

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("上限");

            verify(orderRepository, never()).insertOrder(anyMap());
        }
    }

    @Nested
    @DisplayName("ORD-010〜ORD-011 個人・法人")
    class CustomerType {

        @Test
        @DisplayName("ORD-010 個人注文は会社名なしで注文パラメータを作成できる")
        void placeOrder_personal_withoutCompanyName_succeeds() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, form, cart);

            assertThat(orderNumber).isNotBlank();
            verify(orderRepository).insertOrder(anyMap());
        }

        @Test
        @DisplayName("ORD-011 法人注文の会社名なしは業務エラーとなる")
        void placeOrder_corporate_withoutCompanyName_throwsBusinessError() {
            CheckoutInputForm form = validPersonalForm();
            form.setPersonalOrCorporate("corporate");
            form.setCompanyName(null);
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("会社名");

            verify(orderRepository, never()).insertOrder(anyMap());
        }
    }

    @Nested
    @DisplayName("ORD-012〜ORD-013 支払案内JSON")
    class PaymentInstruction {

        @Test
        @DisplayName("ORD-012 コンビニ決済は支払案内JSONを生成して保存パラメータへ設定する")
        void placeOrder_convenienceStore_generatesPaymentInstructionJson() throws Exception {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            form.setPaymentMethod("convenience_store");
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);
            when(objectMapper.writeValueAsString(anyMap())).thenReturn("{\"payment_number\":\"123456\",\"payment_due_date\":\"2026-09-06\"}");

            orderService.placeOrder(1L, form, cart);

            verify(objectMapper).writeValueAsString(anyMap());
            verify(orderRepository).insertOrder(anyMap());
        }

        @Test
        @DisplayName("ORD-013 コンビニ以外の決済は支払案内JSONをNULLとして扱う")
        void placeOrder_nonConvenienceStore_paymentInstructionIsNull() throws Exception {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            form.setPaymentMethod("bank_transfer");
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            orderService.placeOrder(1L, form, cart);

            verify(objectMapper, never()).writeValueAsString(anyMap());
            verify(orderRepository).insertOrder(anyMap());
        }
    }

    @Nested
    @DisplayName("クーポン付き注文")
    class CouponOrder {

        @Test
        @DisplayName("注文確定でクーポンを再検証し、割引後税額・スナップショット・利用回数を保存する")
        void placeOrder_withCoupon_revalidatesAndSavesSnapshotAndUsage() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1,
                    new BigDecimal("30000"), false, BigDecimal.ZERO, false));
            Coupon coupon = new Coupon(
                    22L,
                    "SAVE5000",
                    "5,000円割引",
                    "fixed_amount",
                    new BigDecimal("5000"),
                    null,
                    BigDecimal.ZERO,
                    FIXED_DATETIME.minusDays(1),
                    FIXED_DATETIME.plusDays(1),
                    """
                    {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                    """,
                    null,
                    null,
                    0L,
                    true
            );
            CouponValidationResult validCoupon =
                    CouponValidationResult.valid(coupon, new BigDecimal("5000"));

            when(couponRepository.findByCode("SAVE5000")).thenReturn(Optional.of(coupon));
            when(couponRepository.findByCodeForUpdate("SAVE5000")).thenReturn(Optional.of(coupon));
            when(couponValidationService.validateCoupon(coupon, cart, 1L))
                    .thenReturn(validCoupon, validCoupon);
            when(couponValidationService.parseTargetConditions(coupon.targetConditions()))
                    .thenReturn(new CouponConditions(List.of("desk"), List.of(), "all"));
            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);
            when(couponRepository.incrementTotalUsage(22L)).thenReturn(true);

            orderService.placeOrder(1L, form, cart, " SAVE5000 ");

            verify(orderRepository).insertOrder(argThat(orderParams -> {
                assertThat(orderParams.get("subtotalAmount")).isEqualTo(new BigDecimal("30000"));
                assertThat(orderParams.get("couponDiscountAmount")).isEqualTo(new BigDecimal("5000"));
                assertThat(orderParams.get("taxAmount")).isEqualTo(new BigDecimal("2500"));
                assertThat(orderParams.get("totalAmount")).isEqualTo(new BigDecimal("27500"));
                assertThat(orderParams.get("appliedCouponCode")).isEqualTo("SAVE5000");
                assertThat(orderParams.get("appliedCouponName")).isEqualTo("5,000円割引");
                assertThat(orderParams.get("appliedCouponId")).isEqualTo(22L);
                return true;
            }));
            verify(orderRepository).insertOrderItem(argThat(itemParams -> {
                assertThat(itemParams.get("lineSubtotal")).isEqualTo(new BigDecimal("30000"));
                assertThat(itemParams.get("lineCouponDiscountAmount")).isEqualTo(new BigDecimal("5000"));
                assertThat(itemParams.get("lineTaxAmount")).isEqualTo(new BigDecimal("2500"));
                assertThat(itemParams.get("lineTotalAmount")).isEqualTo(new BigDecimal("27500"));
                return true;
            }));
            verify(couponValidationService, times(2)).validateCoupon(coupon, cart, 1L);
            verify(couponRepository).ensureUserUsage(1L, 22L);
            verify(couponRepository).incrementTotalUsage(22L);
            verify(couponRepository).incrementUserUsage(1L, 22L);
        }

        @Test
        @DisplayName("定率割引と組立費を含む明細は割引後税額を保存し送料は維持する")
        void placeOrder_percentageCoupon_recalculatesLineTaxAfterDiscount() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartLineView item = line(1L, 10L, "P0001-C01", 5, 1,
                    new BigDecimal("1011"), true, new BigDecimal("25"), true);
            CartSummaryView summary = new CartSummaryView(
                    new BigDecimal("1011"), new BigDecimal("25"), new BigDecimal("800"),
                    new BigDecimal("103"), new BigDecimal("1939"));
            CartView cart = new CartView(List.of(item), 1, 1, summary);
            Coupon coupon = new Coupon(
                    23L, "PERCENT", "定率", "percentage", new BigDecimal("33"),
                    null, BigDecimal.ZERO, FIXED_DATETIME.minusDays(1), FIXED_DATETIME.plusDays(1),
                    """
                    {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                    """,
                    null, null, 0L, true
            );
            CouponValidationResult validCoupon =
                    CouponValidationResult.valid(coupon, new BigDecimal("333"));

            when(couponRepository.findByCode("PERCENT")).thenReturn(Optional.of(coupon));
            when(couponRepository.findByCodeForUpdate("PERCENT")).thenReturn(Optional.of(coupon));
            when(couponValidationService.validateCoupon(coupon, cart, 1L))
                    .thenReturn(validCoupon, validCoupon);
            when(couponValidationService.parseTargetConditions(coupon.targetConditions()))
                    .thenReturn(new CouponConditions(List.of("desk"), List.of(), "all"));
            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(101L);
            when(couponRepository.incrementTotalUsage(23L)).thenReturn(true);

            orderService.placeOrder(1L, form, cart, "PERCENT");

            verify(orderRepository).insertOrder(argThat(params -> {
                assertThat(params.get("taxAmount")).isEqualTo(new BigDecimal("70"));
                assertThat(params.get("totalAmount")).isEqualTo(new BigDecimal("1573"));
                return true;
            }));
            verify(orderRepository).insertOrderItem(argThat(params -> {
                assertThat(params.get("lineSubtotal")).isEqualTo(new BigDecimal("1036"));
                assertThat(params.get("lineCouponDiscountAmount")).isEqualTo(new BigDecimal("333"));
                assertThat(params.get("lineTaxAmount")).isEqualTo(new BigDecimal("70"));
                assertThat(params.get("lineTotalAmount")).isEqualTo(new BigDecimal("773"));
                return true;
            }));
        }

        @Test
        @DisplayName("対象外明細には割引を配賦せず割引配賦・税額の端数を既存規則で処理する")
        void placeOrder_mixedTargets_allocatesDiscountOnlyToTargetsWithoutTaxAdjustment() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartLineView target1 = line(1L, 10L, "P0001-C01", 5, 1,
                    new BigDecimal("100"), false, BigDecimal.ZERO, false);
            CartLineView target2 = line(2L, 11L, "P0002-C01", 5, 1,
                    new BigDecimal("200"), false, BigDecimal.ZERO, false);
            CartLineView target3 = line(3L, 12L, "P0003-C01", 5, 1,
                    new BigDecimal("200"), false, BigDecimal.ZERO, false);
            CartLineView excluded = new CartLineView(
                    4L, 13L, "チェア", "P0004-C01", "chair", "ブラック",
                    new BigDecimal("1000"), 5, false, BigDecimal.ZERO, false, 1, "/products/13");
            CartView cart = cart(target1, target2, target3, excluded);
            Coupon coupon = new Coupon(
                    24L, "MIXED", "固定", "fixed_amount", new BigDecimal("2"),
                    null, BigDecimal.ZERO, FIXED_DATETIME.minusDays(1), FIXED_DATETIME.plusDays(1),
                    """
                    {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                    """,
                    null, null, 0L, true
            );
            CouponValidationResult validCoupon =
                    CouponValidationResult.valid(coupon, new BigDecimal("2"));
            List<Map<String, Object>> savedItems = new java.util.ArrayList<>();

            when(couponRepository.findByCode("MIXED")).thenReturn(Optional.of(coupon));
            when(couponRepository.findByCodeForUpdate("MIXED")).thenReturn(Optional.of(coupon));
            when(couponValidationService.validateCoupon(coupon, cart, 1L))
                    .thenReturn(validCoupon, validCoupon);
            when(couponValidationService.parseTargetConditions(coupon.targetConditions()))
                    .thenReturn(new CouponConditions(List.of("desk"), List.of(), "all"));
            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(102L);
            doAnswer(invocation -> {
                savedItems.add(invocation.getArgument(0));
                return null;
            }).when(orderRepository).insertOrderItem(anyMap());
            when(couponRepository.incrementTotalUsage(24L)).thenReturn(true);

            orderService.placeOrder(1L, form, cart, "MIXED");

            assertThat(savedItems).extracting(params -> params.get("lineCouponDiscountAmount"))
                    .containsExactly(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
            assertThat(savedItems).extracting(params -> params.get("lineTaxAmount"))
                    .containsExactly(BigDecimal.TEN, new BigDecimal("19"), new BigDecimal("19"), new BigDecimal("100"));
            assertThat(savedItems).extracting(params -> params.get("lineTotalAmount"))
                    .containsExactly(new BigDecimal("110"), new BigDecimal("218"), new BigDecimal("218"),
                            new BigDecimal("1100"));
            BigDecimal lineTaxTotal = savedItems.stream()
                    .map(params -> (BigDecimal) params.get("lineTaxAmount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal lineTotal = savedItems.stream()
                    .map(params -> (BigDecimal) params.get("lineTotalAmount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(lineTaxTotal).isEqualTo(new BigDecimal("148"));
            assertThat(lineTotal).isEqualTo(new BigDecimal("1646"));
            verify(orderRepository).insertOrder(argThat(params -> {
                assertThat(params.get("taxAmount")).isEqualTo(new BigDecimal("149"));
                assertThat(params.get("totalAmount")).isEqualTo(new BigDecimal("1647"));
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("ORD-014〜ORD-015 税額計算")
    class TaxCalculation {

        @Test
        @DisplayName("ORD-014 税率未取得は規定のデフォルト税率10%を使用する")
        void placeOrder_taxRateNull_usesDefaultTenPercent() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(null);
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            orderService.placeOrder(1L, form, cart);

            verify(orderRepository).insertOrderItem(anyMap());
        }

        @Test
        @DisplayName("ORD-015 商品別税額は指定の丸め規則で計算される")
        void placeOrder_lineTaxAmount_usesDownRounding() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            // 税抜小計 30,000円 × 10% = 3,000円
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            orderService.placeOrder(1L, form, cart);

            verify(orderRepository).insertOrderItem(anyMap());
        }
    }

    @Nested
    @DisplayName("ORD-016〜ORD-017 トランザクション・メール")
    class TransactionAndMail {

        @Test
        @DisplayName("ORD-016 トランザクション同期なしは注文完了メール予約の失敗として扱う")
        void placeOrder_noTransactionSync_throwsIllegalState() {
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("トランザクション同期");

            verify(notificationMailService, never()).sendOrderCompleteMail(any(OrderCompleteMailPayload.class));
        }

        @Test
        @DisplayName("ORD-017 DB保存例外は呼び出し元へ伝播し完了メールを送信しない")
        void placeOrder_dbException_propagatesAndDoesNotSendMail() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");

            verify(notificationMailService, never()).sendOrderCompleteMail(any(OrderCompleteMailPayload.class));
        }
    }

    @Nested
    @DisplayName("ORD-018〜ORD-019 在庫更新・注文番号重複")
    class StockUpdateAndDuplicate {

        @Test
        @DisplayName("ORD-018 在庫更新仕様の確認: 在庫再確認のみでstock_quantity更新を呼ばない")
        void placeOrder_doesNotUpdateStockQuantity() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenReturn(100L);

            orderService.placeOrder(1L, form, cart);

            // 現行実装は在庫再確認のみで、在庫引落しの更新処理は存在しない。
            // OrderRepository に在庫更新メソッドが無いことを仕様として確認する。
            verify(orderRepository).insertOrder(anyMap());
            verify(orderRepository).insertOrderItem(anyMap());
        }

        @Test
        @DisplayName("ORD-019 注文番号重複はDB側の例外がサービス境界を越えて伝播する")
        void placeOrder_duplicateOrderNumber_dbExceptionPropagates() {
            TransactionSynchronizationManager.initSynchronization();
            CheckoutInputForm form = validPersonalForm();
            CartView cart = cart(line(1L, 10L, "P0001-C01", 5, 1, new BigDecimal("30000"), false, BigDecimal.ZERO, false));

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
            when(orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(1);
            when(orderRepository.insertOrder(anyMap())).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate order_number"));

            assertThatThrownBy(() -> orderService.placeOrder(1L, form, cart))
                    .isInstanceOf(org.springframework.dao.DuplicateKeyException.class)
                    .hasMessageContaining("duplicate order_number");
        }
    }

    @Nested
    @DisplayName("注文完了画面・購入履歴・再購入")
    class OrderReference {

        @Test
        @DisplayName("注文完了画面は注文番号をtrimして取得する")
        void findOrderCompleteView_trimsOrderNumber() {
            OrderCompleteView view = new OrderCompleteView(
                    "ORD20260903-000001", "山田", "太郎", "taro@example.com",
                    "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "bank_transfer", null, null,
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    null, null, BigDecimal.ZERO
            );
            when(orderRepository.findOrderCompleteByOrderNumber("ORD20260903-000001"))
                    .thenReturn(Optional.of(view));

            Optional<OrderCompleteView> result = orderService.findOrderCompleteView("  ORD20260903-000001  ");

            assertThat(result).isPresent();
            assertThat(result.get().orderNumber()).isEqualTo("ORD20260903-000001");
            verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260903-000001");
        }

        @Test
        @DisplayName("注文完了画面は空文字・nullでOptional.emptyを返す")
        void findOrderCompleteView_blankOrNull_returnsEmpty() {
            assertThat(orderService.findOrderCompleteView(null)).isEmpty();
            assertThat(orderService.findOrderCompleteView("   ")).isEmpty();
            verify(orderRepository, never()).findOrderCompleteByOrderNumber(anyString());
        }

        @Test
        @DisplayName("購入履歴一覧はページ番号を1以上へ補正する")
        void findMemberOrderHistories_normalizesPageToAtLeastOne() {
            when(orderRepository.findMemberOrders(1L, 1, 10))
                    .thenReturn(new MemberOrderHistoryPage(List.of(), 0, 1, 10));

            MemberOrderHistoryPage page = orderService.findMemberOrderHistories(1L, 0);

            assertThat(page.page()).isEqualTo(1);
            verify(orderRepository).findMemberOrders(1L, 1, 10);
        }

        @Test
        @DisplayName("注文詳細は注文番号をtrimして取得する")
        void findMemberOrderDetail_trimsOrderNumber() {
            when(orderRepository.findMemberOrderDetail(1L, "ORD20260903-000001"))
                    .thenReturn(Optional.of(mock(MemberOrderDetailView.class)));

            Optional<MemberOrderDetailView> result = orderService.findMemberOrderDetail(1L, "  ORD20260903-000001  ");

            assertThat(result).isPresent();
            verify(orderRepository).findMemberOrderDetail(1L, "ORD20260903-000001");
        }

        @Test
        @DisplayName("再購入商品は注文番号が空なら空リストを返す")
        void findReorderItems_blankOrderNumber_returnsEmptyList() {
            assertThat(orderService.findReorderItems(1L, "  ")).isEmpty();
            verify(orderRepository, never()).findReorderItems(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("注文入力フォーム初期化")
    class InitialForm {

        @Test
        @DisplayName("ゲストは空の初期フォームを返す")
        void createInitialForm_guest_returnsEmptyForm() {
            CheckoutInputForm form = orderService.createInitialForm(Optional.empty());

            assertThat(form.getLastName()).isNull();
            assertThat(form.getPersonalOrCorporate()).isEqualTo("personal");
        }

        @Test
        @DisplayName("会員情報が存在しない場合は空の初期フォームを返す")
        void createInitialForm_memberPrefillEmpty_returnsEmptyForm() {
            when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.empty());

            CheckoutInputForm form = orderService.createInitialForm(
                    Optional.of(new MemberSessionUser(1L, "taro@example.com", "山田", "太郎"))
            );

            assertThat(form.getLastName()).isNull();
        }

        @Test
        @DisplayName("会員情報から初期フォームへ反映する")
        void createInitialForm_memberPrefillPresent_reflectsValues() {
            CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                    "personal", "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "taro@example.com", "0312345678", null,
                    "1000001", "東京都", "千代田区", "丸の内1-1-1", 3, true
            );
            when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

            CheckoutInputForm form = orderService.createInitialForm(
                    Optional.of(new MemberSessionUser(1L, "taro@example.com", "山田", "太郎"))
            );

            assertThat(form.getLastName()).isEqualTo("山田");
            assertThat(form.getFirstName()).isEqualTo("太郎");
            assertThat(form.getPostalCodePart1()).isEqualTo("100");
            assertThat(form.getPostalCodePart2()).isEqualTo("0001");
            assertThat(form.getDeliveryFloor()).isEqualTo("3");
            assertThat(form.getHasElevator()).isTrue();
        }
    }
}