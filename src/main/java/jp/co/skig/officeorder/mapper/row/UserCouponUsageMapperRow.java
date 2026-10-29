package jp.co.skig.officeorder.mapper.row;

import java.time.OffsetDateTime;

public record UserCouponUsageMapperRow(
        long id,
        long memberId,
        long couponId,
        int usageCount,
        OffsetDateTime lastUsedAt
) {
}
