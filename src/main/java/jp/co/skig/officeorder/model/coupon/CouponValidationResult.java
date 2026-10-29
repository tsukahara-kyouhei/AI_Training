package jp.co.skig.officeorder.model.coupon;

import java.math.BigDecimal;

public record CouponValidationResult(
        boolean valid,
        Coupon coupon,
        BigDecimal discountAmount,
        CouponErrorCode errorCode
) {
    public static CouponValidationResult valid(Coupon coupon, BigDecimal discountAmount) {
        return new CouponValidationResult(true, coupon, discountAmount, null);
    }

    public static CouponValidationResult invalid(CouponErrorCode errorCode) {
        return new CouponValidationResult(false, null, BigDecimal.ZERO, errorCode);
    }
}
