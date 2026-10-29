package jp.co.skig.officeorder.model.coupon;

import java.util.List;

public record CouponConditions(
        List<String> allowedCategoryIds,
        List<Long> allowedProductIds,
        String targetUserType
) {
}
