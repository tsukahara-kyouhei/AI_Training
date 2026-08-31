package jp.co.skig.officeorder.model.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品一覧、最近見た商品、関連商品で共通利用する商品カード表示モデル。
 */
public record ProductCardView(
        Long productId,
        String productName,
        String priceText,
        String taxExcludedPriceText,
        List<String> colorCodes,
        String productCode,
        Boolean inStock,
        String detailUrl,
        BigDecimal averageRating,
        Long reviewCount
) {
}

