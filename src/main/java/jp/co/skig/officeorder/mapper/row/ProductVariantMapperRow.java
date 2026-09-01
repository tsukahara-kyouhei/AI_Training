package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 商品詳細で選択可能なカラー別商品コード情報。
 */
public record ProductVariantMapperRow(
                Long productVariantId,
                String productCode,
                Long colorId,
                String colorName,
                String colorCode,
                BigDecimal unitPrice,
                Integer stockQuantity) {
}
