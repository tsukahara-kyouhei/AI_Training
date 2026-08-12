package jp.co.skig.officeorder.model.review;

public class ReviewForm {
    // レビュー入力用フォーム（productId, rating, title, body）
    private Long productId;
    private Integer rating;
    private String title;
    private String body;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

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