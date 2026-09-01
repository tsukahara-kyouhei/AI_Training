package jp.co.skig.officeorder.mapper.row;

import java.time.OffsetDateTime;

public record ProductReviewMapperRow(
                Long reviewId,
                Long memberId,
                Long productId,
                Integer rating,
                String title,
                String body,
                Boolean published,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
}
