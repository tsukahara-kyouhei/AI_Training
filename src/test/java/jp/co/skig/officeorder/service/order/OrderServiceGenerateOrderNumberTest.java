package jp.co.skig.officeorder.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService} の {@code generateOrderNumber()} の単体テスト。
 *
 * <p>注文番号採番ロジック（フォーマット・上限チェック）を検証する。
 * {@code generateOrderNumber} は private メソッドのためリフレクションで呼び出す。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceGenerateOrderNumberTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationMailService notificationMailService;

    @Mock
    private MessageSource messageSource;

    /** テスト対象の固定日時: 2026-03-10T09:12:00+09:00 */
    private static final OffsetDateTime ORDER_DATETIME =
            OffsetDateTime.of(2026, 3, 10, 9, 12, 0, 0, ZoneOffset.ofHours(9));

    private OrderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-10T00:12:00Z"), ZoneOffset.UTC);
        service = new OrderService(orderRepository, objectMapper, notificationMailService, clock, messageSource);
    }

    /**
     * リフレクションで private {@code generateOrderNumber} を呼び出すヘルパ。
     */
    private String invokeGenerateOrderNumber(OffsetDateTime orderDatetime) throws Exception {
        Method method = OrderService.class.getDeclaredMethod("generateOrderNumber", OffsetDateTime.class);
        method.setAccessible(true);
        return (String) method.invoke(service, orderDatetime);
    }

    // ---- GN-01: 連番 1（初日初回）→ 6桁ゼロ埋め ----
    @Test
    void gn01_sequence1_formatsWithLeadingZeros() throws Exception {
        when(orderRepository.nextOrderSequence(ORDER_DATETIME.toLocalDate())).thenReturn(1);

        String orderNumber = invokeGenerateOrderNumber(ORDER_DATETIME);

        assertThat(orderNumber).isEqualTo("ORD20260310-000001");
    }

    // ---- GN-02: 連番 100 → 桁数確認 ----
    @Test
    void gn02_sequence100_formatsCorrectly() throws Exception {
        when(orderRepository.nextOrderSequence(ORDER_DATETIME.toLocalDate())).thenReturn(100);

        String orderNumber = invokeGenerateOrderNumber(ORDER_DATETIME);

        assertThat(orderNumber).isEqualTo("ORD20260310-000100");
    }

    // ---- GN-03: 連番 999999（上限値）→ 採番成功 ----
    @Test
    void gn03_sequenceAtMaxBoundary_succeeds() throws Exception {
        when(orderRepository.nextOrderSequence(ORDER_DATETIME.toLocalDate())).thenReturn(999_999);

        String orderNumber = invokeGenerateOrderNumber(ORDER_DATETIME);

        assertThat(orderNumber).isEqualTo("ORD20260310-999999");
    }

    // ---- GN-04: 連番 1000000（上限超過）→ IllegalStateException ----
    @Test
    void gn04_sequenceOverMax_throwsIllegalStateException() {
        when(orderRepository.nextOrderSequence(ORDER_DATETIME.toLocalDate())).thenReturn(1_000_000);

        assertThatThrownBy(() -> invokeGenerateOrderNumber(ORDER_DATETIME))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("上限");
    }
}
