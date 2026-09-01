package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 購入履歴詳細のヘッダと金額サマリーを表す行。
 */
public record MemberOrderDetailMapperRow(
                String orderNumber,
                OffsetDateTime orderDatetime,
                String orderStatus,
                BigDecimal subtotalAmount,
                BigDecimal assemblyFeeTotal,
                BigDecimal shippingFee,
                BigDecimal taxAmount,
                BigDecimal totalAmount) {
}
