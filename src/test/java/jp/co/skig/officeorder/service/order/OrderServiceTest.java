package jp.co.skig.officeorder.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * OrderService の単体テスト。
 *
 * <p>注文番号採番ロジック（{@code generateOrderNumber}）を中心に検証する。
 * BUG-001 対応として、シード初期化後の採番が 101 以降で始まることや、
 * 最大連番超過時の例外を確認する。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    /** 固定日付：BUG-001 障害が発生した日。 */
    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 3, 30);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationMailService notificationMailService;

    @Mock
    private MessageSource messageSource;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 注文日時を 2026-03-30T00:00:00Z で固定
        Clock fixedClock = Clock.fixed(
                ORDER_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);

        // MessageSource のスタブ：コードをそのまま返す（業務バリデーションで使用）
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0, String.class));

        orderService = new OrderService(
                orderRepository,
                new ObjectMapper(),
                notificationMailService,
                fixedClock,
                messageSource);

        // @Transactional 環境を模倣するため手動で初期化する
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // -----------------------------------------------------------------------
    // BUG-001 関連テスト：generateOrderNumber の採番ロジック検証
    // -----------------------------------------------------------------------

    /**
     * BUG-001 シナリオ：シード初期化後、counters が last_sequence=100 で
     * 初期化されているため、最初の採番は 101 になる。
     * 生成される注文番号が ORD20260330-000101 であることを確認する。
     */
    @Test
    void placeOrder_returnsOrderNumber000101_whenSequenceIsInitializedAfterSeed() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(101);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(null, buildValidForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-000101");
    }

    /**
     * 通常シナリオ：counters が空（last_sequence なし）のとき最初の採番は 1。
     * 生成される注文番号が ORD20260330-000001 であることを確認する。
     *
     * <p>BUG-001 ではシードが counters を初期化していないため、このシナリオで
     * UNIQUE 制約違反が発生していた。シードへの修正によりこの採番は新規 DB
     * 初日以外では起きなくなる。
     */
    @Test
    void placeOrder_returnsOrderNumber000001_whenCountersIsEmpty() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(null, buildValidForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-000001");
    }

    /**
     * 連番最大値（999999）で注文番号が ORD20260330-999999 になることを確認する。
     * 最大値ちょうどは正常系の境界値である。
     */
    @Test
    void placeOrder_returnsOrderNumber999999_whenSequenceIsAtMax() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(999_999);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(null, buildValidForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-999999");
    }

    /**
     * 連番が日次上限（999999）を超過した場合、IllegalStateException がスローされることを確認する。
     */
    @Test
    void placeOrder_throwsIllegalStateException_whenSequenceExceedsMax() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1_000_000);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), buildSingleItemCart()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("日次連番が上限を超過");
    }

    // -----------------------------------------------------------------------
    // 業務バリデーション：入力チェックの基本動作
    // -----------------------------------------------------------------------

    /**
     * 空カートで注文確定しようとした場合、IllegalArgumentException がスローされることを確認する。
     */
    @Test
    void placeOrder_throwsIllegalArgumentException_whenCartIsEmpty() {
        CartView emptyCart = new CartView(List.of(), 0, 0, null);

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), emptyCart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * null カートで注文確定しようとした場合、IllegalArgumentException がスローされることを確認する。
     */
    @Test
    void placeOrder_throwsIllegalArgumentException_whenCartIsNull() {
        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 在庫数を超える数量が注文された場合、IllegalArgumentException がスローされることを確認する。
     */
    @Test
    void placeOrder_throwsIllegalArgumentException_whenQuantityExceedsStock() {
        // 在庫1に対して数量2を注文
        CartLineView overStockLine = new CartLineView(
                1L, 1L, "テスト商品", "P0001-C01", "ブラック",
                BigDecimal.valueOf(10_000), 1, false, BigDecimal.ZERO, false, 2, "/products/1");
        CartSummaryView summary = new CartSummaryView(
                BigDecimal.valueOf(20_000), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(2_000), BigDecimal.valueOf(22_000));
        CartView overStockCart = new CartView(List.of(overStockLine), 1, 2, summary);

        assertThatThrownBy(() -> orderService.placeOrder(null, buildValidForm(), overStockCart))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 法人区分で会社名が空の場合、IllegalArgumentException がスローされることを確認する。
     */
    @Test
    void placeOrder_throwsIllegalArgumentException_whenCorporateWithoutCompanyName() {
        CheckoutInputForm corporateForm = buildValidForm();
        corporateForm.setPersonalOrCorporate("corporate");
        corporateForm.setCompanyName(null);

        assertThatThrownBy(() -> orderService.placeOrder(null, corporateForm, buildSingleItemCart()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // 正常系：ゲスト注文・会員注文・法人注文
    // -----------------------------------------------------------------------

    /**
     * ゲスト注文時（memberId=null）の注文が成功し、注文番号が返ることを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("ゲスト注文時（memberId=null）の注文が成功する")
    void placeOrder_returnsOrderNumber_whenGuestOrder() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(null, buildValidForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-000001");
    }

    /**
     * 会員注文時（memberId あり）の注文が成功し、注文番号が返ることを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("会員注文時（memberId あり）の注文が成功する")
    void placeOrder_returnsOrderNumber_whenMemberOrder() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(42L, buildValidForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-000001");
    }

    /**
     * 法人区分で会社名あり・部署名ありの注文が成功することを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("法人区分で会社名あり・部署名ありの注文が成功する")
    void placeOrder_returnsOrderNumber_whenCorporateWithCompanyAndDepartment() {
        when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1);
        when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(orderRepository.insertOrder(any())).thenReturn(1L);

        String orderNumber = orderService.placeOrder(null, buildValidCorporateForm(), buildSingleItemCart());

        assertThat(orderNumber).isEqualTo("ORD20260330-000001");
    }

    // -----------------------------------------------------------------------
    // 正常系：createInitialForm
    // -----------------------------------------------------------------------

    /**
     * ゲスト（member=empty）の場合、空フォームが返ることを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("createInitialForm でゲスト時は空フォームが返る")
    void createInitialForm_returnsEmptyForm_whenGuestUser() {
        CheckoutInputForm form = orderService.createInitialForm(java.util.Optional.empty());

        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    /**
     * 7桁郵便番号（例 "1000001"）が part1（"100"）と part2（"0001"）に分割されてフォームへ反映される。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("createInitialForm で7桁郵便番号がパート1・2に分割されてフォームへ反映される")
    void createInitialForm_splitsPostalCodeIntoParts_whenSevenDigitCodeProvided() {
        var memberSessionUser = new jp.co.skig.officeorder.model.member.MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        var prefill = new jp.co.skig.officeorder.model.order.CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ", null, null,
                "test@example.com", "0312345678", null,
                "1000001", "東京都", "千代田区", "一番町1", 1, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(java.util.Optional.of(prefill));

        CheckoutInputForm form = orderService.createInitialForm(java.util.Optional.of(memberSessionUser));

        assertThat(form.getPostalCodePart1()).isEqualTo("100");
        assertThat(form.getPostalCodePart2()).isEqualTo("0001");
    }

    /**
     * 会員情報がフォームへ正しく反映されることを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("createInitialForm で会員の既存情報がフォームへ反映される")
    void createInitialForm_populatesFormFromMemberInfo() {
        var memberSessionUser = new jp.co.skig.officeorder.model.member.MemberSessionUser(2L, "member@example.com", "鈴木", "花子");
        var prefill = new jp.co.skig.officeorder.model.order.CheckoutMemberPrefill(
                "personal", "鈴木", "花子", "スズキ", "ハナコ", null, null,
                "member@example.com", "0398765432", null,
                null, "大阪府", "大阪市", "梅田1-1", null, false
        );
        when(orderRepository.findCheckoutMemberPrefill(2L)).thenReturn(java.util.Optional.of(prefill));

        CheckoutInputForm form = orderService.createInitialForm(java.util.Optional.of(memberSessionUser));

        assertThat(form.getLastName()).isEqualTo("鈴木");
        assertThat(form.getFirstName()).isEqualTo("花子");
        assertThat(form.getEmail()).isEqualTo("member@example.com");
        assertThat(form.getPrefecture()).isEqualTo("大阪府");
    }

    // -----------------------------------------------------------------------
    // 正常系：購入履歴取得
    // -----------------------------------------------------------------------

    /**
     * 購入履歴一覧が会員IDとページ番号を渡して取得できることを確認する。
     */
    @Test
    @org.junit.jupiter.api.DisplayName("購入履歴一覧が会員IDとページ番号を渡して取得できる")
    void findMemberOrderHistories_delegatesWithMemberIdAndPage() {
        var expectedPage = new jp.co.skig.officeorder.model.member.MemberOrderHistoryPage(List.of(), 0, 1, 10);
        when(orderRepository.findMemberOrders(10L, 1, 10)).thenReturn(expectedPage);

        var result = orderService.findMemberOrderHistories(10L, 1);

        assertThat(result).isEqualTo(expectedPage);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    /**
     * 全必須フィールドが設定された有効な注文フォームを返す。
     */
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
        form.setAddressLine("一番町1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    /**
     * 在庫充分・単一明細のカートを返す。
     */
    private CartView buildSingleItemCart() {
        CartLineView line = new CartLineView(
                1L, 1L, "テスト商品", "P0001-C01", "ブラック",
                BigDecimal.valueOf(10_000), 10, false, BigDecimal.ZERO, false, 1, "/products/1");
        CartSummaryView summary = new CartSummaryView(
                BigDecimal.valueOf(10_000), BigDecimal.ZERO,
                BigDecimal.valueOf(800), BigDecimal.valueOf(1_080), BigDecimal.valueOf(11_880));
        return new CartView(List.of(line), 1, 1, summary);
    }

    /**
     * 会員IDが指定された注文フォームを返す（会員注文のテスト用）。
     * 法人区分で注文し会社名も設定する。
     */
    @SuppressWarnings("unused")
    private CheckoutInputForm buildValidCorporateForm() {
        CheckoutInputForm form = buildValidForm();
        form.setPersonalOrCorporate("corporate");
        form.setCompanyName("株式会社テスト");
        form.setDepartmentName("総務部");
        return form;
    }
}
