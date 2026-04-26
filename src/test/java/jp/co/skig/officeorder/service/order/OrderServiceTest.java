package jp.co.skig.officeorder.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService} の単体テスト。
 *
 * <p>注意: {@link OrderService#placeOrder} は {@code @Transactional} かつ
 * {@code TransactionSynchronizationManager} を使うため、単体テストでは
 * トランザクション同期が無効な状態での例外ケースのみ検証する。
 * 正常系は結合テスト (MapperTest) で検証する。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    NotificationMailService notificationMailService;

    Clock clock = Clock.fixed(Instant.parse("2025-04-01T09:00:00Z"), ZoneOffset.UTC);

    OrderService sut;

    @BeforeEach
    void setUp() {
        StaticMessageSource ms = new StaticMessageSource();
        ms.addMessage("business.order.cartEmpty", java.util.Locale.JAPAN, "カートが空です。");
        ms.addMessage("business.order.companyRequired", java.util.Locale.JAPAN, "法人の場合は会社名を入力してください。");
        ms.addMessage("business.stockShortage", java.util.Locale.JAPAN, "在庫が不足しています。");
        ms.addMessage("business.order.deliveryFloorRequired", java.util.Locale.JAPAN, "階数を入力してください。");
        ms.addMessage("business.order.deliveryFloorInteger", java.util.Locale.JAPAN, "階数は整数で入力してください。");
        sut = new OrderService(orderRepository, new ObjectMapper(), notificationMailService, clock, ms);
    }

    // ── createInitialForm ─────────────────────────────────────────────────

    @Test
    void ゲストの場合は空のフォームが返ること() {
        CheckoutInputForm form = sut.createInitialForm(Optional.empty());

        assertThat(form).isNotNull();
        assertThat(form.getEmail()).isNull();
    }

    @Test
    void ログイン会員でプレフィルが存在しない場合は空のフォームが返ること() {
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.empty());

        CheckoutInputForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getEmail()).isNull();
    }

    @Test
    void ログイン会員でプレフィルが存在する場合はフォームに反映されること() {
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        var prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "1000001", "東京都", "千代田区", "千代田1-1", 1, Boolean.TRUE
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getEmail()).isEqualTo("test@example.com");
        assertThat(form.getLastName()).isEqualTo("山田");
        assertThat(form.getFirstName()).isEqualTo("太郎");
        assertThat(form.getPostalCodePart1()).isEqualTo("100");
        assertThat(form.getPostalCodePart2()).isEqualTo("0001");
    }

    @Test
    void 郵便番号が7桁以外の場合はPostalCodeが設定されないこと() {
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        var prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "123",  // 7桁以外
                "東京都", "千代田区", "千代田1-1", 1, Boolean.TRUE
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getPostalCodePart1()).isNull();
        assertThat(form.getPostalCodePart2()).isNull();
    }

    // ── generateOrderNumber ────────────────────────────────────────────

    /**
     * BUG-001 の根本原因に対する回帰テスト。
     *
     * <p>注文番号がアプリケーションの Clock に基づく日付と 6 桁連番で生成されることを検証する。
     * シードデータは固定日付 "ORD20000101-" を使うため、このロジックが正しく動作する限り
     * シードデータとの注文番号衝突は発生しない。
     */
    @Test
    void 注文番号がクロック日付と6桁連番で生成されること() {
        // clock は 2025-04-01T09:00:00Z に固定されているため日付部は "20250401"
        CheckoutInputForm form = buildValidForm();
        CartView cart = buildSingleItemCart(1, 10);

        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.nextOrderSequence(LocalDate.of(2025, 4, 1))).thenReturn(1);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            String orderNumber = sut.placeOrder(1L, form, cart);
            assertThat(orderNumber).isEqualTo("ORD20250401-000001");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 採番連番が上限を超えるとIllegalStateExceptionがスローされること() {
        CheckoutInputForm form = buildValidForm();
        CartView cart = buildSingleItemCart(1, 10);

        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.nextOrderSequence(any())).thenReturn(1_000_000);

        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("上限");
    }

    // ── placeOrder (異常系のみ) ───────────────────────────────────────────

    @Test
    void nullカートを渡すとIllegalArgumentExceptionがスローされること() {
        CheckoutInputForm form = buildValidForm();

        assertThatThrownBy(() -> sut.placeOrder(1L, form, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 空カートを渡すとIllegalArgumentExceptionがスローされること() {
        CheckoutInputForm form = buildValidForm();
        CartView emptyCart = new CartView(
                List.of(), 0, 0,
                new CartSummaryView(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        assertThatThrownBy(() -> sut.placeOrder(1L, form, emptyCart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 法人区分で会社名が空の場合はIllegalArgumentExceptionがスローされること() {
        CheckoutInputForm form = buildValidForm();
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName(null);  // 会社名なし
        CartView cart = buildSingleItemCart(5, 5);

        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 在庫不足の商品がある場合はIllegalArgumentExceptionがスローされること() {
        CheckoutInputForm form = buildValidForm();
        // quantity=5, stock=3 → 在庫不足
        CartView cart = buildSingleItemCart(5, 3);

        assertThatThrownBy(() -> sut.placeOrder(1L, form, cart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── findOrderCompleteView ─────────────────────────────────────────────

    @Test
    void 空文字の注文番号はemptyを返すこと() {
        assertThat(sut.findOrderCompleteView("")).isEmpty();
        assertThat(sut.findOrderCompleteView("  ")).isEmpty();
        assertThat(sut.findOrderCompleteView(null)).isEmpty();
    }

    @Test
    void 有効な注文番号でリポジトリが呼ばれること() {
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20250401-000001"))
                .thenReturn(Optional.empty());

        var result = sut.findOrderCompleteView("ORD20250401-000001");

        assertThat(result).isEmpty();
    }

    // ── findMemberOrderHistories ──────────────────────────────────────────

    @Test
    void ページ0以下は1に補正されてリポジトリに渡ること() {
        var page = new jp.co.skig.officeorder.model.member.MemberOrderHistoryPage(List.of(), 0L, 1, 10);
        when(orderRepository.findMemberOrders(1L, 1, 10)).thenReturn(page);

        sut.findMemberOrderHistories(1L, 0);

        org.mockito.Mockito.verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    // ── findMemberOrderDetail ─────────────────────────────────────────────

    @Test
    void 空の注文番号を渡すとemptyが返ること() {
        assertThat(sut.findMemberOrderDetail(1L, null)).isEmpty();
        assertThat(sut.findMemberOrderDetail(1L, "")).isEmpty();
    }

    // ── findReorderItems ──────────────────────────────────────────────────

    @Test
    void 空の注文番号を渡すと空リストが返ること() {
        assertThat(sut.findReorderItems(1L, null)).isEmpty();
        assertThat(sut.findReorderItems(1L, "")).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CheckoutInputForm buildValidForm() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("test@example.com");
        form.setDaytimePhone("0312345678");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("千代田1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(Boolean.TRUE);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    private CartView buildSingleItemCart(int quantity, int stock) {
        CartLineView line = new CartLineView(
                10L, 100L, "テスト商品", "P001", "白",
                new BigDecimal("1000"), stock, false,
                BigDecimal.ZERO, false, quantity, "/products/100"
        );
        CartSummaryView summary = new CartSummaryView(
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("800"),
                new BigDecimal("100"),
                new BigDecimal("1900")
        );
        return new CartView(List.of(line), 1, quantity, summary);
    }
}
