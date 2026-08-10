package jp.co.skig.officeorder.model.review;

import lombok.Data;

@Data
public class ReviewForm {
    // レビュー入力用フォーム（productId, rating, title, body）
    private Long productId;
    private Integer rating;
    private String title;
    private String body;
}