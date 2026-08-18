package jp.co.skig.officeorder.model.product;

import java.time.OffsetDateTime;

/**
 * 会員レビューモデル
 */
public record ProductReview(
        Long id,
        Long memberId,
        Long productId,
        Integer rating,
        String title,
        String content,
        Boolean isPublished,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}