package jp.co.skig.officeorder.model.review;

import java.time.LocalDateTime;

/**
 * レビュー表示用ビューモデル。
 */
public record ReviewView(
        long reviewId,
        long memberId,
        long productId,
        int rating,
        String title,
        String body,
        boolean published,
        boolean blocked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
