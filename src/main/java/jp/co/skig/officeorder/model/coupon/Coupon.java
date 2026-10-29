package jp.co.skig.officeorder.model.coupon;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Coupon(
        long id,
        String code,
        String name,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minPurchaseAmount,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String targetConditions,
        Long maxUsageTotal,
        Integer maxUsagePerUser,
        long usedCountTotal,
        boolean active
) {
}
