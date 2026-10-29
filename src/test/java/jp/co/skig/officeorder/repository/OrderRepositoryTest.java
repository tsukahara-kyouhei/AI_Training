package jp.co.skig.officeorder.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.OrderMapper;
import jp.co.skig.officeorder.mapper.row.MemberOrderDetailMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderItemMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderStatusHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderCompleteMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderCouponRefundMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderReorderItemMapperRow;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 注文リポジトリの単体テスト。
 *
 * <p>単体テスト計画 4.6 リポジトリ・データ変換に対応する。
 */
class OrderRepositoryTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, TOKYO);

    private OrderMapper orderMapper;
    private ObjectMapper objectMapper;
    private AppTimeProvider appTimeProvider;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        objectMapper = new ObjectMapper();
        appTimeProvider = mock(AppTimeProvider.class);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(FIXED_NOW);
        orderRepository = new OrderRepository(orderMapper, objectMapper, appTimeProvider);
    }

    @Nested
    @DisplayName("注文ヘッダ・連番・税率")
    class OrderHeader {

        @Test
        @DisplayName("insertOrderはMapperが返した注文IDを返す")
        void insertOrder_returnsOrderId() {
            when(orderMapper.insertOrder(any())).thenReturn(100L);

            long orderId = orderRepository.insertOrder(Map.of());

            assertThat(orderId).isEqualTo(100L);
        }

        @Test
        @DisplayName("insertOrderはMapperがnullを返したら例外を送出する")
        void insertOrder_nullOrderId_throws() {
            when(orderMapper.insertOrder(any())).thenReturn(null);

            assertThatThrownBy(() -> orderRepository.insertOrder(Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("注文ヘッダ");
        }

        @Test
        @DisplayName("nextOrderSequenceはMapperの連番を返す")
        void nextOrderSequence_returnsSequence() {
            when(orderMapper.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(42);

            int sequence = orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3));

            assertThat(sequence).isEqualTo(42);
        }

        @Test
        @DisplayName("nextOrderSequenceはnull・0以下で例外を送出する")
        void nextOrderSequence_invalid_throws() {
            when(orderMapper.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(null);

            assertThatThrownBy(() -> orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("連番");

            when(orderMapper.nextOrderSequence(LocalDate.of(2026, 9, 3))).thenReturn(0);

            assertThatThrownBy(() -> orderRepository.nextOrderSequence(LocalDate.of(2026, 9, 3)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("連番");
        }

        @Test
        @DisplayName("findCurrentTaxRatePercentは税率未設定時はデフォルト10を返す")
        void findCurrentTaxRatePercent_null_returnsDefaultTen() {
            when(orderMapper.selectCurrentTaxRatePercent(FIXED_NOW)).thenReturn(null);

            assertThat(orderRepository.findCurrentTaxRatePercent()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("findCurrentTaxRatePercentは取得した税率を返す")
        void findCurrentTaxRatePercent_returnsRate() {
            when(orderMapper.selectCurrentTaxRatePercent(FIXED_NOW)).thenReturn(new BigDecimal("8"));

            assertThat(orderRepository.findCurrentTaxRatePercent()).isEqualByComparingTo(new BigDecimal("8"));
        }
    }

    @Nested
    @DisplayName("注文完了画面")
    class OrderComplete {

        @Test
        @DisplayName("注文完了情報はMapper行を表示用モデルへ変換する")
        void findOrderCompleteByOrderNumber_convertsRow() throws Exception {
            OrderCompleteMapperRow row = new OrderCompleteMapperRow(
                    "ORD20260903-000001", "山田", "太郎", "taro@example.com",
                    "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "bank_transfer", null,
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    null, null, BigDecimal.ZERO
            );
            when(orderMapper.selectOrderCompleteByOrderNumber("ORD20260903-000001")).thenReturn(row);

            Optional<OrderCompleteView> result = orderRepository.findOrderCompleteByOrderNumber("ORD20260903-000001");

            assertThat(result).isPresent();
            assertThat(result.get().orderNumber()).isEqualTo("ORD20260903-000001");
            assertThat(result.get().paymentMethod()).isEqualTo("bank_transfer");
            assertThat(result.get().convenienceStorePaymentNumber()).isNull();
        }

        @Test
        @DisplayName("注文完了情報は0件ならOptional.emptyを返す")
        void findOrderCompleteByOrderNumber_notFound_returnsEmpty() {
            when(orderMapper.selectOrderCompleteByOrderNumber("ORD20260903-000001")).thenReturn(null);

            assertThat(orderRepository.findOrderCompleteByOrderNumber("ORD20260903-000001")).isEmpty();
        }

        @Test
        @DisplayName("コンビニ決済の支払案内JSONを表示用フィールドへ変換する")
        void findOrderCompleteByOrderNumber_convenienceStore_parsesInstruction() throws Exception {
            OrderCompleteMapperRow row = new OrderCompleteMapperRow(
                    "ORD20260903-000001", "山田", "太郎", "taro@example.com",
                    "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "convenience_store",
                    "{\"payment_number\":\"123456\",\"payment_due_date\":\"2026-09-06\"}",
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    null, null, BigDecimal.ZERO
            );
            when(orderMapper.selectOrderCompleteByOrderNumber("ORD20260903-000001")).thenReturn(row);

            Optional<OrderCompleteView> result = orderRepository.findOrderCompleteByOrderNumber("ORD20260903-000001");

            assertThat(result).isPresent();
            assertThat(result.get().convenienceStorePaymentNumber()).isEqualTo("123456");
            assertThat(result.get().convenienceStorePaymentDueDate()).isEqualTo("2026-09-06");
        }

        @Test
        @DisplayName("支払案内JSONが不正な場合は空の案内として扱う")
        void findOrderCompleteByOrderNumber_invalidJson_returnsEmptyInstruction() throws Exception {
            OrderCompleteMapperRow row = new OrderCompleteMapperRow(
                    "ORD20260903-000001", "山田", "太郎", "taro@example.com",
                    "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "convenience_store", "{invalid json",
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    null, null, BigDecimal.ZERO
            );
            when(orderMapper.selectOrderCompleteByOrderNumber("ORD20260903-000001")).thenReturn(row);

            Optional<OrderCompleteView> result = orderRepository.findOrderCompleteByOrderNumber("ORD20260903-000001");

            assertThat(result).isPresent();
            assertThat(result.get().convenienceStorePaymentNumber()).isNull();
            assertThat(result.get().convenienceStorePaymentDueDate()).isNull();
        }
    }

    @Nested
    @DisplayName("購入履歴")
    class OrderHistory {

        @Test
        @DisplayName("購入履歴一覧はMapper行を表示用モデルへ変換する")
        void findMemberOrders_convertsRows() {
            MemberOrderHistoryMapperRow row = new MemberOrderHistoryMapperRow(
                    "ORD20260903-000001", FIXED_NOW, new BigDecimal("33000"), "received"
            );
            when(orderMapper.countMemberOrders(1L)).thenReturn(1L);
            when(orderMapper.selectMemberOrders(1L, 10, 0)).thenReturn(List.of(row));

            MemberOrderHistoryPage page = orderRepository.findMemberOrders(1L, 1, 10);

            assertThat(page.totalCount()).isEqualTo(1L);
            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().orderNumber()).isEqualTo("ORD20260903-000001");
            assertThat(page.items().getFirst().orderStatus()).isEqualTo("received");
        }

        @Test
        @DisplayName("購入履歴一覧は0件なら空リストを返す")
        void findMemberOrders_zeroRows_returnsEmpty() {
            when(orderMapper.countMemberOrders(1L)).thenReturn(0L);
            when(orderMapper.selectMemberOrders(1L, 10, 0)).thenReturn(List.of());

            MemberOrderHistoryPage page = orderRepository.findMemberOrders(1L, 1, 10);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalCount()).isZero();
        }

        @Test
        @DisplayName("購入履歴一覧はcountがnullなら0件として扱う")
        void findMemberOrders_nullCount_treatsAsZero() {
            when(orderMapper.countMemberOrders(1L)).thenReturn(null);
            when(orderMapper.selectMemberOrders(1L, 10, 0)).thenReturn(List.of());

            MemberOrderHistoryPage page = orderRepository.findMemberOrders(1L, 1, 10);

            assertThat(page.totalCount()).isZero();
        }

        @Test
        @DisplayName("注文詳細はヘッダ・ステータス履歴・明細をまとめて返す")
        void findMemberOrderDetail_combinesHeaderHistoriesAndItems() {
            MemberOrderDetailMapperRow header = new MemberOrderDetailMapperRow(
                    "ORD20260903-000001", FIXED_NOW, "received",
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    "SAVE5000", "5,000円割引", new BigDecimal("5000")
            );
            MemberOrderStatusHistoryMapperRow status = new MemberOrderStatusHistoryMapperRow(
                    FIXED_NOW, "received"
            );
            MemberOrderItemMapperRow item = new MemberOrderItemMapperRow(
                    "P0001-C01", "デスク", "ブラック",
                    new BigDecimal("30000"), BigDecimal.ZERO, 1, new BigDecimal("30000"),
                    new BigDecimal("5000")
            );
            when(orderMapper.selectMemberOrderDetail(1L, "ORD20260903-000001")).thenReturn(header);
            when(orderMapper.selectMemberOrderStatusHistories(1L, "ORD20260903-000001")).thenReturn(List.of(status));
            when(orderMapper.selectMemberOrderItems(1L, "ORD20260903-000001")).thenReturn(List.of(item));

            Optional<MemberOrderDetailView> result = orderRepository.findMemberOrderDetail(1L, "ORD20260903-000001");

            assertThat(result).isPresent();
            assertThat(result.get().orderNumber()).isEqualTo("ORD20260903-000001");
            assertThat(result.get().statusHistories()).hasSize(1);
            assertThat(result.get().items()).hasSize(1);
            assertThat(result.get().items().getFirst().productCode()).isEqualTo("P0001-C01");
            assertThat(result.get().items().getFirst().lineCouponDiscountAmount())
                    .isEqualByComparingTo("5000");
            assertThat(result.get().hasCouponLineDiscountBreakdown()).isTrue();
        }

        @Test
        @DisplayName("既存クーポン注文は明細割引を再構成せず注文単位の割引だけを表示する")
        void findMemberOrderDetail_legacyCouponOrder_hidesUnavailableLineAllocation() {
            MemberOrderDetailMapperRow header = new MemberOrderDetailMapperRow(
                    "ORD20260903-000002", FIXED_NOW, "received",
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("2500"), new BigDecimal("27500"),
                    "SAVE5000", "5,000円割引", new BigDecimal("5000")
            );
            MemberOrderItemMapperRow item = new MemberOrderItemMapperRow(
                    "P0001-C01", "デスク", "ブラック",
                    new BigDecimal("30000"), BigDecimal.ZERO, 1, new BigDecimal("30000"),
                    BigDecimal.ZERO
            );
            when(orderMapper.selectMemberOrderDetail(1L, "ORD20260903-000002")).thenReturn(header);
            when(orderMapper.selectMemberOrderStatusHistories(1L, "ORD20260903-000002")).thenReturn(List.of());
            when(orderMapper.selectMemberOrderItems(1L, "ORD20260903-000002")).thenReturn(List.of(item));

            Optional<MemberOrderDetailView> result = orderRepository.findMemberOrderDetail(1L, "ORD20260903-000002");

            assertThat(result).isPresent();
            assertThat(result.get().couponDiscountAmount()).isEqualByComparingTo("5000");
            assertThat(result.get().hasCouponLineDiscountBreakdown()).isFalse();
        }

        @Test
        @DisplayName("注文詳細はヘッダなしならOptional.emptyを返す")
        void findMemberOrderDetail_noHeader_returnsEmpty() {
            when(orderMapper.selectMemberOrderDetail(1L, "ORD20260903-000001")).thenReturn(null);

            assertThat(orderRepository.findMemberOrderDetail(1L, "ORD20260903-000001")).isEmpty();
        }

        @Test
        @DisplayName("注文明細の数量がnullなら0として扱う")
        void findMemberOrderDetail_nullQuantity_treatsAsZero() {
            MemberOrderDetailMapperRow header = new MemberOrderDetailMapperRow(
                    "ORD20260903-000001", FIXED_NOW, "received",
                    new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3000"), new BigDecimal("33000"),
                    null, null, BigDecimal.ZERO
            );
            MemberOrderItemMapperRow item = new MemberOrderItemMapperRow(
                    "P0001-C01", "デスク", "ブラック",
                    new BigDecimal("30000"), BigDecimal.ZERO, null, new BigDecimal("30000"),
                    BigDecimal.ZERO
            );
            when(orderMapper.selectMemberOrderDetail(1L, "ORD20260903-000001")).thenReturn(header);
            when(orderMapper.selectMemberOrderStatusHistories(1L, "ORD20260903-000001")).thenReturn(List.of());
            when(orderMapper.selectMemberOrderItems(1L, "ORD20260903-000001")).thenReturn(List.of(item));

            Optional<MemberOrderDetailView> result = orderRepository.findMemberOrderDetail(1L, "ORD20260903-000001");

            assertThat(result).isPresent();
            assertThat(result.get().items().getFirst().quantity()).isZero();
        }
    }

    @Nested
    @DisplayName("再購入")
    class Reorder {

        @Test
        @DisplayName("再購入商品は組立可能かつ組立費ありなら組立希望をtrueにする")
        void findReorderItems_assemblyAvailableWithFee_requestsAssembly() {
            OrderReorderItemMapperRow row = new OrderReorderItemMapperRow(
                    1L, "P0001-C01", 2, true, new BigDecimal("5000")
            );
            when(orderMapper.selectMemberReorderItems(1L, "ORD20260903-000001")).thenReturn(List.of(row));

            List<OrderReorderItem> items = orderRepository.findReorderItems(1L, "ORD20260903-000001");

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().productVariantId()).isEqualTo(1L);
            assertThat(items.getFirst().quantity()).isEqualTo(2);
            assertThat(items.getFirst().assemblyRequested()).isTrue();
        }

        @Test
        @DisplayName("再購入商品は組立不可なら組立希望をnullにする")
        void findReorderItems_assemblyNotAvailable_assemblyRequestedIsNull() {
            OrderReorderItemMapperRow row = new OrderReorderItemMapperRow(
                    1L, "P0001-C01", 2, false, null
            );
            when(orderMapper.selectMemberReorderItems(1L, "ORD20260903-000001")).thenReturn(List.of(row));

            List<OrderReorderItem> items = orderRepository.findReorderItems(1L, "ORD20260903-000001");

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().assemblyRequested()).isNull();
        }

        @Test
        @DisplayName("再購入商品は数量がnullなら0として扱う")
        void findReorderItems_nullQuantity_treatsAsZero() {
            OrderReorderItemMapperRow row = new OrderReorderItemMapperRow(
                    1L, "P0001-C01", null, false, null
            );
            when(orderMapper.selectMemberReorderItems(1L, "ORD20260903-000001")).thenReturn(List.of(row));

            List<OrderReorderItem> items = orderRepository.findReorderItems(1L, "ORD20260903-000001");

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().quantity()).isZero();
        }
    }

    @Nested
    @DisplayName("クーポン利用実績返還")
    class CouponRefund {

        @Test
        @DisplayName("返品済み注文はcoupon、会員usageの順にロックして一度だけ返還する")
        void refundCouponUsageByOrderId_locksAndRestoresInOrder() {
            when(orderMapper.selectCouponRefundByOrderIdForUpdate(100L))
                    .thenReturn(new OrderCouponRefundMapperRow(7L, 12L, "returned", false));
            when(orderMapper.lockCouponForRefund(7L)).thenReturn(7L);
            when(orderMapper.lockUserCouponUsageForRefund(12L, 7L)).thenReturn(20L);
            when(orderMapper.decrementCouponUsedCount(7L)).thenReturn(1);
            when(orderMapper.decrementCouponUsage(12L, 7L)).thenReturn(1);
            when(orderMapper.markCouponUsageRefunded(100L)).thenReturn(1);

            orderRepository.refundCouponUsageByOrderId(100L);

            var inOrder = inOrder(orderMapper);
            inOrder.verify(orderMapper).selectCouponRefundByOrderIdForUpdate(100L);
            inOrder.verify(orderMapper).lockCouponForRefund(7L);
            inOrder.verify(orderMapper).lockUserCouponUsageForRefund(12L, 7L);
            inOrder.verify(orderMapper).decrementCouponUsedCount(7L);
            inOrder.verify(orderMapper).decrementCouponUsage(12L, 7L);
            inOrder.verify(orderMapper).markCouponUsageRefunded(100L);
        }

        @Test
        @DisplayName("返還済み注文は利用回数を再度減らさない")
        void refundCouponUsageByOrderId_alreadyRefunded_skipsCounters() {
            when(orderMapper.selectCouponRefundByOrderIdForUpdate(100L))
                    .thenReturn(new OrderCouponRefundMapperRow(7L, 12L, "cancelled", true));

            orderRepository.refundCouponUsageByOrderId(100L);

            verify(orderMapper, never()).lockCouponForRefund(7L);
            verify(orderMapper, never()).decrementCouponUsedCount(7L);
            verify(orderMapper, never()).decrementCouponUsage(12L, 7L);
        }
    }

    @Nested
    @DisplayName("会員初期表示")
    class CheckoutPrefill {

        @Test
        @DisplayName("会員初期表示はMapper行をOptionalで返す")
        void findCheckoutMemberPrefill_returnsPrefill() {
            CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                    "personal", "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "taro@example.com", "0312345678", null,
                    "1000001", "東京都", "千代田区", "丸の内1-1-1", 3, true
            );
            when(orderMapper.selectCheckoutMemberPrefill(1L)).thenReturn(prefill);

            Optional<CheckoutMemberPrefill> result = orderRepository.findCheckoutMemberPrefill(1L);

            assertThat(result).isPresent();
            assertThat(result.get().lastName()).isEqualTo("山田");
        }

        @Test
        @DisplayName("会員初期表示は0件ならOptional.emptyを返す")
        void findCheckoutMemberPrefill_notFound_returnsEmpty() {
            when(orderMapper.selectCheckoutMemberPrefill(1L)).thenReturn(null);

            assertThat(orderRepository.findCheckoutMemberPrefill(1L)).isEmpty();
        }
    }
}