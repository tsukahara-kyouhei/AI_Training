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
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {
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
     * クーポン割引額を画面表示用に整形する。
     */
    public String discountAmountText() {
        return MoneyFormatter.formatYen(discountAmount);
    }

    /**
     * 割引が適用されているかを判定する。
     */
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
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
