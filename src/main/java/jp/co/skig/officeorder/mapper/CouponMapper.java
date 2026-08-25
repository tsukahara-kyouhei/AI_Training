package jp.co.skig.officeorder.mapper;

import jp.co.skig.officeorder.model.coupon.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface CouponMapper {
    /**
     * クーポンコードで有効なクーポン情報を検索する
     */
    Optional<Coupon> findByCode(@Param("code") String code);
}
