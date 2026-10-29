package jp.co.skig.officeorder.model.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CouponApiRequest(
        @JsonProperty("coupon_code") String couponCode
) {
}
