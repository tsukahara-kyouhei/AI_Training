package jp.co.skig.officeorder.service.coupon;

import jp.co.skig.officeorder.model.coupon.CouponErrorCode;

public class CouponValidationException extends IllegalArgumentException {
    private final CouponErrorCode errorCode;

    public CouponValidationException(CouponErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CouponErrorCode errorCode() {
        return errorCode;
    }
}
