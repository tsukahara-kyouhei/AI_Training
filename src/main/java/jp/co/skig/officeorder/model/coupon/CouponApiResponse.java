package jp.co.skig.officeorder.model.coupon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CouponApiResponse(
        boolean success,
        CouponCartSummaryResponse cart,
        @JsonProperty("error_code") String errorCode,
        String message
) {
    public static CouponApiResponse success(CouponCartSummaryResponse cart) {
        return new CouponApiResponse(true, cart, null, null);
    }

    public static CouponApiResponse failure(String errorCode, String message) {
        return new CouponApiResponse(false, null, errorCode, message);
    }
}
