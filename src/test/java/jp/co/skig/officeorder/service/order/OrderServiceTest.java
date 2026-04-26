package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    NotificationMailService notificationMailService;

    @Mock
    MessageSource messageSource;

    // 2026-04-15 09:00:00 JST で固定
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.ofHours(9));

    OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        orderService = new OrderService(
                orderRepository, objectMapper, notificationMailService, fixedClock, messageSource);
        // placeOrder 内の TransactionSynchronizationManager を有効化
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- createInitialForm ---

    @Test
    void createInitialForm_noMember_returnsEmptyForm() {
        CheckoutInputForm form = orderService.createInitialForm(Optional.empty());

        assertThat(form).isNotNull();
        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    @Test
    void createInitialForm_memberWithPrefill_returnsPrefilledForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "taro@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "taro@example.com", "0312345678", null,
                "1000001", "東京都", "千代田区", "丸の内1-1", 3, true);
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = orderService.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isEqualTo("山田");
        assertThat(form.getFirstName()).isEqualTo("太郎");
        assertThat(form.getEmail()).isEqualTo("taro@example.com");
        assertThat(form.getDeliveryFloor()).isEqualTo("3");
    }

    @Test
    void createInitialForm_memberWithNoPrefill_returnsEmptyForm() {
        MemberSessionUser member = new MemberSessionUser(2L, "hanako@example.com", "佐藤", "花子");
        when(orderRepository.findCheckoutMemberPrefill(2L)).thenReturn(Optional.empty());

        CheckoutInputForm form = orderService.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isNull();
    }

    @Test
    void createInitialForm_memberWithShortPostalCode_doesNotSplitPostalCode() {
        MemberSessionUser member = new MemberSessionUser(3L, "test@example.com", "田中", "次郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "田中", "次郎", "タナカ", "ジロウ",
                null, null, "test@example.com", "0312345678", null,
                "10001", "東京都", "千代田区", "丸の内1-1", 1, false);
        when(orderRepository.findCheckoutMemberPrefill(3L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = orderService.createInitialForm(Optional.of(member));

        assertThat(form.getPostalCodePart1()).isNull();
        assertThat(form.getPostalCodePart2()).isNull();
    }

    // --- placeOrder ---

    @Test
    void placeOrder_emptyCart_throwsIllegalArgumentException() {
        CartView emptyCart = new CartView(List.of(), 0, 0,
                new CartSummaryView(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), emptyCart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).insertOrder(any());
    }

    @Test
    void placeOrder_nullCart_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_corporateWithoutCompanyName_throwsIllegalArgumentException() {
        CheckoutInputForm form = buildValidForm();
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName(null);
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);

        assertThatThrownBy(() -> orderService.placeOrder(null, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_stockQuantityZero_throwsIllegalArgumentException() {
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 0); // stockQuantity = 0

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_quantityExceedsStock_throwsIllegalArgumentException() {
        CartView cart = buildValidCart(new BigDecimal("50000"), 5, 3); // quantity=5, stock=3

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_blankDeliveryFloor_throwsIllegalArgumentException() {
        CheckoutInputForm form = buildValidForm();
        form.setDeliveryFloor("  ");
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);

        assertThatThrownBy(() -> orderService.placeOrder(null, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_nonNumericDeliveryFloor_throwsIllegalArgumentException() {
        CheckoutInputForm form = buildValidForm();
        form.setDeliveryFloor("ABC");
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);

        assertThatThrownBy(() -> orderService.placeOrder(null, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_sequenceExceeded_throwsIllegalStateException() {
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(1_000_000); // over max

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), cart))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("日次連番");
    }

    @Test
    void placeOrder_guestBankTransfer_returnsOrderNumber() {
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(1);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String result = orderService.placeOrder(null, buildValidForm(), cart);

        assertThat(result).startsWith("ORD20260415-");
        assertThat(result).endsWith("000001");
    }

    @Test
    void placeOrder_memberConvenienceStore_returnsOrderNumber() throws Exception {
        CheckoutInputForm form = buildValidForm();
        form.setPaymentMethod("convenience_store");
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(2);
        when(orderRepository.insertOrder(any())).thenReturn(2L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payment_number\":\"123456\"}");

        String result = orderService.placeOrder(1L, form, cart);

        assertThat(result).startsWith("ORD");
    }

    @Test
    void placeOrder_cashOnDelivery_returnsOrderNumber() {
        CheckoutInputForm form = buildValidForm();
        form.setPaymentMethod("cash_on_delivery");
        CartView cart = buildValidCart(new BigDecimal("50000"), 1, 10);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(3);
        when(orderRepository.insertOrder(any())).thenReturn(3L);

        String result = orderService.placeOrder(null, form, cart);

        assertThat(result).startsWith("ORD");
    }

    @Test
    void placeOrder_assemblyItem_insertsOrderWithAssemblyInfo() {
        CartView cart = buildValidCartWithAssembly(new BigDecimal("50000"), new BigDecimal("5000"), 1, 10);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(4);
        when(orderRepository.insertOrder(any())).thenReturn(4L);

        String result = orderService.placeOrder(null, buildValidForm(), cart);

        assertThat(result).startsWith("ORD");
        verify(orderRepository).insertOrderItem(any());
        verify(orderRepository).insertOrderStatusHistory(any());
    }

    // --- findOrderCompleteView ---

    @Test
    void findOrderCompleteView_blankOrderNumber_returnsEmpty() {
        Optional<OrderCompleteView> result = orderService.findOrderCompleteView("  ");

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findOrderCompleteByOrderNumber(any());
    }

    @Test
    void findOrderCompleteView_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20260415-000001"))
                .thenReturn(Optional.empty());

        Optional<OrderCompleteView> result = orderService.findOrderCompleteView("ORD20260415-000001");

        assertThat(result).isEmpty();
        verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260415-000001");
    }

    // --- findMemberOrderHistories (page normalization) ---

    @Test
    void findMemberOrderHistories_zeroPage_normalizesToOne() {
        orderService.findMemberOrderHistories(1L, 0);

        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_negativePage_normalizesToOne() {
        orderService.findMemberOrderHistories(1L, -3);

        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_validPage_keepsPage() {
        orderService.findMemberOrderHistories(1L, 2);

        verify(orderRepository).findMemberOrders(1L, 2, 10);
    }

    // --- findMemberOrderDetail ---

    @Test
    void findMemberOrderDetail_blankOrderNumber_returnsEmpty() {
        Optional<jp.co.skig.officeorder.model.member.MemberOrderDetailView> result =
                orderService.findMemberOrderDetail(1L, "  ");

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findMemberOrderDetail(anyLong(), any());
    }

    @Test
    void findMemberOrderDetail_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findMemberOrderDetail(1L, "ORD20260415-000001"))
                .thenReturn(Optional.empty());

        orderService.findMemberOrderDetail(1L, "ORD20260415-000001");

        verify(orderRepository).findMemberOrderDetail(1L, "ORD20260415-000001");
    }

    // --- findReorderItems ---

    @Test
    void findReorderItems_blankOrderNumber_returnsEmptyList() {
        List<jp.co.skig.officeorder.model.order.OrderReorderItem> result =
                orderService.findReorderItems(1L, "  ");

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findReorderItems(anyLong(), any());
    }

    @Test
    void findReorderItems_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findReorderItems(1L, "ORD20260415-000001")).thenReturn(List.of());

        orderService.findReorderItems(1L, "ORD20260415-000001");

        verify(orderRepository).findReorderItems(1L, "ORD20260415-000001");
    }

    // --- helpers ---

    private CheckoutInputForm buildValidForm() {
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
        form.setAddressLine("丸の内1-1");
        form.setDeliveryFloor("3");
        form.setHasElevator(true);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    private CartView buildValidCart(BigDecimal unitPrice, int quantity, int stockQuantity) {
        CartLineView line = new CartLineView(
                1L, 101L, "テストデスク", "CODE-001", "白",
                unitPrice, stockQuantity, false, BigDecimal.ZERO, false, quantity, "/products/101");
        BigDecimal productSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal taxAmount = productSubtotal
                .multiply(new BigDecimal("10"))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);
        BigDecimal shippingFee = productSubtotal.add(taxAmount)
                .compareTo(BigDecimal.valueOf(5000)) >= 0
                ? BigDecimal.ZERO : new BigDecimal("800");
        CartSummaryView summary = new CartSummaryView(
                productSubtotal, BigDecimal.ZERO, shippingFee, taxAmount,
                productSubtotal.add(taxAmount).add(shippingFee));
        return new CartView(List.of(line), 1, quantity, summary);
    }

    private CartView buildValidCartWithAssembly(
            BigDecimal unitPrice, BigDecimal assemblyFee, int quantity, int stockQuantity) {
        CartLineView line = new CartLineView(
                1L, 101L, "組立デスク", "CODE-A01", "白",
                unitPrice, stockQuantity, true, assemblyFee, true, quantity, "/products/101");
        BigDecimal productSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal assemblyFeeTotal = assemblyFee.multiply(BigDecimal.valueOf(quantity));
        BigDecimal taxableSubtotal = productSubtotal.add(assemblyFeeTotal);
        BigDecimal taxAmount = taxableSubtotal
                .multiply(new BigDecimal("10"))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);
        BigDecimal totalAmount = taxableSubtotal.add(taxAmount);
        CartSummaryView summary = new CartSummaryView(
                productSubtotal, assemblyFeeTotal, BigDecimal.ZERO, taxAmount, totalAmount);
        return new CartView(List.of(line), 1, quantity, summary);
    }
}
