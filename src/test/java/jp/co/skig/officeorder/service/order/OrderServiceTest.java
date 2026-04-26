package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link OrderService} のユニットテスト。
 *
 * <p>
 * BUG-001「注文確定時のシステムエラー」の根本原因である
 * 注文番号採番ロジックを中心に検証する。
 */
class OrderServiceTest {

  /** 固定時刻: 2026-03-10T00:00:00Z（BUG-001 発生日に合わせる） */
  private static final Instant FIXED_INSTANT = Instant.parse("2026-03-10T00:00:00Z");
  /** 固定時刻から導出される注文日付。 */
  private static final LocalDate ORDER_DATE = LocalDate.of(2026, 3, 10);

  private OrderRepository orderRepository;
  private MessageSource messageSource;
  private OrderService service;

  @BeforeEach
  void setUp() {
    orderRepository = mock(OrderRepository.class);
    messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(any(String.class), any(Object[].class), any(Locale.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
    service = new OrderService(
        orderRepository,
        new ObjectMapper(),
        mock(NotificationMailService.class),
        fixedClock,
        messageSource);
  }

  // ============================================================
  // BUG-001: 注文番号採番ロジックの検証
  // ============================================================

  @Nested
  @DisplayName("generateOrderNumber — BUG-001 対応")
  class GenerateOrderNumber {

    @Test
    @DisplayName("sequence=1 のとき ORD20260310-000001 を返す（seed が当日生成する値と同じ形式）")
    void sequence1_producesExpectedFormat() {
      // seed が挿入する order_number と同じ形式になることを確認する。
      // BUG-001 の修正前は、この形式の値が seed に既に存在するため UNIQUE 制約違反が発生していた。
      setupForPlaceOrder(1);

      String orderNumber = placeOrderInMockTransaction(minimalForm(), singleItemCart());

      assertThat(orderNumber).isEqualTo("ORD20260310-000001");
    }

    @Test
    @DisplayName("sequence=101 のとき ORD20260310-000101 を返す（BUG-001 修正後の動作確認）")
    void sequence101_afterSeedFix_producesExpectedFormat() {
      // BUG-001 修正: seed が order_number_counters に last_sequence=100 を登録するため、
      // seed 当日の初回採番は 101 になる。seed の order_number（001〜100）と衝突しない。
      setupForPlaceOrder(101);

      String orderNumber = placeOrderInMockTransaction(minimalForm(), singleItemCart());

      assertThat(orderNumber).isEqualTo("ORD20260310-000101");
    }

    @Test
    @DisplayName("sequence=999_999（上限）のとき正常に採番できる")
    void sequenceAtMaxLimit_succeeds() {
      setupForPlaceOrder(999_999);

      String orderNumber = placeOrderInMockTransaction(minimalForm(), singleItemCart());

      assertThat(orderNumber).isEqualTo("ORD20260310-999999");
    }

    @Test
    @DisplayName("sequence=1_000_000（上限超過）のとき IllegalStateException")
    void sequenceOverMax_throwsIllegalStateException() {
      when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
      when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(1_000_000);
      when(orderRepository.insertOrder(any())).thenReturn(1L);

      try (MockedStatic<TransactionSynchronizationManager> tsm = Mockito
          .mockStatic(TransactionSynchronizationManager.class)) {
        tsm.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
        tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
            .thenAnswer(inv -> null);

        assertThatThrownBy(() -> service.placeOrder(1L, minimalForm(), singleItemCart()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("上限を超過");
      }
    }

    @Test
    @DisplayName("采番された sequence が order_number_counters に正しく問い合わせられる")
    void delegatesToNextOrderSequenceWithCorrectDate() {
      setupForPlaceOrder(1);

      placeOrderInMockTransaction(minimalForm(), singleItemCart());

      verify(orderRepository).nextOrderSequence(ORDER_DATE);
    }

    @Test
    @DisplayName("O-04: 固定時刻を 2026-12-31 にしたとき日付部が ORD20261231 になる")
    void clockOnDecember31_producesCorrectDatePart() {
      Clock clock31Dec = Clock.fixed(Instant.parse("2026-12-31T00:00:00Z"), ZoneId.of("UTC"));
      OrderService svc31 = new OrderService(
          orderRepository, new ObjectMapper(),
          mock(NotificationMailService.class), clock31Dec, messageSource);
      LocalDate date31 = LocalDate.of(2026, 12, 31);
      when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
      when(orderRepository.nextOrderSequence(date31)).thenReturn(1);
      when(orderRepository.insertOrder(any())).thenReturn(101L);

      try (MockedStatic<TransactionSynchronizationManager> tsm = Mockito
          .mockStatic(TransactionSynchronizationManager.class)) {
        tsm.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
        tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
            .thenAnswer(inv -> null);

        String orderNumber = svc31.placeOrder(1L, minimalForm(), singleItemCart());
        assertThat(orderNumber).isEqualTo("ORD20261231-000001");
      }
    }
  }

  // ============================================================
  // placeOrder: 業務バリデーション
  // ============================================================

  @Nested
  @DisplayName("placeOrder — バリデーション")
  class PlaceOrderValidation {

    @Test
    @DisplayName("カートが null のとき IllegalArgumentException")
    void nullCart_throwsIllegalArgumentException() {
      assertThatThrownBy(() -> service.placeOrder(1L, minimalForm(), null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("カートが空（明細ゼロ件）のとき IllegalArgumentException")
    void emptyCart_throwsIllegalArgumentException() {
      CartView emptyCart = new CartView(
          List.of(), 0, 0,
          new CartSummaryView(BigDecimal.ZERO, BigDecimal.ZERO,
              BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

      assertThatThrownBy(() -> service.placeOrder(1L, minimalForm(), emptyCart))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("法人注文かつ会社名が未入力のとき IllegalArgumentException")
    void corporateWithoutCompanyName_throwsIllegalArgumentException() {
      CheckoutInputForm form = minimalForm();
      form.setPersonalOrCorporate("corporate");
      form.setCompanyName(null);

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-08: 法人 + companyName=空文字 → IllegalArgumentException（companyRequired）")
    void corporateWithEmptyCompanyName_throwsIllegalArgumentException() {
      CheckoutInputForm form = minimalForm();
      form.setPersonalOrCorporate("corporate");
      form.setCompanyName("");

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-09: 法人 + companyName=空白のみ → IllegalArgumentException（companyRequired）")
    void corporateWithBlankCompanyName_throwsIllegalArgumentException() {
      CheckoutInputForm form = minimalForm();
      form.setPersonalOrCorporate("corporate");
      form.setCompanyName("   ");

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-10: 個人注文で companyName=null でも成功する")
    void personalWithNullCompanyName_succeeds() {
      CheckoutInputForm form = minimalForm();
      form.setPersonalOrCorporate("personal");
      form.setCompanyName(null);
      setupForPlaceOrder(1);

      String orderNumber = placeOrderInMockTransaction(form, singleItemCart());

      assertThat(orderNumber).isNotBlank();
    }

    @Test
    @DisplayName("O-11: deliveryFloor=null → IllegalArgumentException（deliveryFloorRequired）")
    void deliveryFloorNull_throwsDeliveryFloorRequired() {
      CheckoutInputForm form = minimalForm();
      form.setDeliveryFloor(null);

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-12: deliveryFloor=空文字 → IllegalArgumentException（deliveryFloorRequired）")
    void deliveryFloorEmpty_throwsDeliveryFloorRequired() {
      CheckoutInputForm form = minimalForm();
      form.setDeliveryFloor("");

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-13: deliveryFloor=abc（非整数）→ IllegalArgumentException（deliveryFloorInteger）")
    void deliveryFloorNonInteger_throwsDeliveryFloorInteger() {
      CheckoutInputForm form = minimalForm();
      form.setDeliveryFloor("abc");

      assertThatThrownBy(() -> service.placeOrder(1L, form, singleItemCart()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("O-14: deliveryFloor=3（有効整数）→ 成功")
    void deliveryFloorValid_succeeds() {
      CheckoutInputForm form = minimalForm();
      form.setDeliveryFloor("3");
      setupForPlaceOrder(1);

      String orderNumber = placeOrderInMockTransaction(form, singleItemCart());

      assertThat(orderNumber).isNotBlank();
    }

    @Test
    @DisplayName("O-17: 数量=在庫数量（境界値、ちょうど足りる）→ 成功")
    void quantityEqualsStock_succeeds() {
      // stockQuantity=3, quantity=3 → 在庫チェック通過
      CartLineView exactStock = new CartLineView(
          1L, 10L, "テスト商品", "P0001-C01", "ナチュラル",
          new BigDecimal("10000"), 3, false, BigDecimal.ZERO, false, 3, "/products/10");
      CartSummaryView summary = new CartSummaryView(
          new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
          new BigDecimal("3000"), new BigDecimal("33000"));
      CartView cart = new CartView(List.of(exactStock), 1, 3, summary);
      setupForPlaceOrder(1);

      String orderNumber = placeOrderInMockTransaction(minimalForm(), cart);

      assertThat(orderNumber).isNotBlank();
    }

    @Test
    @DisplayName("在庫切れ商品があるとき IllegalArgumentException")
    void outOfStockItem_throwsIllegalArgumentException() {
      CartLineView outOfStock = new CartLineView(
          1L, 10L, "テスト商品", "P0001-C01", "ナチュラル",
          new BigDecimal("10000"), 0, false, BigDecimal.ZERO, false, 1, "/products/10");
      CartSummaryView summary = new CartSummaryView(
          new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO,
          new BigDecimal("1000"), new BigDecimal("11000"));
      CartView cart = new CartView(List.of(outOfStock), 1, 1, summary);

      assertThatThrownBy(() -> service.placeOrder(1L, minimalForm(), cart))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("注文数量が在庫を超えるとき IllegalArgumentException")
    void quantityExceedsStock_throwsIllegalArgumentException() {
      CartLineView overQuantity = new CartLineView(
          1L, 10L, "テスト商品", "P0001-C01", "ナチュラル",
          new BigDecimal("10000"), 2, false, BigDecimal.ZERO, false, 5, "/products/10");
      CartSummaryView summary = new CartSummaryView(
          new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO,
          new BigDecimal("5000"), new BigDecimal("55000"));
      CartView cart = new CartView(List.of(overQuantity), 1, 5, summary);

      assertThatThrownBy(() -> service.placeOrder(1L, minimalForm(), cart))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ============================================================
  // O-18〜O-23: placeOrder の副作用（受注保存、支払案内）
  // ============================================================

  @Nested
  @DisplayName("placeOrder — 副作用検証")
  class PlaceOrderSideEffects {

    @Test
    @DisplayName("O-18: memberId=null（ゲスト）のとき insertOrder の params で memberId が null")
    void guestOrder_memberIdNullInParams() {
      setupForPlaceOrder(1);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      placeOrderInMockTransaction(null, minimalForm(), singleItemCart());
      verify(orderRepository).insertOrder(captor.capture());

      assertThat(captor.getValue()).containsEntry("memberId", null);
    }

    @Test
    @DisplayName("O-19: memberId=100L のとき insertOrder の params で memberId が 100L")
    void memberOrder_memberIdInParams() {
      setupForPlaceOrder(1);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      placeOrderInMockTransaction(100L, minimalForm(), singleItemCart());
      verify(orderRepository).insertOrder(captor.capture());

      assertThat(captor.getValue()).containsEntry("memberId", 100L);
    }

    @Test
    @DisplayName("O-20: paymentMethod=convenience_store のとき paymentInstructionJson が非 null JSON")
    void convenenceStore_paymentInstructionJsonNonNull() {
      CheckoutInputForm form = minimalForm();
      form.setPaymentMethod("convenience_store");
      setupForPlaceOrder(1);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      placeOrderInMockTransaction(1L, form, singleItemCart());
      verify(orderRepository).insertOrder(captor.capture());

      Object json = captor.getValue().get("paymentInstructionJson");
      assertThat(json).isNotNull().asString().contains("payment_number");
    }

    @Test
    @DisplayName("O-21: paymentMethod=credit_card のとき paymentInstructionJson が null")
    void creditCard_paymentInstructionJsonNull() {
      CheckoutInputForm form = minimalForm();
      form.setPaymentMethod("credit_card");
      setupForPlaceOrder(1);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      placeOrderInMockTransaction(1L, form, singleItemCart());
      verify(orderRepository).insertOrder(captor.capture());

      assertThat(captor.getValue().get("paymentInstructionJson")).isNull();
    }

    @Test
    @DisplayName("O-22: カートに 3 明細あるとき insertOrderItem が 3 回呼ばれる")
    void threeItems_insertOrderItemCalledThreeTimes() {
      setupForPlaceOrder(1);

      placeOrderInMockTransaction(1L, minimalForm(), threeItemCart());

      verify(orderRepository, times(3)).insertOrderItem(any());
    }

    @Test
    @DisplayName("O-23: 正常注文のとき insertOrderStatusHistory が 1 回呼ばれる")
    void normalOrder_insertOrderStatusHistoryCalledOnce() {
      setupForPlaceOrder(1);

      placeOrderInMockTransaction(1L, minimalForm(), singleItemCart());

      verify(orderRepository, times(1)).insertOrderStatusHistory(any());
    }
  }

  // ============================================================
  // O-24〜O-28: createInitialForm
  // ============================================================

  @Nested
  @DisplayName("createInitialForm")
  class CreateInitialForm {

    private static final long MEMBER_ID = 200L;
    private final MemberSessionUser memberUser = new MemberSessionUser(MEMBER_ID, "m@e.com", "田中", "花子");

    @Test
    @DisplayName("O-24: member=empty のとき全フィールドが null のフォームを返す")
    void memberEmpty_returnsBlankForm() {
      CheckoutInputForm form = service.createInitialForm(Optional.empty());

      assertThat(form.getLastName()).isNull();
      assertThat(form.getEmail()).isNull();
    }

    @Test
    @DisplayName("O-25: ログイン済みでも prefill が empty のとき空フォームを返す")
    void prefillEmpty_returnsBlankForm() {
      when(orderRepository.findCheckoutMemberPrefill(MEMBER_ID)).thenReturn(Optional.empty());

      CheckoutInputForm form = service.createInitialForm(Optional.of(memberUser));

      assertThat(form.getLastName()).isNull();
    }

    @Test
    @DisplayName("O-26: ログイン済みで prefill データがある場合はフォームに値が反映される")
    void prefillPresent_formPopulated() {
      CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
          "personal", "鈴木", "一郎", "スズキ", "イチロウ",
          null, null, "i@e.com", "0312345678", null,
          null, "大阪府", "大阪市", "梅田1-1", 5, true);
      when(orderRepository.findCheckoutMemberPrefill(MEMBER_ID)).thenReturn(Optional.of(prefill));

      CheckoutInputForm form = service.createInitialForm(Optional.of(memberUser));

      assertThat(form.getLastName()).isEqualTo("鈴木");
      assertThat(form.getEmail()).isEqualTo("i@e.com");
      assertThat(form.getPrefecture()).isEqualTo("大阪府");
    }

    @Test
    @DisplayName("O-27: 郵便番号が7桁のとき part1（3桁）と part2（4桁）に分割される")
    void postalCode7Digits_splitCorrectly() {
      CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
          "personal", "鈴木", "一郎", "スズキ", "イチロウ",
          null, null, "i@e.com", "0312345678", null,
          "1234567", "東京都", "中央区", "銀座1-1", 2, true);
      when(orderRepository.findCheckoutMemberPrefill(MEMBER_ID)).thenReturn(Optional.of(prefill));

      CheckoutInputForm form = service.createInitialForm(Optional.of(memberUser));

      assertThat(form.getPostalCodePart1()).isEqualTo("123");
      assertThat(form.getPostalCodePart2()).isEqualTo("4567");
    }

    @Test
    @DisplayName("O-28: 郵便番号が null のとき part1・part2 は null のまま")
    void postalCodeNull_partsRemainNull() {
      CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
          "personal", "鈴木", "一郎", "スズキ", "イチロウ",
          null, null, "i@e.com", "0312345678", null,
          null, "東京都", "中央区", "銀座1-1", 2, true);
      when(orderRepository.findCheckoutMemberPrefill(MEMBER_ID)).thenReturn(Optional.of(prefill));

      CheckoutInputForm form = service.createInitialForm(Optional.of(memberUser));

      assertThat(form.getPostalCodePart1()).isNull();
      assertThat(form.getPostalCodePart2()).isNull();
    }
  }

  // ============================================================
  // O-29〜O-32: findOrderCompleteView
  // ============================================================

  @Nested
  @DisplayName("findOrderCompleteView")
  class FindOrderCompleteView {

    @Test
    @DisplayName("O-29: orderNumber=null → Optional.empty を返す（repository は呼ばれない）")
    void nullOrderNumber_returnsEmpty() {
      Optional<OrderCompleteView> result = service.findOrderCompleteView(null);

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findOrderCompleteByOrderNumber(anyString());
    }

    @Test
    @DisplayName("O-30: orderNumber=空文字 → Optional.empty を返す")
    void emptyOrderNumber_returnsEmpty() {
      Optional<OrderCompleteView> result = service.findOrderCompleteView("");

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findOrderCompleteByOrderNumber(anyString());
    }

    @Test
    @DisplayName("O-31: orderNumber=空白のみ → Optional.empty を返す")
    void blankOrderNumber_returnsEmpty() {
      Optional<OrderCompleteView> result = service.findOrderCompleteView("   ");

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findOrderCompleteByOrderNumber(anyString());
    }

    @Test
    @DisplayName("O-32: 有効な orderNumber のとき repository に委譲して結果を返す")
    void validOrderNumber_delegatesToRepository() {
      OrderCompleteView view = mock(OrderCompleteView.class);
      when(orderRepository.findOrderCompleteByOrderNumber("ORD20260310-000001"))
          .thenReturn(Optional.of(view));

      Optional<OrderCompleteView> result = service.findOrderCompleteView("ORD20260310-000001");

      assertThat(result).contains(view);
    }
  }

  // ============================================================
  // O-33〜O-35: findMemberOrderHistories（ページ補正）
  // ============================================================

  @Nested
  @DisplayName("findMemberOrderHistories — ページ補正")
  class FindMemberOrderHistories {

    private static final long MEMBER_ID = 300L;

    @Test
    @DisplayName("O-33: page=1 のとき page=1 で repository が呼ばれる")
    void pageOne_calledWithOne() {
      when(orderRepository.findMemberOrders(eq(MEMBER_ID), eq(1), anyInt()))
          .thenReturn(mock(MemberOrderHistoryPage.class));

      service.findMemberOrderHistories(MEMBER_ID, 1);

      verify(orderRepository).findMemberOrders(MEMBER_ID, 1, 10);
    }

    @Test
    @DisplayName("O-34: page=0 のとき page=1 に補正される")
    void pageZero_normalizedToOne() {
      when(orderRepository.findMemberOrders(eq(MEMBER_ID), eq(1), anyInt()))
          .thenReturn(mock(MemberOrderHistoryPage.class));

      service.findMemberOrderHistories(MEMBER_ID, 0);

      verify(orderRepository).findMemberOrders(MEMBER_ID, 1, 10);
    }

    @Test
    @DisplayName("O-35: page=-1 のとき page=1 に補正される")
    void pageNegative_normalizedToOne() {
      when(orderRepository.findMemberOrders(eq(MEMBER_ID), eq(1), anyInt()))
          .thenReturn(mock(MemberOrderHistoryPage.class));

      service.findMemberOrderHistories(MEMBER_ID, -1);

      verify(orderRepository).findMemberOrders(MEMBER_ID, 1, 10);
    }
  }

  // ============================================================
  // O-36〜O-39: findMemberOrderDetail / findReorderItems
  // ============================================================

  @Nested
  @DisplayName("findMemberOrderDetail / findReorderItems — blank ガード")
  class FindMemberOrderDetailAndReorderItems {

    private static final long MEMBER_ID = 400L;

    @Test
    @DisplayName("O-36: findMemberOrderDetail に orderNumber=null → Optional.empty（repository 呼ばれず）")
    void detailNullOrderNumber_returnsEmpty() {
      Optional<?> result = service.findMemberOrderDetail(MEMBER_ID, null);

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findMemberOrderDetail(anyLong(), anyString());
    }

    @Test
    @DisplayName("O-37: findMemberOrderDetail に orderNumber=空文字 → Optional.empty")
    void detailEmptyOrderNumber_returnsEmpty() {
      Optional<?> result = service.findMemberOrderDetail(MEMBER_ID, "");

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findMemberOrderDetail(anyLong(), anyString());
    }

    @Test
    @DisplayName("O-38: findReorderItems に orderNumber=null → 空リスト（repository 呼ばれず）")
    void reorderNullOrderNumber_returnsEmpty() {
      List<?> result = service.findReorderItems(MEMBER_ID, null);

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findReorderItems(anyLong(), anyString());
    }

    @Test
    @DisplayName("O-39: findReorderItems に orderNumber=空文字 → 空リスト")
    void reorderEmptyOrderNumber_returnsEmpty() {
      List<?> result = service.findReorderItems(MEMBER_ID, "");

      assertThat(result).isEmpty();
      verify(orderRepository, never()).findReorderItems(anyLong(), anyString());
    }
  }

  // ============================================================
  // ヘルパメソッド
  // ============================================================

  /**
   * {@link TransactionSynchronizationManager} の静的メソッドをモックしたうえで
   * {@link OrderService#placeOrder} を呼び出す。memberId は 1L 固定。
   */
  private String placeOrderInMockTransaction(CheckoutInputForm form, CartView cart) {
    return placeOrderInMockTransaction(1L, form, cart);
  }

  /**
   * memberId を指定して {@link OrderService#placeOrder} をトランザクションコンテキスト付きで呼び出す。
   */
  private String placeOrderInMockTransaction(Long memberId, CheckoutInputForm form, CartView cart) {
    try (MockedStatic<TransactionSynchronizationManager> tsm = Mockito
        .mockStatic(TransactionSynchronizationManager.class)) {
      tsm.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
          .thenAnswer(inv -> null);
      return service.placeOrder(memberId, form, cart);
    }
  }

  /** OrderRepository の各モックを happy path 用に設定する。 */
  private void setupForPlaceOrder(int sequence) {
    when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
    when(orderRepository.nextOrderSequence(ORDER_DATE)).thenReturn(sequence);
    when(orderRepository.insertOrder(any())).thenReturn(101L);
  }

  /** 最低限のフィールドを埋めた個人注文フォーム。 */
  private static CheckoutInputForm minimalForm() {
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
    form.setAddressLine("千代田1-1");
    form.setDeliveryFloor("3");
    form.setHasElevator(Boolean.TRUE);
    form.setPaymentMethod("bank_transfer");
    return form;
  }

  /** 在庫あり・通常配送の 1 明細カート。 */
  private static CartView singleItemCart() {
    CartLineView line = new CartLineView(
        1L, 10L, "テスト商品", "P0001-C01", "ナチュラル",
        new BigDecimal("10000"), 5, false, BigDecimal.ZERO, false, 1, "/products/10");
    CartSummaryView summary = new CartSummaryView(
        new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO,
        new BigDecimal("1000"), new BigDecimal("11000"));
    return new CartView(List.of(line), 1, 1, summary);
  }

  /** 在庫あり・通常配送の 3 明細カート（O-22 用）。 */
  private static CartView threeItemCart() {
    BigDecimal price = new BigDecimal("5000");
    List<CartLineView> lines = List.of(
        new CartLineView(1L, 10L, "商品A", "PA-01", "白", price, 10, false, BigDecimal.ZERO, false, 1, "/products/10"),
        new CartLineView(2L, 11L, "商品B", "PB-01", "黒", price, 10, false, BigDecimal.ZERO, false, 2, "/products/11"),
        new CartLineView(3L, 12L, "商品C", "PC-01", "赤", price, 10, false, BigDecimal.ZERO, false, 1, "/products/12"));
    CartSummaryView summary = new CartSummaryView(
        new BigDecimal("20000"), BigDecimal.ZERO, BigDecimal.ZERO,
        new BigDecimal("2000"), new BigDecimal("22000"));
    return new CartView(lines, 3, 4, summary);
  }
}
