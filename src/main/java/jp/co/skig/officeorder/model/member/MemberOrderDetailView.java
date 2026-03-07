package jp.co.skig.officeorder.model.member;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * 購入履歴詳細画面全体を表す表示モデル。
 */
public record MemberOrderDetailView(
        String orderNumber,
        OffsetDateTime orderDatetime,
        String orderStatus,
        BigDecimal subtotalAmount,
        BigDecimal assemblyFeeTotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<MemberOrderStatusHistoryView> statusHistories,
        List<MemberOrderItemDetailView> items
) {
    private static final DateTimeFormatter ORDER_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 注文日時を一覧・詳細で統一した表示形式へ整形する。
     */
    public String orderDatetimeDisplay() {
        if (orderDatetime == null) {
            return "-";
        }
        return ORDER_DATETIME_FORMATTER.format(orderDatetime);
    }

    /**
     * 注文ステータスコードを日本語ラベルへ変換する。
     */
    public String orderStatusLabel() {
        if ("awaiting_payment".equals(orderStatus)) {
            return "入金待ち";
        }
        if ("processing".equals(orderStatus)) {
            return "処理中";
        }
        if ("completed".equals(orderStatus)) {
            return "完了";
        }
        if ("cancelled".equals(orderStatus)) {
            return "キャンセル";
        }
        return "受付";
    }

    /**
     * 商品小計を画面表示用に整形する。
     */
    public String subtotalAmountText() {
        return MoneyFormatter.formatYen(subtotalAmount);
    }

    /**
     * 組立・設置費合計を画面表示用に整形する。
     */
    public String assemblyFeeTotalText() {
        return MoneyFormatter.formatYen(assemblyFeeTotal);
    }

    /**
     * 送料を画面表示用に整形する。
     */
    public String shippingFeeText() {
        return MoneyFormatter.formatYen(shippingFee);
    }

    /**
     * 消費税額を画面表示用に整形する。
     */
    public String taxAmountText() {
        return MoneyFormatter.formatYen(taxAmount);
    }

    /**
     * 合計金額を画面表示用に整形する。
     */
    public String totalAmountText() {
        return MoneyFormatter.formatYen(totalAmount);
    }
}
