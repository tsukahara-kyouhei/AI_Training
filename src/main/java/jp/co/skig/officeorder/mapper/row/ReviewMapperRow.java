package jp.co.skig.officeorder.mapper.row;

import java.time.LocalDateTime;

/**
 * レビュー取得結果の1行分のマッピング。
 */
public record ReviewMapperRow(
        long reviewId,
        long memberId,
        long productId,
        int rating,
        String title,
        String body,
        boolean isPublished,
        boolean isBlocked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
