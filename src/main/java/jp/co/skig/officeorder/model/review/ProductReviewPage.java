package jp.co.skig.officeorder.model.review;

import java.util.List;

public class ProductReviewPage {

    private List<ProductReviewView> items;
    private Long totalCount;
    private Long productId;
    private int page;
    private int size;

    // --- Getters and Setters ---

    public List<ProductReviewView> getItems() {
        return items;
    }

    public void setItems(List<ProductReviewView> items) {
        this.items = items;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}