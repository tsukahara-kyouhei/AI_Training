package jp.co.skig.officeorder.model.review;

import java.math.BigDecimal;

/**
 * 平均評価・件数の表示用ビューモデル。
 */
public record ReviewSummaryView(
        long productId,
        int reviewCount,
        BigDecimal averageRating
) {
    /** レビューなし時のデフォルトサマリーを返す。 */
    public static ReviewSummaryView empty(long productId) {
        return new ReviewSummaryView(productId, 0, BigDecimal.ZERO);
    }
}
