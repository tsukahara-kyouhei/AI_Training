package jp.co.skig.officeorder.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService} の単体テスト。
 *
 * <p>注文番号採番ロジック（{@code generateOrderNumber}）の仕様を検証する。
 * 採番は private メソッドのため、{@link OrderService#placeOrder} 経由で観察する。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // 2026-04-24T10:00:00+09:00（JST） に固定した Clock
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-24T01:00:00Z"),
            ZoneOffset.ofHours(9)
    );
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 4, 24);

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private NotificationMailService notificationMailService;
    @Mock
    private MessageSource messageSource;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Clock は引数で直接渡す（@InjectMocks では Clock の注入が難しいため手動生成）
        orderService = new OrderService(
                orderRepository, objectMapper, notificationMailService, FIXED_CLOCK, messageSource
        );
        // placeOrder 内の sendOrderCompleteMailAfterCommit が
        // TransactionSynchronizationManager.isSynchronizationActive() を呼ぶため初期化する
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ---- 注文番号フォーマット ----

    @Test
    void placeOrder_注文番号はORD日付6桁連番の形式になる() {
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.nextOrderSequence(FIXED_DATE)).thenReturn(1);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(1L, minimalForm(), minimalCart());

        assertThat(orderNumber).isEqualTo("ORD20260424-000001");
    }

    @Test
    void placeOrder_注文番号の連番は6桁ゼロパディングされる() {
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.nextOrderSequence(FIXED_DATE)).thenReturn(123);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(1L, minimalForm(), minimalCart());

        assertThat(orderNumber).isEqualTo("ORD20260424-000123");
    }

    @Test
    void placeOrder_注文番号の日付部はappClockの日付から生成される() {
        // Clock が 2026-04-24 → 日付部は "20260424" になること
        // （CURRENT_DATE など実行時の日付に依存しないことの確認）
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.nextOrderSequence(FIXED_DATE)).thenReturn(1);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(1L, minimalForm(), minimalCart());

        assertThat(orderNumber).startsWith("ORD20260424-");
    }

    // ---- 連番上限 ----

    @Test
    void placeOrder_連番が上限超過の場合はIllegalStateExceptionが発生する() {
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        // ORDER_NUMBER_MAX_SEQUENCE = 999_999 を超える値を返させる
        when(orderRepository.nextOrderSequence(FIXED_DATE)).thenReturn(1_000_000);

        assertThatThrownBy(() -> orderService.placeOrder(1L, minimalForm(), minimalCart()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("連番");
    }

    // ---- カート業務バリデーション ----

    @Test
    void placeOrder_nullカートはIllegalArgumentExceptionが発生する() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenReturn("エラー");

        assertThatThrownBy(() -> orderService.placeOrder(1L, minimalForm(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrder_空カートはIllegalArgumentExceptionが発生する() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenReturn("エラー");
        CartView emptyCart = new CartView(List.of(), 0, 0, minimalCart().summary());

        assertThatThrownBy(() -> orderService.placeOrder(1L, minimalForm(), emptyCart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- findOrderCompleteView 早期リターン ----

    @Test
    void findOrderCompleteView_nullはOptionalEmptyを返す() {
        assertThat(orderService.findOrderCompleteView(null)).isEmpty();
    }

    @Test
    void findOrderCompleteView_空文字はOptionalEmptyを返す() {
        assertThat(orderService.findOrderCompleteView("")).isEmpty();
    }

    @Test
    void findOrderCompleteView_空白のみはOptionalEmptyを返す() {
        assertThat(orderService.findOrderCompleteView("   ")).isEmpty();
    }

    // ---- findMemberOrderDetail 早期リターン ----

    @Test
    void findMemberOrderDetail_nullはOptionalEmptyを返す() {
        assertThat(orderService.findMemberOrderDetail(1L, null)).isEmpty();
    }

    @Test
    void findMemberOrderDetail_空文字はOptionalEmptyを返す() {
        assertThat(orderService.findMemberOrderDetail(1L, "")).isEmpty();
    }

    // ---- findReorderItems 早期リターン ----

    @Test
    void findReorderItems_nullは空リストを返す() {
        assertThat(orderService.findReorderItems(1L, null)).isEmpty();
    }

    @Test
    void findReorderItems_空文字は空リストを返す() {
        assertThat(orderService.findReorderItems(1L, "")).isEmpty();
    }

    // ---- findMemberOrderHistories ページ補正 ----

    @Test
    void findMemberOrderHistories_pageが0のとき1に補正される() {
        orderService.findMemberOrderHistories(1L, 0);
        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_pageが負のとき1に補正される() {
        orderService.findMemberOrderHistories(1L, -3);
        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    void findMemberOrderHistories_pageが正のときそのまま渡される() {
        orderService.findMemberOrderHistories(1L, 5);
        verify(orderRepository).findMemberOrders(1L, 5, 10);
    }

    // ---- ヘルパーメソッド ----

    private CheckoutInputForm minimalForm() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("テスト");
        form.setFirstName("太郎");
        form.setLastNameKana("テスト");
        form.setFirstNameKana("タロウ");
        form.setEmail("test@example.com");
        form.setDaytimePhone("0312345678");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    private CartView minimalCart() {
        CartLineView line = new CartLineView(
                1L, 1L, "テスト商品", "P0001-C01", "黒系",
                new BigDecimal("10000"), 10, false, BigDecimal.ZERO, false, 1, "/products/1"
        );
        CartSummaryView summary = new CartSummaryView(
                new BigDecimal("10000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("1000"), new BigDecimal("11000")
        );
        return new CartView(List.of(line), 1, 1, summary);
    }
}
