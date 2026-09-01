package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * 商品詳細画面の上部表示に必要な商品基本情報。
 */
public record ProductDetailMapperRow(
        Long productId,
        String productName,
        String description,
        String categoryId,
        Boolean assemblyAvailable,
        BigDecimal assemblyFee,
        Boolean hasVariation,
        Long variationGroupId,
        String variationName,
        BigDecimal averageRating,
        Long reviewCount) {
}
