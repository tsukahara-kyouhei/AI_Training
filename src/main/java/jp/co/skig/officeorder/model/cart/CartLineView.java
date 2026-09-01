package jp.co.skig.officeorder.model.cart;

import java.math.BigDecimal;

import jp.co.skig.officeorder.common.MoneyFormatter;

/**
 * カート画面に表示する1明細分の表示モデル。
 */
public record CartLineView(
        long productVariantId,
        long productId,
        String productName,
        String productCode,
        String colorName,
        BigDecimal unitPrice,
        int stockQuantity,
        boolean assemblyAvailable,
        BigDecimal assemblyFeePerUnit,
        boolean assemblyRequested,
        int quantity,
        String detailUrl) {
    /**
     * 単価を画面表示用の日本円表記へ変換する。
     */
    public String unitPriceText() {
        return MoneyFormatter.formatYen(unitPrice);
    }

    /**
     * 1点あたりの組立・設置費を画面表示用に整形する。
     */
    public String assemblyFeePerUnitText() {
        return MoneyFormatter.formatYen(assemblyFeePerUnit);
    }

    /**
     * 商品代金のみの明細小計を計算する。
     */
    public BigDecimal lineProductSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 組立・設置対象かつ希望ありの場合のみ、組立・設置費の明細小計を計算する。
     */
    public BigDecimal lineAssemblySubtotal() {
        if (!assemblyAvailable || !assemblyRequested) {
            return BigDecimal.ZERO;
        }
        return assemblyFeePerUnit.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 商品代金と組立・設置費を合算した税抜小計を返す。
     */
    public BigDecimal lineSubtotalBeforeTax() {
        return lineProductSubtotal().add(lineAssemblySubtotal());
    }

    /**
     * 税抜小計を画面表示用に整形する。
     */
    public String lineSubtotalBeforeTaxText() {
        return MoneyFormatter.formatYen(lineSubtotalBeforeTax());
    }

    /**
     * 組立・設置費小計を画面表示用に整形する。
     */
    public String lineAssemblySubtotalText() {
        return MoneyFormatter.formatYen(lineAssemblySubtotal());
    }
}
