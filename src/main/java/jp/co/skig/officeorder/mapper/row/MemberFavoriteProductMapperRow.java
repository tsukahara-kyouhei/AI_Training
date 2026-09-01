package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * お気に入り一覧の1商品分の表示情報。
 */
public record MemberFavoriteProductMapperRow(
                Long productId,
                String productName,
                BigDecimal minPrice,
                Integer maxStock,
                String productCode) {
}
