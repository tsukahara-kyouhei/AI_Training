package jp.co.skig.officeorder.service.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.skig.officeorder.mapper.CouponMapper;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.MemberCouponUsage;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;

    @Autowired
    public CouponServiceImpl(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    public BigDecimal validateAndCalculateDiscount(String couponCode, long memberId, BigDecimal cartTotal) {
        Coupon coupon = couponMapper.findByCode(couponCode);

        // 1. 存在確認と有効フラグチェック
        if (coupon == null || Boolean.FALSE.equals(coupon.getIsActive())) {
            throw new IllegalArgumentException("無効なクーポンです。");
        }

        // 2. 有効期間チェック
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new IllegalArgumentException("クーポンの有効期間外です。");
        }

        // 3. 最小購入金額チェック (型を安全にBigDecimalに揃えて比較)
        BigDecimal minPurchase = new BigDecimal(String.valueOf(coupon.getMinPurchaseAmount()));
        if (cartTotal.compareTo(minPurchase) < 0) {
            throw new IllegalArgumentException("最小購入金額を満たしていません。");
        }

        // 4. 利用上限回数チェック
        int usageCount = couponMapper.countUsage(memberId, coupon.getCouponId());
        if (usageCount >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("クーポンの利用上限回数に達しています。");
        }

        // 5. 割引額の計算
        String discountType = String.valueOf(coupon.getDiscountType());
        BigDecimal discountValue = new BigDecimal(String.valueOf(coupon.getDiscountValue()));

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            // 定率: cartTotal * (discountValue / 100) -> 端数切り捨て
            return cartTotal.multiply(discountValue)
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
        } else {
            // 定額: 割引額をそのまま返す
            return discountValue;
        }
    }

    @Override
    public void recordCouponUsage(long memberId, long couponId) {
        MemberCouponUsage usage = new MemberCouponUsage();
        usage.setMemberId(memberId);
        usage.setCouponId(couponId);
        usage.setUsageCount(1);

        couponMapper.insertUsage(usage);
    }
}