package jp.co.skig.officeorder.mapper.row;

public record OrderCouponRefundMapperRow(
        Long couponId,
        Long memberId,
        String orderStatus,
        boolean couponUsageRefunded
) {
}
