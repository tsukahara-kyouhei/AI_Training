package jp.co.skig.officeorder.model.review;

public class ReviewSummaryView {

    // レビュー集計表示用モデル（averageRating, reviewCount）
    private Double averageRating;
    private Long reviewCount;

    // Getters and Setters
    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }
}