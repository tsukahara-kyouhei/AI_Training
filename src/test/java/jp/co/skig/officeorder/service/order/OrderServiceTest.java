package jp.co.skig.officeorder.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T00:00:00Z");
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationMailService notificationMailService;

    @Mock
    private MessageSource messageSource;

    private OrderService sut;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, JST);
        sut = new OrderService(orderRepository, objectMapper, notificationMailService, clock, messageSource);
    }

    // ─── createInitialForm ──────────────────────────────────────────────

    @Test
    void createInitialForm_guest_returns_empty_form() {
        // Act
        CheckoutInputForm result = sut.createInitialForm(Optional.empty());

        // Assert
        assertThat(result.getLastName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void createInitialForm_member_but_prefill_not_found_returns_empty_form() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.empty());

        // Act
        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void createInitialForm_member_with_7digit_postal_code_splits_into_two_parts() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "1234567", "東京都", "渋谷区", "渋谷1-1", 3, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        // Act
        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getPostalCodePart1()).isEqualTo("123");
        assertThat(result.getPostalCodePart2()).isEqualTo("4567");
    }

    @Test
    void createInitialForm_member_with_null_postal_code_does_not_populate_postal_parts() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                null, "東京都", "渋谷区", "渋谷1-1", 3, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        // Act
        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getPostalCodePart1()).isNull();
        assertThat(result.getPostalCodePart2()).isNull();
    }

    // ─── findOrderCompleteView ──────────────────────────────────────────

    @Test
    void findOrderCompleteView_blank_order_number_returns_empty() {
        // Act
        Optional<OrderCompleteView> result = sut.findOrderCompleteView("  ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findOrderCompleteView_null_order_number_returns_empty() {
        // Act
        Optional<OrderCompleteView> result = sut.findOrderCompleteView(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findOrderCompleteView_valid_order_number_trims_and_delegates_to_repository() {
        // Arrange
        OrderCompleteView view = new OrderCompleteView("ORD20260414-000001", null, null, null, null,
                null, null, null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20260414-000001"))
                .thenReturn(Optional.of(view));

        // Act
        Optional<OrderCompleteView> result = sut.findOrderCompleteView("  ORD20260414-000001  ");

        // Assert
        assertThat(result).contains(view);
        verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260414-000001");
    }

    // ─── findMemberOrderHistories ────────────────────────────────────────

    @Test
    void findMemberOrderHistories_page_zero_is_normalized_to_1() {
        // Arrange
        MemberOrderHistoryPage page = new MemberOrderHistoryPage(List.of(), 0L, 1, 10);
        when(orderRepository.findMemberOrders(anyLong(), anyInt(), anyInt())).thenReturn(page);

        // Act
        sut.findMemberOrderHistories(1L, 0);

        // Assert
        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_negative_page_is_normalized_to_1() {
        // Arrange
        MemberOrderHistoryPage page = new MemberOrderHistoryPage(List.of(), 0L, 1, 10);
        when(orderRepository.findMemberOrders(anyLong(), anyInt(), anyInt())).thenReturn(page);

        // Act
        sut.findMemberOrderHistories(1L, -3);

        // Assert
        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    // ─── findMemberOrderDetail ───────────────────────────────────────────

    @Test
    void findMemberOrderDetail_blank_order_number_returns_empty() {
        // Act
        Optional<MemberOrderDetailView> result = sut.findMemberOrderDetail(1L, "  ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findMemberOrderDetail_valid_order_number_delegates_to_repository() {
        // Arrange
        when(orderRepository.findMemberOrderDetail(1L, "ORD20260414-000001"))
                .thenReturn(Optional.empty());

        // Act
        sut.findMemberOrderDetail(1L, "  ORD20260414-000001  ");

        // Assert
        verify(orderRepository).findMemberOrderDetail(1L, "ORD20260414-000001");
    }

    // ─── findReorderItems ────────────────────────────────────────────────

    @Test
    void findReorderItems_blank_order_number_returns_empty_list() {
        // Act
        List<OrderReorderItem> result = sut.findReorderItems(1L, "  ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findReorderItems_null_order_number_returns_empty_list() {
        // Act
        List<OrderReorderItem> result = sut.findReorderItems(1L, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findReorderItems_valid_order_number_trims_and_delegates_to_repository() {
        // Arrange
        when(orderRepository.findReorderItems(1L, "ORD20260414-000001")).thenReturn(List.of());

        // Act
        sut.findReorderItems(1L, "  ORD20260414-000001  ");

        // Assert
        verify(orderRepository).findReorderItems(1L, "ORD20260414-000001");
    }

    // ─── placeOrder ──────────────────────────────────────────────────────

    /** テスト用のカート明細1行を組み立てる。 */
    private CartLineView buildCartLine(int stockQuantity) {
        return new CartLineView(
                1L, 10L, "テスト商品", "PROD001", "ホワイト",
                BigDecimal.valueOf(5000), stockQuantity, false, BigDecimal.ZERO, false, 1,
                "/products/10"
        );
    }

    /** テスト用の有効なカートを組み立てる。 */
    private CartView buildCartWith(CartLineView line) {
        CartSummaryView summary = new CartSummaryView(
                BigDecimal.valueOf(5000), BigDecimal.ZERO,
                BigDecimal.valueOf(800), BigDecimal.valueOf(500), BigDecimal.valueOf(6300)
        );
        return new CartView(List.of(line), 1, 1, summary);
    }

    /** テスト用の個人向け有効フォームを組み立てる。 */
    private CheckoutInputForm buildValidForm(String deliveryFloor) {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("test@example.com");
        form.setDaytimePhone("0312345678");
        form.setPostalCodePart1("123");
        form.setPostalCodePart2("4567");
        form.setPrefecture("東京都");
        form.setCity("渋谷区");
        form.setAddressLine("渋谷1-1-1");
        form.setDeliveryFloor(deliveryFloor);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    @Test
    void placeOrder_corporate_without_company_name_throws_illegal_argument_exception() {
        // Arrange
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("3");
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName(null);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");

        // Act / Assert – validateBusinessRules で法人名必須エラー
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_stock_shortage_throws_illegal_argument_exception() {
        // Arrange – 在庫0のアイテムを持つカート
        CartView cart = buildCartWith(buildCartLine(0));
        CheckoutInputForm form = buildValidForm("3");
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");

        // Act / Assert – revalidateCartStock で在庫不足エラー
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_blank_delivery_floor_throws_illegal_argument_exception() {
        // Arrange – 在庫あり・deliveryFloor を空にする
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("");
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");

        // Act / Assert – parseFloor で空文字エラー
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_non_numeric_delivery_floor_throws_illegal_argument_exception() {
        // Arrange – 在庫あり・deliveryFloor を文字列にする
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("abc");
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");

        // Act / Assert – parseFloor で数値変換エラー
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_transaction_not_active_throws_illegal_state_exception() {
        // Arrange – 全バリデーション通過後に TransactionSyncManager が inactive
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("3");
        org.mockito.Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");
        org.mockito.Mockito.lenient().when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        org.mockito.Mockito.lenient().when(orderRepository.insertOrder(any())).thenReturn(1L);

        // Act / Assert – sendOrderCompleteMailAfterCommit で IllegalStateException
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeOrder_after_commit_calls_notification_mail_service() {
        // Arrange
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("3");
        org.mockito.Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("フラッシュ");
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        ArgumentCaptor<TransactionSynchronization> syncCaptor =
                ArgumentCaptor.forClass(TransactionSynchronization.class);

        try (MockedStatic<TransactionSynchronizationManager> mockedTsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            mockedTsm.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            // Act
            String orderNumber = sut.placeOrder(1L, form, cart);
            assertThat(orderNumber).isNotBlank();

            // afterCommit コールバックを取り出して手動実行
            mockedTsm.verify(() ->
                    TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            syncCaptor.getValue().afterCommit();
        }

        // Assert – afterCommit() が notificationMailService を呼んでいる
        verify(notificationMailService).sendOrderCompleteMail(any(OrderCompleteMailPayload.class));
    }

    @Test
    void placeOrder_empty_cart_throws_illegal_argument_exception() {
        // Arrange – 空カート（isEmpty() == true）
        CartSummaryView summary = new CartSummaryView(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        CartView emptyCart = new CartView(List.of(), 0, 0, summary);
        org.mockito.Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");

        // Act / Assert
        assertThatThrownBy(() -> sut.placeOrder(1L, buildValidForm("3"), emptyCart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_convenience_store_payment_generates_payment_instruction_json() throws Exception {
        // Arrange – paymentMethod を convenience_store に設定、組立費ありカートで L209 もカバー
        CartLineView lineWithAssembly = new CartLineView(
                1L, 10L, "テスト商品", "PROD001", "ホワイト",
                BigDecimal.valueOf(5000), 5, true, BigDecimal.valueOf(2000), true, 1,
                "/products/10"
        );
        CartView cart = buildCartWith(lineWithAssembly);
        CheckoutInputForm form = buildValidForm("3");
        form.setPaymentMethod("convenience_store");
        org.mockito.Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("フラッシュ");
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payment_number\":\"123456\"}");
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        try (MockedStatic<TransactionSynchronizationManager> mockedTsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            mockedTsm.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            // Act
            String orderNumber = sut.placeOrder(1L, form, cart);
            assertThat(orderNumber).isNotBlank();
        }
    }

    @Test
    void placeOrder_convenience_store_objectmapper_exception_wraps_in_illegal_state() throws Exception {
        // Arrange – objectMapper.writeValueAsString が例外スロー → L385-390 catchブランチ
        CartView cart = buildCartWith(buildCartLine(5));
        CheckoutInputForm form = buildValidForm("3");
        form.setPaymentMethod("convenience_store");
        org.mockito.Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("エラー");
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("serialize error") {});

        // Act / Assert
        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalStateException.class);
    }
}
