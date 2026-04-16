package jp.co.skig.officeorder.service.order;

import jp.co.skig.officeorder.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService#generateOrderNumber} の単体テスト。
 *
 * <p>BUG-001 対応として、シードデータが当日日付で 100 件挿入された後でも
 * 採番ロジックが collision を起こさないことを確認する。
 * SQL 修正（OrderMapper.xml の nextOrderSequence）により リポジトリ層が
 * 適切な連番（101〜）を返すことを前提に、Service 層の変換ロジックを検証する。
 */
class OrderServiceGenerateOrderNumberTest {

    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(
                orderRepository,
                mock(com.fasterxml.jackson.databind.ObjectMapper.class),
                mock(jp.co.skig.officeorder.service.mail.NotificationMailService.class),
                Clock.systemDefaultZone(),
                mock(org.springframework.context.MessageSource.class)
        );
    }

    // ========================
    // 正常系
    // ========================

    @Test
    void generateOrderNumber_sequence1_returnsFormattedNumber() throws Exception {
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(1);

        String result = invokeGenerateOrderNumber(OffsetDateTime.parse("2026-04-16T10:00:00+09:00"));

        assertThat(result).isEqualTo("ORD20260416-000001");
    }

    @Test
    void generateOrderNumber_sequence101_returnsFormattedNumber() throws Exception {
        // BUG-001 シナリオ: シードデータが 1〜100 を占有後、SQL 修正により 101 が返される
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(101);

        String result = invokeGenerateOrderNumber(OffsetDateTime.parse("2026-04-16T10:00:00+09:00"));

        assertThat(result).isEqualTo("ORD20260416-000101");
    }

    @Test
    void generateOrderNumber_maxSequence_returnsFormattedNumber() throws Exception {
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(999_999);

        String result = invokeGenerateOrderNumber(OffsetDateTime.parse("2026-04-16T10:00:00+09:00"));

        assertThat(result).isEqualTo("ORD20260416-999999");
    }

    @Test
    void generateOrderNumber_differentDate_usesDateFromDatetime() throws Exception {
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(1);

        String result = invokeGenerateOrderNumber(OffsetDateTime.parse("2025-12-31T23:59:59+09:00"));

        assertThat(result).isEqualTo("ORD20251231-000001");
    }

    // ========================
    // 異常系
    // ========================

    @Test
    void generateOrderNumber_sequenceExceedsMax_throwsIllegalState() {
        when(orderRepository.nextOrderSequence(any(LocalDate.class))).thenReturn(1_000_000);

        assertThatThrownBy(() -> invokeGenerateOrderNumber(
                OffsetDateTime.parse("2026-04-16T10:00:00+09:00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("注文番号の日次連番が上限を超過しました");
    }

    // ========================
    // ヘルパー
    // ========================

    /** リフレクションで private メソッド generateOrderNumber を呼び出す。 */
    private String invokeGenerateOrderNumber(OffsetDateTime orderDatetime) throws Exception {
        Method method = OrderService.class.getDeclaredMethod("generateOrderNumber", OffsetDateTime.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(orderService, orderDatetime);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        }
    }
}
