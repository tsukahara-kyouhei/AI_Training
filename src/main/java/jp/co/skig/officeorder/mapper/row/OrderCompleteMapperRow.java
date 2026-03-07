package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 注文完了画面と完了メールのベースとなる注文情報の取得結果。
 */
public record OrderCompleteMapperRow(
        String orderNumber,
        String customerLastName,
        String customerFirstName,
        String customerEmail,
        String shippingPostalCode,
        String shippingPrefecture,
        String shippingCity,
        String shippingAddressLine,
        Integer shippingFloor,
        Boolean shippingHasElevator,
        String paymentMethod,
        Object paymentInstruction,
        BigDecimal subtotalAmount,
        BigDecimal assemblyFeeTotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount
) {
}
