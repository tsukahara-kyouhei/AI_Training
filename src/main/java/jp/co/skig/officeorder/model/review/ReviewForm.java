package jp.co.skig.officeorder.model.review;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * レビュー投稿/編集画面のフォームモデル。
 */
public class ReviewForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{validation.review.rating.required}")
    @Min(value = 1, message = "{validation.review.rating.range}")
    @Max(value = 5, message = "{validation.review.rating.range}")
    private Integer rating;

    @Size(max = 100, message = "{validation.review.title.max}")
    private String title;

    @NotBlank(message = "{validation.review.body.required}")
    @Size(max = 1000, message = "{validation.review.body.max}")
    private String body;

    private boolean published = true;

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

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
