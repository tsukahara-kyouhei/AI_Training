package jp.co.skig.officeorder.model.product;

import java.time.OffsetDateTime;

public record ProductReviewView(
                Long reviewId,
                Long memberId,
                Integer rating,
                String title,
                String body,
                OffsetDateTime createdAt) {
}
