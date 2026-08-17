package jp.co.skig.officeorder.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.MemberCouponUsage;

@Mapper
public interface CouponMapper {

    Coupon findByCode(@Param("code") String code);

    int countUsage(@Param("memberId") long memberId, @Param("couponId") long couponId);

    int insertUsage(@Param("usage") MemberCouponUsage usage);

}
