package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * ランキングやおすすめ関連商品で使う順位付き商品行。
 */
public record ProductRankedMapperRow(
        Integer rank,
        Long productId,
        String productName,
        BigDecimal minPrice,
        Integer maxStock,
        String productCode
) {
}
