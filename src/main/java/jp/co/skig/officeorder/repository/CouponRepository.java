package jp.co.skig.officeorder.repository;

import java.util.Optional;

import jp.co.skig.officeorder.mapper.CouponMapper;
import jp.co.skig.officeorder.mapper.row.CouponMapperRow;
import jp.co.skig.officeorder.mapper.row.UserCouponUsageMapperRow;
import jp.co.skig.officeorder.model.coupon.Coupon;
import org.springframework.stereotype.Repository;

@Repository
public class CouponRepository {
    private final CouponMapper couponMapper;

    public CouponRepository(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    public Optional<Coupon> findByCode(String code) {
        return Optional.ofNullable(couponMapper.selectByCode(code)).map(this::toCoupon);
    }

    public Optional<Coupon> findByCodeForUpdate(String code) {
        return Optional.ofNullable(couponMapper.selectByCodeForUpdate(code)).map(this::toCoupon);
    }

    public Optional<UserCouponUsageMapperRow> findUserUsage(long memberId, long couponId) {
        return Optional.ofNullable(couponMapper.selectUserUsageCount(memberId, couponId))
                .map(count -> new UserCouponUsageMapperRow(0L, memberId, couponId, count.intValue(), null));
    }

    public void ensureUserUsage(long memberId, long couponId) {
        couponMapper.ensureUserUsage(memberId, couponId);
        if (couponMapper.selectUserUsageForUpdate(memberId, couponId) == null) {
            throw new IllegalStateException("会員別クーポン利用実績のロックに失敗しました。");
        }
    }

    public long countCompletedOrders(long memberId) {
        Long count = couponMapper.countCompletedOrders(memberId);
        return count == null ? 0L : count;
    }

    public boolean incrementTotalUsage(long couponId) {
        return couponMapper.incrementTotalUsage(couponId) == 1;
    }

    public void incrementUserUsage(long memberId, long couponId) {
        if (couponMapper.incrementUserUsage(memberId, couponId) != 1) {
            throw new IllegalStateException("会員別クーポン利用実績の更新に失敗しました。");
        }
    }

    private Coupon toCoupon(CouponMapperRow row) {
        return new Coupon(
                row.id(),
                row.code(),
                row.name(),
                row.discountType(),
                row.discountValue(),
                row.maxDiscountAmount(),
                row.minPurchaseAmount(),
                row.validFrom(),
                row.validTo(),
                row.targetConditions(),
                row.maxUsageTotal(),
                row.maxUsagePerUser(),
                row.usedCountTotal(),
                row.active()
        );
    }
}
