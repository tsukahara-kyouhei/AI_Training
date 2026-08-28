package jp.co.skig.officeorder.model.product;

import java.util.List;

/**
 * 商品一覧、最近見た商品、関連商品で共通利用する商品カード表示モデル。
 */
public record ProductCardView(
                long productId,
                String productName,
                String priceText,
                String taxIncludedPriceText,
                List<String> colorCodes,
                String productCode,
                boolean inStock,
                String detailUrl) {
}
