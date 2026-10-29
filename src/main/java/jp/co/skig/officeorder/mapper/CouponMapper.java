package jp.co.skig.officeorder.mapper;

import java.util.Optional;

import jp.co.skig.officeorder.mapper.row.CouponMapperRow;
import jp.co.skig.officeorder.mapper.row.UserCouponUsageMapperRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponMapper {
    CouponMapperRow selectByCode(@Param("code") String code);

    CouponMapperRow selectByCodeForUpdate(@Param("code") String code);

    Long selectUserUsageCount(@Param("memberId") long memberId, @Param("couponId") long couponId);

    UserCouponUsageMapperRow selectUserUsageForUpdate(@Param("memberId") long memberId,
                                                     @Param("couponId") long couponId);

    int ensureUserUsage(@Param("memberId") long memberId, @Param("couponId") long couponId);

    int incrementTotalUsage(@Param("couponId") long couponId);

    int incrementUserUsage(@Param("memberId") long memberId, @Param("couponId") long couponId);

    Long countCompletedOrders(@Param("memberId") long memberId);

}
