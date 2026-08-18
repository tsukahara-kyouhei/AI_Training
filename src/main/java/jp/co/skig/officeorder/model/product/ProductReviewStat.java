package jp.co.skig.officeorder.model.product;

/**
 * レビュー集計情報モデル（平均評価・件数）
 */
public record ProductReviewStat(
        Long productId,
        Double averageRating,
        Long reviewCount
) {
}