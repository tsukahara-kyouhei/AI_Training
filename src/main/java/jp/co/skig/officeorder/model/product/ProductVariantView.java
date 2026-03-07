package jp.co.skig.officeorder.model.product;

import java.math.BigDecimal;

/**
 * 商品詳細で選択するカラー別商品コードの表示モデル。
 */
public record ProductVariantView(
        long productVariantId,
        String productCode,
        long colorId,
        String colorName,
        String colorCode,
        BigDecimal unitPrice,
        String unitPriceText,
        int stockQuantity
) {
}

