package jp.co.skig.officeorder.model.coupon;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CouponCartSummaryResponse(
        BigDecimal subtotal,
        @JsonProperty("coupon_discount_amount") BigDecimal couponDiscountAmount,
        @JsonProperty("discounted_subtotal") BigDecimal discountedSubtotal,
        @JsonProperty("assembly_fee_total") BigDecimal assemblyFeeTotal,
        @JsonProperty("tax_amount") BigDecimal taxAmount,
        @JsonProperty("shipping_fee") BigDecimal shippingFee,
        BigDecimal total,
        @JsonProperty("applied_coupon_code") String appliedCouponCode,
        @JsonProperty("applied_coupon_name") String appliedCouponName
) {
}
