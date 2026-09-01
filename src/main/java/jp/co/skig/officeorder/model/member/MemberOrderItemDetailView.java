package jp.co.skig.officeorder.model.member;

import java.math.BigDecimal;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * 注文詳細画面に表示する明細1行分の表示モデル。
 */
public record MemberOrderItemDetailView(
        String productCode,
        String productName,
        String colorName,
        BigDecimal unitPrice,
        BigDecimal assemblyFee,
        int quantity,
        BigDecimal lineSubtotal) {
    /**
     * 単価を画面表示用に整形する。
     */
    public String unitPriceText() {
        return MoneyFormatter.formatYen(unitPrice);
    }

    /**
     * 組立・設置費を画面表示用に整形する。
     */
    public String assemblyFeeText() {
        return MoneyFormatter.formatYen(assemblyFee);
    }

    /**
     * 明細小計を画面表示用に整形する。
     */
    public String lineSubtotalText() {
        return MoneyFormatter.formatYen(lineSubtotal);
    }
}
