package jp.co.skig.officeorder.model.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品詳細画面全体の表示に必要な情報を束ねたモデル。
 */
public record ProductDetailView(
                long productId,
                String productName,
                String description,
                String categoryId,
                boolean hasVariation,
                Long variationGroupId,
                String variationName,
                BigDecimal taxRatePercent,
                BigDecimal selectedPriceExcludingTax,
                String selectedPriceExcludingTaxText,
                BigDecimal selectedPriceIncludingTax,
                String selectedPriceIncludingTaxText,
                BigDecimal selectedAssemblyFee,
                String selectedAssemblyFeeText,
                ProductVariantView selectedVariant,
                List<ProductVariantView> variants,
                List<ProductSeriesLinkView> seriesLinks,
                List<ProductCardView> relatedProducts,
                boolean outOfStock,
                BigDecimal averageRating,
                long reviewCount,
                List<ProductReviewView> reviews) {
}
