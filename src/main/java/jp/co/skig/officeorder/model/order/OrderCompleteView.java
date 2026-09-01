package jp.co.skig.officeorder.model.order;

import java.math.BigDecimal;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * 注文完了画面に表示する注文結果モデル。
 */
public record OrderCompleteView(
        String orderNumber,
        String customerLastName,
        String customerFirstName,
        String customerEmail,
        String shippingPostalCode,
        String shippingPrefecture,
        String shippingCity,
        String shippingAddressLine,
        Integer shippingFloor,
        Boolean shippingHasElevator,
        String paymentMethod,
        String convenienceStorePaymentNumber,
        String convenienceStorePaymentDueDate,
        BigDecimal subtotalAmount,
        BigDecimal assemblyFeeTotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {
    /**
     * コンビニ決済向けの追加案内表示が必要かを判定する。
     */
    public boolean isConvenienceStorePayment() {
        return "convenience_store".equals(paymentMethod);
    }

    /**
     * 決済手段コードを画面表示用の文言へ変換する。
     */
    public String paymentMethodLabel() {
        if ("cash_on_delivery".equals(paymentMethod)) {
            return "代金引換";
        }
        if ("convenience_store".equals(paymentMethod)) {
            return "コンビニ決済";
        }
        return "銀行振込";
    }

    /**
     * 合計金額を画面表示用に整形する。
     */
    public String totalAmountText() {
        return MoneyFormatter.formatYen(totalAmount);
    }
}
