package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 商品一覧カードのベースとなる1商品分の取得結果。
 */
public record ProductListMapperRow(
        Long productId,
        String productName,
        BigDecimal minPrice,
        Integer maxStock,
        String productCode,
        BigDecimal averageRating,
        Long reviewCount) {
}
