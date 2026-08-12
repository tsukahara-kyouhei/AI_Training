package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.mapper.OrderMapper;
import jp.co.skig.officeorder.mapper.row.MemberOrderDetailMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderItemMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderStatusHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderCompleteMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderReorderItemMapperRow;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryView;
import jp.co.skig.officeorder.model.member.MemberOrderItemDetailView;
import jp.co.skig.officeorder.model.member.MemberOrderStatusHistoryView;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 注文関連データの取得・保存と画面表示用整形を担当するリポジトリ。
 */
@Repository
public class OrderRepository {

    /** 注文関連ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(OrderRepository.class);

    /** 注文SQLを呼び出す MyBatis Mapper。 */
    private final OrderMapper orderMapper;
    /** payment_instruction のJSON変換に使う ObjectMapper。 */
    private final ObjectMapper objectMapper;
    /** 時刻依存条件に使う共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * 注文リポジトリを生成する。
     *
     * @param orderMapper     注文Mapper
     * @param objectMapper    JSONシリアライザ
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public OrderRepository(OrderMapper orderMapper,
            ObjectMapper objectMapper,
            AppTimeProvider appTimeProvider) {
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * 注文入力画面の会員初期表示情報を取得する。
     *
     * @param memberId 会員ID
     * @return 初期表示情報
     */
    public Optional<CheckoutMemberPrefill> findCheckoutMemberPrefill(long memberId) {
        CheckoutMemberPrefill row = orderMapper.selectCheckoutMemberPrefill(memberId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(row);
    }

    /**
     * 注文ヘッダを保存する。
     *
     * @param params 保存パラメータ
     * @return 採番された注文ID
     */
    public long insertOrder(Map<String, Object> params) {
        Long orderId = orderMapper.insertOrder(params);
        if (orderId == null) {
            throw new IllegalStateException("注文ヘッダの保存に失敗しました。");
        }
        return orderId;
    }

    /**
     * 注文明細を保存する。
     *
     * @param params 保存パラメータ
     */
    public void insertOrderItem(Map<String, Object> params) {
        orderMapper.insertOrderItem(params);
    }

    /**
     * 注文ステータス履歴を保存する。
     *
     * @param params 保存パラメータ
     */
    public void insertOrderStatusHistory(Map<String, Object> params) {
        orderMapper.insertOrderStatusHistory(params);
    }

    /**
     * 指定日の日次連番を採番する。
     *
     * @param orderDate 注文日
     * @return 採番された連番
     */
    public int nextOrderSequence(LocalDate orderDate) {
        Integer sequence = orderMapper.nextOrderSequence(orderDate);
        if (sequence == null || sequence <= 0) {
            throw new IllegalStateException("注文番号連番の採番に失敗しました。");
        }
        return sequence;
    }

    /**
     * 現在有効な消費税率を取得する。
     *
     * @return 税率百分率。未設定時は 10
     */
    public BigDecimal findCurrentTaxRatePercent() {
        BigDecimal taxRate = orderMapper.selectCurrentTaxRatePercent(appTimeProvider.nowOffsetDateTime());
        return taxRate == null ? BigDecimal.TEN : taxRate;
    }

    /**
     * 注文完了画面表示用の注文情報を取得する。
     *
     * @param orderNumber 注文番号
     * @return 注文完了画面情報
     */
    public Optional<OrderCompleteView> findOrderCompleteByOrderNumber(String orderNumber) {
        OrderCompleteMapperRow row = orderMapper.selectOrderCompleteByOrderNumber(orderNumber);
        if (row == null) {
            return Optional.empty();
        }
        String paymentMethod = row.paymentMethod();
        Map<String, String> instruction = parsePaymentInstruction(
                orderNumber,
                paymentMethod,
                row.paymentInstruction());
        if ("convenience_store".equals(paymentMethod)
                && (instruction.get("payment_number") == null || instruction.get("payment_due_date") == null)) {
            log.warn("event={} orderNumber={} paymentMethod={} reason=missing_payment_instruction_fields",
                    LogEvent.ORDER_PAYMENT_INSTRUCTION_PARSE_FAILED.value(),
                    orderNumber,
                    paymentMethod);
        }
        return Optional.of(new OrderCompleteView(
                row.orderNumber(),
                row.customerLastName(),
                row.customerFirstName(),
                row.customerEmail(),
                row.shippingPostalCode(),
                row.shippingPrefecture(),
                row.shippingCity(),
                row.shippingAddressLine(),
                row.shippingFloor(),
                row.shippingHasElevator(),
                paymentMethod,
                instruction.get("payment_number"),
                instruction.get("payment_due_date"),
                row.subtotalAmount(),
                row.assemblyFeeTotal(),
                row.shippingFee(),
                row.taxAmount(),
                row.totalAmount()));
    }

    /**
     * 会員の購入履歴一覧をページング付きで取得する。
     *
     * @param memberId 会員ID
     * @param page     ページ番号
     * @param size     ページサイズ
     * @return 購入履歴ページ
     */
    public MemberOrderHistoryPage findMemberOrders(long memberId, int page, int size) {
        long totalCount = Optional.ofNullable(orderMapper.countMemberOrders(memberId)).orElse(0L);
        int offset = Math.max(0, (page - 1) * size);
        var rows = orderMapper.selectMemberOrders(memberId, size, offset);
        var items = rows.stream()
                .map(this::toMemberOrderHistoryView)
                .toList();
        return new MemberOrderHistoryPage(items, totalCount, page, size);
    }

    /**
     * 会員の注文詳細を取得し、履歴と明細をまとめて返す。
     *
     * @param memberId    会員ID
     * @param orderNumber 注文番号
     * @return 注文詳細
     */
    public Optional<MemberOrderDetailView> findMemberOrderDetail(long memberId, String orderNumber) {
        MemberOrderDetailMapperRow header = orderMapper.selectMemberOrderDetail(memberId, orderNumber);
        if (header == null) {
            return Optional.empty();
        }
        List<MemberOrderStatusHistoryView> statusHistories = orderMapper
                .selectMemberOrderStatusHistories(memberId, orderNumber)
                .stream()
                .map(this::toMemberOrderStatusHistoryView)
                .toList();
        List<MemberOrderItemDetailView> items = orderMapper
                .selectMemberOrderItems(memberId, orderNumber)
                .stream()
                .map(this::toMemberOrderItemDetailView)
                .toList();
        return Optional.of(new MemberOrderDetailView(
                header.orderNumber(),
                header.orderDatetime(),
                header.orderStatus(),
                header.subtotalAmount(),
                header.assemblyFeeTotal(),
                header.shippingFee(),
                header.taxAmount(),
                header.totalAmount(),
                statusHistories,
                items));
    }

    /**
     * 再購入用の注文商品一覧を取得する。
     *
     * @param memberId    会員ID
     * @param orderNumber 注文番号
     * @return 再購入商品一覧
     */
    public List<OrderReorderItem> findReorderItems(long memberId, String orderNumber) {
        return orderMapper.selectMemberReorderItems(memberId, orderNumber).stream()
                .map(this::toOrderReorderItem)
                .toList();
    }

    /**
     * payment_instruction を画面表示用の key-value へ変換する。
     *
     * @param orderNumber    注文番号
     * @param paymentMethod  支払方法
     * @param rawInstruction 生の保存値
     * @return 支払案内情報
     */
    private Map<String, String> parsePaymentInstruction(String orderNumber,
            String paymentMethod,
            Object rawInstruction) {
        if (rawInstruction == null) {
            return Map.of();
        }
        try {
            String json = String.valueOf(rawInstruction);
            if (json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            String payload = String.valueOf(rawInstruction);
            int payloadLength = payload == null ? 0 : payload.length();
            log.warn("event={} orderNumber={} paymentMethod={} payloadLength={} reason=parse_failed",
                    LogEvent.ORDER_PAYMENT_INSTRUCTION_PARSE_FAILED.value(),
                    orderNumber,
                    paymentMethod,
                    payloadLength,
                    ex);
            return Map.of();
        }
    }

    /**
     * 注文履歴一覧行を表示用モデルへ変換する。
     *
     * @param row 履歴行
     * @return 表示用注文履歴
     */
    private MemberOrderHistoryView toMemberOrderHistoryView(MemberOrderHistoryMapperRow row) {
        return new MemberOrderHistoryView(
                row.orderNumber(),
                row.orderDatetime(),
                row.totalAmount(),
                row.orderStatus());
    }

    /**
     * 注文ステータス履歴行を表示用モデルへ変換する。
     *
     * @param row ステータス履歴行
     * @return 表示用ステータス履歴
     */
    private MemberOrderStatusHistoryView toMemberOrderStatusHistoryView(MemberOrderStatusHistoryMapperRow row) {
        return new MemberOrderStatusHistoryView(
                row.changedAt(),
                row.status());
    }

    /**
     * 注文明細行を表示用モデルへ変換する。
     *
     * @param row 注文明細行
     * @return 表示用注文明細
     */
    private MemberOrderItemDetailView toMemberOrderItemDetailView(MemberOrderItemMapperRow row) {
        return new MemberOrderItemDetailView(
                row.productCode(),
                row.productName(),
                row.colorName(),
                row.unitPrice(),
                row.assemblyFee(),
                row.quantity() == null ? 0 : row.quantity(),
                row.lineSubtotal());
    }

    /**
     * 再購入用行をカート追加用モデルへ変換する。
     *
     * @param row 再購入対象行
     * @return カート再投入用商品
     */
    private OrderReorderItem toOrderReorderItem(OrderReorderItemMapperRow row) {
        Boolean assemblyRequested = null;
        if (Boolean.TRUE.equals(row.assemblyAvailable())) {
            assemblyRequested = row.assemblyFee() != null && row.assemblyFee().compareTo(BigDecimal.ZERO) > 0;
        }
        return new OrderReorderItem(
                row.productVariantId(),
                row.productCode(),
                row.quantity() == null ? 0 : row.quantity(),
                assemblyRequested);
    }

    // 購入履歴のチェック（1件以上あれば true を返す）
    public boolean hasPurchasedProduct(Long memberId, String productId) {
        Long count = orderMapper.countMemberPurchasedProduct(memberId, productId);
        return count != null && count > 0;
    }

}
