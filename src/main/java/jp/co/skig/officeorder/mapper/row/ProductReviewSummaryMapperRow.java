package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

public record ProductReviewSummaryMapperRow(
        Long reviewCount,
        BigDecimal averageRating) {
}
