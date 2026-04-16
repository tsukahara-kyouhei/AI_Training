package jp.co.skig.officeorder.service.order;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService} の単体テスト（generateOrderNumber 以外の公開メソッド）。
 *
 * <p>placeOrder は @Transactional と TransactionSynchronizationManager への依存があるため
 * 統合テストで検証する。
 */
class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderService sut;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("error");
        sut = new OrderService(
                orderRepository,
                new ObjectMapper(),
                mock(NotificationMailService.class),
                Clock.systemDefaultZone(),
                messageSource
        );
    }

    // --- createInitialForm ---

    @Test
    @DisplayName("ゲスト（会員なし）の場合は空のフォームを返す")
    void createInitialForm_noMember_returnsEmptyForm() {
        CheckoutInputForm form = sut.createInitialForm(Optional.empty());

        assertThat(form).isNotNull();
        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    @Test
    @DisplayName("会員プリフィルがある場合はフォームに会員情報をセットする")
    void createInitialForm_memberFoundPrefill_fillsForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com",
                "03-1234-5678", null, "1234567",
                "東京都", "新宿区", "新宿1-1-1", 1, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isEqualTo("山田");
        assertThat(form.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("郵便番号が 7 桁のとき前 3 桁と後 4 桁に分割してセットする")
    void createInitialForm_postalCode7Digits_splitIntoTwoParts() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com",
                null, null, "1234567",
                "東京都", "新宿区", "新宿1-1-1", 1, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getPostalCodePart1()).isEqualTo("123");
        assertThat(form.getPostalCodePart2()).isEqualTo("4567");
    }

    // --- findOrderCompleteView ---

    @Test
    @DisplayName("null の注文番号は empty を返す")
    void findOrderCompleteView_nullOrderNumber_returnsEmpty() {
        assertThat(sut.findOrderCompleteView(null)).isEmpty();
    }

    @Test
    @DisplayName("空の注文番号は empty を返す")
    void findOrderCompleteView_emptyOrderNumber_returnsEmpty() {
        assertThat(sut.findOrderCompleteView("   ")).isEmpty();
    }

    @Test
    @DisplayName("有効な注文番号はリポジトリに trim した値を渡して委譲する")
    void findOrderCompleteView_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20260417-000001"))
                .thenReturn(Optional.of(mock(OrderCompleteView.class)));

        sut.findOrderCompleteView("  ORD20260417-000001  ");

        verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260417-000001");
    }

    // --- findMemberOrderHistories ---

    @Test
    @DisplayName("page が 0 のとき 1 に正規化してリポジトリに渡す")
    void findMemberOrderHistories_pageZero_normalizesToOne() {
        when(orderRepository.findMemberOrders(1L, 1, 10)).thenReturn(mock(MemberOrderHistoryPage.class));

        sut.findMemberOrderHistories(1L, 0);

        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    @Test
    @DisplayName("page が有効のとき PAGE_SIZE=10 でリポジトリに渡す")
    void findMemberOrderHistories_validPage_usesPageSize10() {
        when(orderRepository.findMemberOrders(1L, 3, 10)).thenReturn(mock(MemberOrderHistoryPage.class));

        sut.findMemberOrderHistories(1L, 3);

        verify(orderRepository).findMemberOrders(1L, 3, 10);
    }

    // --- findMemberOrderDetail ---

    @Test
    @DisplayName("空の注文番号は empty を返す")
    void findMemberOrderDetail_emptyOrderNumber_returnsEmpty() {
        assertThat(sut.findMemberOrderDetail(1L, "")).isEmpty();
    }

    @Test
    @DisplayName("有効な注文番号はリポジトリに委譲する")
    void findMemberOrderDetail_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findMemberOrderDetail(1L, "ORD20260417-000001"))
                .thenReturn(Optional.of(mock(MemberOrderDetailView.class)));

        sut.findMemberOrderDetail(1L, "  ORD20260417-000001  ");

        verify(orderRepository).findMemberOrderDetail(1L, "ORD20260417-000001");
    }

    // --- findReorderItems ---

    @Test
    @DisplayName("空の注文番号は空リストを返す")
    void findReorderItems_emptyOrderNumber_returnsEmptyList() {
        assertThat(sut.findReorderItems(1L, "")).isEmpty();
    }

    @Test
    @DisplayName("null の注文番号は空リストを返す")
    void findReorderItems_nullOrderNumber_returnsEmptyList() {
        assertThat(sut.findReorderItems(1L, null)).isEmpty();
    }

    @Test
    @DisplayName("有効な注文番号はリポジトリに trim した値を渡して委譲する")
    void findReorderItems_validOrderNumber_delegatesToRepository() {
        when(orderRepository.findReorderItems(1L, "ORD20260417-000001")).thenReturn(java.util.List.of());

        sut.findReorderItems(1L, "  ORD20260417-000001  ");

        verify(orderRepository).findReorderItems(1L, "ORD20260417-000001");
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    @DisplayName("findOrderCompleteView の呼び出しは 50ms 以内に完了する")
    void findOrderCompleteView_completesWithinTimeLimit() {
        when(orderRepository.findOrderCompleteByOrderNumber(anyString())).thenReturn(Optional.empty());
        sut.findOrderCompleteView("ORD20260417-000001");
    }
}
