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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * OrderService の注文番号採番ロジック（generateOrderNumber）に関するテスト。
 *
 * <p>generateOrderNumber は private メソッドのため placeOrder() 経由で検証する。
 * BUG-001 の根本対策（シードSQL修正）後も、採番ロジック自体が仕様通りに
 * 動作することを継続して保証するために追加。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceGenerateOrderNumberTest {

    /** 固定日時: 2026-03-10 09:12:00+09:00 */
    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-03-10T00:12:00Z"); // UTC: 00:12 = JST 09:12

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
        // placeOrder は @Transactional だが単体テストでは Spring が管理しないため、
        // sendOrderCompleteMailAfterCommit 内の isSynchronizationActive() を通過させるため手動で初期化する
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void placeOrder_連番1を採番したとき_ORD20260310_000001が返却されること() {
        when(orderRepository.nextOrderSequence(LocalDate.of(2026, 3, 10))).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = sut.placeOrder(null, validForm(), singleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260310-000001");
    }

    @Test
    void placeOrder_連番42を採番したとき_6桁ゼロ埋めされること() {
        when(orderRepository.nextOrderSequence(LocalDate.of(2026, 3, 10))).thenReturn(42);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(2L);

        String orderNumber = sut.placeOrder(null, validForm(), singleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260310-000042");
    }

    @Test
    void placeOrder_連番999999を採番したとき_上限値の注文番号が返却されること() {
        when(orderRepository.nextOrderSequence(LocalDate.of(2026, 3, 10))).thenReturn(999_999);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(3L);

        String orderNumber = sut.placeOrder(null, validForm(), singleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260310-999999");
    }

    @Test
    void placeOrder_連番が1000000のとき_日次連番上限超過でIllegalStateExceptionがスローされること() {
        // generateOrderNumber が exception を投げるため insertOrder は呼ばれない
        when(orderRepository.nextOrderSequence(LocalDate.of(2026, 3, 10))).thenReturn(1_000_000);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        assertThatThrownBy(() -> sut.placeOrder(null, validForm(), singleItemCart()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("日次連番が上限を超過");
    }

    @Test
    void placeOrder_Clockが返す日付が注文番号の日付部に使われること() {
        // 別日付の Clock でインスタンスを生成し、日付部が Clock 依存であることを検証
        Clock anotherClock = Clock.fixed(
                Instant.parse("2025-01-15T01:00:00Z"), // JST: 2025-01-15 10:00
                JST
        );
        OrderService anotherSut = new OrderService(
                orderRepository, objectMapper, notificationMailService, anotherClock, messageSource
        );
        when(orderRepository.nextOrderSequence(LocalDate.of(2025, 1, 15))).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(4L);

        String orderNumber = anotherSut.placeOrder(null, validForm(), singleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20250115-000001");
    }

    // ───── ヘルパ ─────

    private static CheckoutInputForm validForm() {
        CheckoutInputForm f = new CheckoutInputForm();
        f.setPersonalOrCorporate("personal");
        f.setLastName("田中");
        f.setFirstName("太郎");
        f.setLastNameKana("タナカ");
        f.setFirstNameKana("タロウ");
        f.setEmail("test@example.com");
        f.setDaytimePhone("0312345678");
        f.setPostalCodePart1("100");
        f.setPostalCodePart2("0001");
        f.setPrefecture("東京都");
        f.setCity("千代田区");
        f.setAddressLine("サンプル町1-1-1");
        f.setDeliveryFloor("1");
        f.setHasElevator(Boolean.TRUE);
        f.setPaymentMethod("bank_transfer");
        return f;
    }

    private static CartView singleItemCart() {
        CartLineView line = new CartLineView(
                1L, 100L, "テスト商品", "P0001-C01", "ホワイト",
                BigDecimal.valueOf(10_000), 10, false, BigDecimal.ZERO, false, 1,
                "/products/100"
        );
        CartSummaryView summary = new CartSummaryView(
                BigDecimal.valueOf(10_000), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(1_000), BigDecimal.valueOf(11_000)
        );
        return new CartView(List.of(line), 1, 1, summary);
    }
}
