package jp.co.skig.officeorder.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * カート表示と金額計算に必要な商品情報を取得する Mapper。
 */
@Mapper
public interface CartMapper {

    /**
     * カート内の商品バリアントに対応する価格・在庫・組立可否のスナップショットを取得する。
     */
    List<CartProductSnapshot> selectCartProductSnapshots(@Param("productVariantIds") List<Long> productVariantIds,
            @Param("now") OffsetDateTime now);

    /**
     * 指定時点で有効な消費税率を取得する。
     */
    BigDecimal selectCurrentTaxRatePercent(@Param("now") OffsetDateTime now);
}