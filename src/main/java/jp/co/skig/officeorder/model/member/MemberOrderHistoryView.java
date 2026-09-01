package jp.co.skig.officeorder.model.member;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * 購入履歴一覧の1注文分を表す表示モデル。
 */
public record MemberOrderHistoryView(
        String orderNumber,
        OffsetDateTime orderDatetime,
        BigDecimal totalAmount,
        String orderStatus) {
    private static final DateTimeFormatter ORDER_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 注文日時を購入履歴一覧向けの表示形式へ整形する。
     */
    public String orderDatetimeDisplay() {
        if (orderDatetime == null) {
            return "-";
        }
        return ORDER_DATETIME_FORMATTER.format(orderDatetime);
    }

    /**
     * 合計金額を購入履歴一覧向けの文字列へ整形する。
     */
    public String totalAmountText() {
        return MoneyFormatter.formatYen(totalAmount) + "円";
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
}
