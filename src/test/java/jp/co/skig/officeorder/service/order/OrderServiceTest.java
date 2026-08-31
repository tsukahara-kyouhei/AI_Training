package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.ObjectMapper;

import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ObjectMapper objectMapper;
    private NotificationMailService notificationMailService;
    private Clock appClock;
    private MessageSource messageSource;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        objectMapper = mock(ObjectMapper.class);
        notificationMailService = mock(NotificationMailService.class);
        messageSource = mock(MessageSource.class);

        appClock = Clock.fixed(
                Instant.parse("2026-08-17T10:00:00Z"),
                ZoneOffset.UTC);

        service = new OrderService(
                orderRepository,
                objectMapper,
                notificationMailService,
                appClock,
                messageSource);

        when(messageSource.getMessage(
                anyString(),
                any(),
                any(Locale.class))).thenReturn("message");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // =========================================================
    // createInitialForm
    // =========================================================

    @Test
    void createInitialForm_正常系_未ログインなら空フォームを返す() {
        CheckoutInputForm result = service.createInitialForm(Optional.empty());

        assertThat(result).isNotNull();
        assertThat(result.getPersonalOrCorporate())
                .isEqualTo("personal");

        verify(orderRepository, never())
                .findCheckoutMemberPrefill(any(Long.class));
    }

    @Test
    void createInitialForm_正常系_会員情報が存在しないなら空フォームを返す() {
        MemberSessionUser member = new MemberSessionUser(
                1L,
                "test@example.com",
                "山田",
                "太郎");

        when(orderRepository.findCheckoutMemberPrefill(1L))
                .thenReturn(Optional.empty());

        CheckoutInputForm result = service.createInitialForm(Optional.of(member));

        assertThat(result).isNotNull();
        assertThat(result.getPersonalOrCorporate())
                .isEqualTo("personal");

        verify(orderRepository)
                .findCheckoutMemberPrefill(1L);
    }

    @Test
    void createInitialForm_正常系_会員情報をフォームへ設定する() {
        MemberSessionUser member = new MemberSessionUser(
                1L,
                "test@example.com",
                "山田",
                "太郎");

        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal",
                "山田",
                "太郎",
                "ヤマダ",
                "タロウ",
                "株式会社サンプル",
                "営業部",
                "test@example.com",
                "090-1234-5678",
                "03-1234-5678",
                "1234567",
                "東京都",
                "渋谷区",
                "1-1-1",
                5,
                true);

        when(orderRepository.findCheckoutMemberPrefill(1L))
                .thenReturn(Optional.of(prefill));

        CheckoutInputForm result = service.createInitialForm(Optional.of(member));

        assertThat(result.getPersonalOrCorporate())
                .isEqualTo("personal");
        assertThat(result.getLastName())
                .isEqualTo("山田");
        assertThat(result.getFirstName())
                .isEqualTo("太郎");
        assertThat(result.getLastNameKana())
                .isEqualTo("ヤマダ");
        assertThat(result.getFirstNameKana())
                .isEqualTo("タロウ");
        assertThat(result.getCompanyName())
                .isEqualTo("株式会社サンプル");
        assertThat(result.getDepartmentName())
                .isEqualTo("営業部");
        assertThat(result.getEmail())
                .isEqualTo("test@example.com");
        assertThat(result.getDaytimePhone())
                .isEqualTo("090-1234-5678");
        assertThat(result.getFax())
                .isEqualTo("03-1234-5678");
        assertThat(result.getPostalCodePart1())
                .isEqualTo("123");
        assertThat(result.getPostalCodePart2())
                .isEqualTo("4567");
        assertThat(result.getPrefecture())
                .isEqualTo("東京都");
        assertThat(result.getCity())
                .isEqualTo("渋谷区");
        assertThat(result.getAddressLine())
                .isEqualTo("1-1-1");
        assertThat(result.getDeliveryFloor())
                .isEqualTo("5");
        assertThat(result.getHasElevator())
                .isTrue();
    }

    // =========================================================
    // placeOrder
    // =========================================================

    @Test
    void placeOrder_異常系_カートがnullなら例外を送出する() {
        CheckoutInputForm form = createValidForm();

        assertThatThrownBy(() -> service.placeOrder(1L, form, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_空カートなら例外を送出する() {
        CheckoutInputForm form = createValidForm();

        CartView cart = new CartView(
                List.of(),
                0,
                0,
                createSummary());

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_法人で会社名が未入力なら例外を送出する() {
        CheckoutInputForm form = createValidForm();
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName(" ");

        CartView cart = createCart();

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_在庫が0なら例外を送出する() {
        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                0,
                false,
                BigDecimal.ZERO,
                false,
                1,
                "/products/1");

        CartView cart = createCart(line);

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_注文数量が在庫数量を超えるなら例外を送出する() {
        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                2,
                false,
                BigDecimal.ZERO,
                false,
                3,
                "/products/1");

        CartView cart = createCart(line);

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_階数が未入力なら例外を送出する() {
        CheckoutInputForm form = createValidForm();
        form.setDeliveryFloor(" ");

        CartView cart = createCart();

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_階数が数値以外なら例外を送出する() {
        CheckoutInputForm form = createValidForm();
        form.setDeliveryFloor("abc");

        CartView cart = createCart();

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_異常系_注文番号連番が上限を超えるなら例外を送出する()
            throws Exception {

        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                10,
                true,
                BigDecimal.valueOf(3000),
                true,
                2,
                "/products/1");

        CartView cart = createCart(line);

        when(orderRepository.findCurrentTaxRatePercent())
                .thenReturn(BigDecimal.TEN);

        when(orderRepository.nextOrderSequence(any(LocalDate.class)))
                .thenReturn(1_000_000);

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalStateException.class);

        verify(orderRepository, never())
                .insertOrder(anyMap());
    }

    @Test
    void placeOrder_正常系_注文を登録して注文番号を返す()
            throws Exception {

        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                10,
                true,
                BigDecimal.valueOf(3000),
                true,
                2,
                "/products/1");

        CartView cart = createCart(line);

        when(orderRepository.findCurrentTaxRatePercent())
                .thenReturn(BigDecimal.TEN);

        when(orderRepository.nextOrderSequence(
                LocalDate.of(2026, 8, 17)))
                .thenReturn(1);

        when(orderRepository.insertOrder(anyMap()))
                .thenReturn(100L);

        TransactionSynchronizationManager.initSynchronization();

        String result = service.placeOrder(1L, form, cart);

        assertThat(result)
                .isEqualTo("ORD20260817-000001");

        verify(orderRepository)
                .insertOrder(anyMap());

        verify(orderRepository)
                .insertOrderItem(anyMap());

        verify(orderRepository)
                .insertOrderStatusHistory(anyMap());

        verify(orderRepository)
                .findCurrentTaxRatePercent();

        verify(orderRepository)
                .nextOrderSequence(LocalDate.of(2026, 8, 17));

        assertThat(
                TransactionSynchronizationManager
                        .getSynchronizations())
                .hasSize(1);
    }

    @Test
    void placeOrder_正常系_組立希望の場合は組立費を明細へ設定する()
            throws Exception {

        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                10,
                true,
                BigDecimal.valueOf(3000),
                true,
                2,
                "/products/1");

        CartView cart = createCart(line);

        when(orderRepository.findCurrentTaxRatePercent())
                .thenReturn(BigDecimal.TEN);

        when(orderRepository.nextOrderSequence(any(LocalDate.class)))
                .thenReturn(1);

        when(orderRepository.insertOrder(anyMap()))
                .thenReturn(100L);

        TransactionSynchronizationManager.initSynchronization();

        service.placeOrder(1L, form, cart);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(orderRepository)
                .insertOrderItem(captor.capture());

        Map<String, Object> itemParams = captor.getValue();

        assertThat(itemParams.get("assemblyAvailable"))
                .isEqualTo(true);

        assertThat(itemParams.get("assemblyFee"))
                .isEqualTo(BigDecimal.valueOf(3000));
    }

    @Test
    void placeOrder_正常系_組立希望なしの場合は組立費を0円にする()
            throws Exception {

        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                10,
                true,
                BigDecimal.valueOf(3000),
                false,
                2,
                "/products/1");

        CartView cart = createCart(line);

        when(orderRepository.findCurrentTaxRatePercent())
                .thenReturn(BigDecimal.TEN);

        when(orderRepository.nextOrderSequence(any(LocalDate.class)))
                .thenReturn(1);

        when(orderRepository.insertOrder(anyMap()))
                .thenReturn(100L);

        TransactionSynchronizationManager.initSynchronization();

        service.placeOrder(1L, form, cart);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(orderRepository)
                .insertOrderItem(captor.capture());

        Map<String, Object> itemParams = captor.getValue();

        assertThat(itemParams.get("assemblyFee"))
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void placeOrder_異常系_トランザクション同期が無効なら例外を送出する() {

        CheckoutInputForm form = createValidForm();

        CartLineView line = new CartLineView(
                1L,
                10L,
                "ワークデスク",
                "P0001-C01",
                "ホワイト",
                BigDecimal.valueOf(10000),
                10,
                true,
                BigDecimal.valueOf(3000),
                true,
                2,
                "/products/1");

        CartView cart = createCart(line);

        when(orderRepository.findCurrentTaxRatePercent())
                .thenReturn(BigDecimal.TEN);

        when(orderRepository.nextOrderSequence(any(LocalDate.class)))
                .thenReturn(1);

        when(orderRepository.insertOrder(anyMap()))
                .thenReturn(100L);

        assertThatThrownBy(() -> service.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================
    // findOrderCompleteView
    // =========================================================

    @Test
    void findOrderCompleteView_異常系_注文番号が空なら空を返す() {

        Optional<OrderCompleteView> result = service.findOrderCompleteView(" ");

        assertThat(result).isEmpty();

        verify(orderRepository, never())
                .findOrderCompleteByOrderNumber(anyString());
    }

    @Test
    void findOrderCompleteView_正常系_注文番号をtrimして検索する() {

        OrderCompleteView expected = mock(OrderCompleteView.class);

        when(orderRepository.findOrderCompleteByOrderNumber("ORD001"))
                .thenReturn(Optional.of(expected));

        Optional<OrderCompleteView> result = service.findOrderCompleteView("  ORD001  ");

        assertThat(result)
                .contains(expected);

        verify(orderRepository)
                .findOrderCompleteByOrderNumber("ORD001");
    }

    // =========================================================
    // findMemberOrderHistories
    // =========================================================

    @Test
    void findMemberOrderHistories_正常系_ページ番号0以下を1に補正する() {

        MemberOrderHistoryPage expected = mock(MemberOrderHistoryPage.class);

        when(orderRepository.findMemberOrders(10L, 1, 10))
                .thenReturn(expected);

        MemberOrderHistoryPage result = service.findMemberOrderHistories(10L, 0);

        assertThat(result)
                .isSameAs(expected);

        verify(orderRepository)
                .findMemberOrders(10L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_正常系_指定ページをそのまま渡す() {

        MemberOrderHistoryPage expected = mock(MemberOrderHistoryPage.class);

        when(orderRepository.findMemberOrders(10L, 3, 10))
                .thenReturn(expected);

        MemberOrderHistoryPage result = service.findMemberOrderHistories(10L, 3);

        assertThat(result)
                .isSameAs(expected);

        verify(orderRepository)
                .findMemberOrders(10L, 3, 10);
    }

    // =========================================================
    // findMemberOrderDetail
    // =========================================================

    @Test
    void findMemberOrderDetail_異常系_注文番号が空なら空を返す() {

        Optional<MemberOrderDetailView> result = service.findMemberOrderDetail(10L, " ");

        assertThat(result).isEmpty();

        verify(orderRepository, never())
                .findMemberOrderDetail(
                        any(Long.class),
                        anyString());
    }

    @Test
    void findMemberOrderDetail_正常系_注文番号をtrimして検索する() {

        MemberOrderDetailView expected = mock(MemberOrderDetailView.class);

        when(orderRepository.findMemberOrderDetail(
                10L,
                "ORD001")).thenReturn(Optional.of(expected));

        Optional<MemberOrderDetailView> result = service.findMemberOrderDetail(
                10L,
                " ORD001 ");

        assertThat(result)
                .contains(expected);

        verify(orderRepository)
                .findMemberOrderDetail(10L, "ORD001");
    }

    // =========================================================
    // findReorderItems
    // =========================================================

    @Test
    void findReorderItems_異常系_注文番号が空なら空リストを返す() {

        List<OrderReorderItem> result = service.findReorderItems(10L, " ");

        assertThat(result)
                .isEmpty();

        verify(orderRepository, never())
                .findReorderItems(
                        any(Long.class),
                        anyString());
    }

    @Test
    void findReorderItems_正常系_注文番号をtrimして検索する() {

        List<OrderReorderItem> expected = List.of(mock(OrderReorderItem.class));

        when(orderRepository.findReorderItems(
                10L,
                "ORD001")).thenReturn(expected);

        List<OrderReorderItem> result = service.findReorderItems(
                10L,
                " ORD001 ");

        assertThat(result)
                .isSameAs(expected);

        verify(orderRepository)
                .findReorderItems(10L, "ORD001");
    }

    // =========================================================
    // Test data
    // =========================================================

    private CheckoutInputForm createValidForm() {
        CheckoutInputForm form = new CheckoutInputForm();

        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("test@example.com");
        form.setDaytimePhone("090-1234-5678");
        form.setPostalCodePart1("123");
        form.setPostalCodePart2("4567");
        form.setPrefecture("東京都");
        form.setCity("渋谷区");
        form.setAddressLine("1-1-1");
        form.setDeliveryFloor("5");
        form.setHasElevator(true);
        form.setPaymentMethod("bank_transfer");

        return form;
    }

    private CartView createCart(
            CartLineView... lines) {
        return new CartView(
                List.of(lines),
                lines.length,
                lines.length,
                createSummary());
    }

    private CartSummaryView createSummary() {
        return new CartSummaryView(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(500),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1350),
                BigDecimal.valueOf(14850));
    }
}