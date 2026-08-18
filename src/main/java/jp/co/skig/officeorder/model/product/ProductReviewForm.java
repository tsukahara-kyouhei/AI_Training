package jp.co.skig.officeorder.model.product;

/**
 * レビュー投稿・編集フォーム用モデル
 */
public record ProductReviewForm(
        Long productId,
        Integer rating,
        String title,
        String content
) {
}