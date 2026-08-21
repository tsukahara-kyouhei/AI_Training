package jp.co.skig.officeorder.model.coupon;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * クーポンマスタの情報を保持するデータクラス。
 *
 * @param couponId              クーポンID
 * @param couponCode            クーポンコード
 * @param discountType          割引タイプ ('fixed': 定額, 'percentage': 定率)
 * @param discountValue         割引値（金額またはパーセンテージ）
 * @param minPurchaseAmount     最低購入金額
 * @param startsAt              利用開始日時
 * @param expiresAt             利用終了日時
 * @param usageLimitTotal       全体利用回数上限 (null可)
 * @param usageLimitPerCustomer 会員別利用回数上限 (null可)
 * @param isActive              有効フラグ
 */
public record CouponForm(

        long couponId,
        String couponCode,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minPurchaseAmount,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        Integer usageLimitTotal,
        Integer usageLimitPerCustomer,
        boolean isActive) {
    /**
     * 定額割引かどうかを判定する。
     *
     * @return 定額割引の場合true
     */
    public boolean isFixed() {
        return "fixed".equals(discountType);
    }

    /**
     * 定率割引かどうかを判定する。
     *
     * @return 定率割引の場合true
     */
    public boolean isPercentage() {
        return "percentage".equals(discountType);
    }
}
