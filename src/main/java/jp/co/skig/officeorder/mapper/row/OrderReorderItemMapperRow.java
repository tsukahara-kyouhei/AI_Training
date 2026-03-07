package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 再購入時にカートへ積み直すための注文明細情報。
 */
public record OrderReorderItemMapperRow(
        Long productVariantId,
        String productCode,
        Integer quantity,
        Boolean assemblyAvailable,
        BigDecimal assemblyFee
) {
}
