package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 注文詳細に表示する注文明細の1行。
 */
public record MemberOrderItemMapperRow(
                String productCode,
                String productName,
                String colorName,
                BigDecimal unitPrice,
                BigDecimal assemblyFee,
                Integer quantity,
                BigDecimal lineSubtotal) {
}
