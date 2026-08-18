package jp.co.skig.officeorder.service.coupon;

import java.math.BigDecimal;

public interface CouponService {
    /**
     * クーポンの適用可否を判定し、割引額を計算する
     */
    BigDecimal validateAndCalculateDiscount(String couponCode, long memberId, BigDecimal cartTotal);

    /**
     * クーポンの利用実績を記録する
     */
    void recordCouponUsage(long memberId, long couponId);
}