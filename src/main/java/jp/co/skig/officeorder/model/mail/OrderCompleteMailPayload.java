package jp.co.skig.officeorder.model.mail;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 注文完了メール生成に必要な注文情報一式。
 */
public record OrderCompleteMailPayload(
                String orderNumber,
                OffsetDateTime orderDatetime,
                String customerLastName,
                String customerFirstName,
                String customerEmail,
                String shippingPostalCode,
                String shippingPrefecture,
                String shippingCity,
                String shippingAddressLine,
                Integer shippingFloor,
                Boolean shippingHasElevator,
                String daytimePhone,
                BigDecimal subtotalAmount,
                BigDecimal assemblyFeeTotal,
                BigDecimal shippingFee,
                BigDecimal taxAmount,
                BigDecimal totalAmount,
                List<OrderItemLine> orderItems) {

        /**
         * メール本文に展開する注文明細1行分。
         */
        public record OrderItemLine(
                        String productName,
                        String colorName,
                        int quantity,
                        BigDecimal unitPrice,
                        BigDecimal lineSubtotal) {
        }
}
