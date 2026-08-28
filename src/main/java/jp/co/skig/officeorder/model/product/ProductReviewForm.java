package jp.co.skig.officeorder.model.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductReviewForm {

    /** 評価 */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    /** レビュータイトル */
    @Size(max = 100)
    private String title;

    /** レビュー本文 */
    @NotNull
    private String body;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
