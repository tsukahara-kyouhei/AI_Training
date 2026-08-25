package jp.co.skig.officeorder.service.coupon;

import jp.co.skig.officeorder.mapper.CouponMapper;
import jp.co.skig.officeorder.model.coupon.Coupon;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class CouponService {

    private final CouponMapper couponMapper;

    public CouponService(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    /**
     * クーポンの検証および割引額の計算を行う
     *
     * @param code クーポンコード
     * @param subtotal 小計金額
     * @return 割引金額（不適用・無効な場合は0）
     */
    public int calculateDiscount(String code, int subtotal) {
        if (code == null || code.isBlank()) {
            return 0;
        }

        Optional<Coupon> couponOpt = couponMapper.findByCode(code);
        if (couponOpt.isEmpty()) {
            return 0; // 該当するクーポンが存在しない
        }

        Coupon coupon = couponOpt.get();

        // 1. 有効フラグチェック
        if (!Boolean.TRUE.equals(coupon.isActive())) {
            return 0;
        }

        // 2. 有効期間チェック
        OffsetDateTime now = OffsetDateTime.now();
        if (coupon.startAt() != null && now.isBefore(coupon.startAt())) {
            return 0;
        }
        if (coupon.endAt() != null && now.isAfter(coupon.endAt())) {
            return 0;
        }

        // 3. 最小購入金額チェック
        if (coupon.minOrderAmount() != null && subtotal < coupon.minOrderAmount()) {
            return 0;
        }

        // 4. 割引額の計算
        if ("amount".equalsIgnoreCase(coupon.discountType())) {
            // 定額引き（小計を超えない範囲）
            return Math.min(coupon.discountValue(), subtotal);
        } else if ("rate".equalsIgnoreCase(coupon.discountType())) {
            // 定率引き（%計算）
            return (int) Math.floor(subtotal * (coupon.discountValue() / 100.0));
        }

        return 0;
    }
}