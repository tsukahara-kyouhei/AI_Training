package jp.co.skig.officeorder.model.product;

import java.math.BigDecimal;

public record ProductReviewSummaryView(
        BigDecimal averageRating,
        Long reviewCount) {
}
