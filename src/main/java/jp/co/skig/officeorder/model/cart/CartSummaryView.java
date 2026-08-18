package jp.co.skig.officeorder.model.cart;

import java.math.BigDecimal;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * カート全体の金額サマリーを表す表示モデル。
 */
public record CartSummaryView(
        BigDecimal productSubtotal,
        BigDecimal assemblyFeeTotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String appliedCouponCode,
        BigDecimal couponDiscountAmount,
        String couponErrorMessage) {
    /**
     * 商品小計を画面表示用に整形する。
     */
    public String productSubtotalText() {
        return MoneyFormatter.formatYen(productSubtotal);
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
