package jp.co.skig.officeorder.model.product;

import java.time.OffsetDateTime;

public record ProductReviewView(
        long reviewId,
        long memberId,
        int rating,
        String title,
        String body,
        OffsetDateTime createdAt) {
}
