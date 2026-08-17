package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationMailService notificationMailService;

    @Mock
    private MessageSource messageSource;

    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fixedClock = Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Tokyo"));

        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService = new OrderService(
                orderRepository,
                objectMapper,
                notificationMailService,
                fixedClock,
                messageSource
        );
    }

    @Nested
    @DisplayName("createInitialForm メソッドのテスト")
    class CreateInitialFormTest {

        @Test
        @DisplayName("ゲストの場合は空のフォームが返されること")
        void guestUser_returnsEmptyForm() {
            CheckoutInputForm form = orderService.createInitialForm(Optional.empty());

            assertThat(form).isNotNull();
            assertThat(form.getLastName()).isNull();
        }

        @Test
        @DisplayName("会員情報が存在する場合、フォームに初期値がプレフィルされること")
        void memberExists_prefillsForm() {
            // MemberSessionUser をモック化して指定
            MemberSessionUser sessionUser = mock(MemberSessionUser.class);
            when(sessionUser.memberId()).thenReturn(1L);

            CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                    "personal", "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "test@example.com", "09012345678", null,
                    "1234567", "東京都", "新宿区", "西新宿1-1-1", 3, true
            );
            when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

            CheckoutInputForm form = orderService.createInitialForm(Optional.of(sessionUser));

            assertThat(form.getLastName()).isEqualTo("山田");
            assertThat(form.getPostalCodePart1()).isEqualTo("123");
            assertThat(form.getPostalCodePart2()).isEqualTo("4567");
            assertThat(form.getDeliveryFloor()).isEqualTo("3");
        }
    }

    @Nested
@DisplayName("placeOrder メソッドのテスト")
class PlaceOrderTest {

    @Test
    @DisplayName("カートが空の場合は例外が発生すること")
    void emptyCart_throwsException() {
        CartView emptyCart = mock(CartView.class);
        // 不要な stub (items()) を除去し、isEmpty のみ定義
        when(emptyCart.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> orderService.placeOrder(1L, new CheckoutInputForm(), emptyCart))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business.order.cartEmpty");
    }

    @Test
    @DisplayName("正常に注文が確定し、DB保存とメール送信フックが正しく実行されること")
    void validOrder_placesOrderSuccessfully() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            CheckoutInputForm rawForm = new CheckoutInputForm();
            rawForm.setPersonalOrCorporate("personal");
            rawForm.setLastName("山田");
            rawForm.setFirstName("太郎");
            rawForm.setEmail("test@example.com");
            rawForm.setDeliveryFloor("2");
            rawForm.setPaymentMethod("credit_card");

            CartLineView line = mock(CartLineView.class);

// ID・数量・在庫などの設定
when(line.productCode()).thenReturn("PROD-001");
when(line.quantity()).thenReturn(1);
when(line.stockQuantity()).thenReturn(10);
when(line.unitPrice()).thenReturn(BigDecimal.valueOf(1000));

// ★ここを修正（OrderService 205行目で呼ばれているメソッド名に合わせる）
when(line.lineSubtotalBeforeTax()).thenReturn(BigDecimal.valueOf(1000));

            CartSummaryView summary = mock(CartSummaryView.class);
            CartView cart = mock(CartView.class);

            when(cart.items()).thenReturn(List.of(line));
            when(cart.summary()).thenReturn(summary);
            when(cart.isEmpty()).thenReturn(false);

            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.valueOf(10));
            when(orderRepository.nextOrderSequence(any())).thenReturn(1);
            when(orderRepository.insertOrder(any())).thenReturn(100L);

            String orderNumber = orderService.placeOrder(1L, rawForm, cart);

            assertThat(orderNumber).isEqualTo("ORD20260817-000001");
            verify(orderRepository).insertOrder(any());
            verify(orderRepository).insertOrderItem(any());
            verify(orderRepository).insertOrderStatusHistory(any());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.get(0).afterCommit();

            verify(notificationMailService).sendOrderCompleteMail(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

    @Nested
    @DisplayName("参照系メソッドのテスト")
    class QueryMethodsTest {

        @Test
        @DisplayName("findOrderCompleteView: 注文番号が空の場合は Optional.empty が返ること")
        void findOrderCompleteView_blankOrderNumber_returnsEmpty() {
            assertThat(orderService.findOrderCompleteView("")).isEmpty();
            assertThat(orderService.findOrderCompleteView(null)).isEmpty();
        }

        @Test
        @DisplayName("findOrderCompleteView: 正常な注文番号で取得できること")
        void findOrderCompleteView_validOrderNumber_returnsView() {
            OrderCompleteView mockView = mock(OrderCompleteView.class);
            when(orderRepository.findOrderCompleteByOrderNumber("ORD20260817-000001")).thenReturn(Optional.of(mockView));

            Optional<OrderCompleteView> result = orderService.findOrderCompleteView(" ORD20260817-000001 ");

            assertThat(result).contains(mockView);
        }

        @Test
        @DisplayName("findMemberOrderHistories: ページ数が1未満の場合は1ページ目に正規化されること")
        void findMemberOrderHistories_normalizesPage() {
            MemberOrderHistoryPage mockPage = mock(MemberOrderHistoryPage.class);
            when(orderRepository.findMemberOrders(1L, 1, 10)).thenReturn(mockPage);

            MemberOrderHistoryPage result = orderService.findMemberOrderHistories(1L, 0);

            assertThat(result).isEqualTo(mockPage);
            verify(orderRepository).findMemberOrders(1L, 1, 10);
        }

        @Test
        @DisplayName("findReorderItems: 注文番号が空の場合は空リストが返ること")
        void findReorderItems_blankOrderNumber_returnsEmptyList() {
            assertThat(orderService.findReorderItems(1L, " ")).isEmpty();
        }
    }
}
