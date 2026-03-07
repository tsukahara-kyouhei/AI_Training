package jp.co.skig.officeorder.model.cart;

import java.math.BigDecimal;

/**
 * カート再構築時に DB から取得する商品スナップショット。
 */
public record CartProductSnapshot(
        Long productVariantId,
        Long productId,
        String productName,
        String productCode,
        String colorName,
        BigDecimal unitPrice,
        Integer stockQuantity,
        Boolean assemblyAvailable,
        BigDecimal assemblyFee
) {
}

