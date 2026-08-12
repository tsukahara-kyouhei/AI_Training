package jp.co.skig.officeorder.model.review;

import java.util.List;

public class ProductReviewPage {

    // レビュー一覧ページネーション用モデル（items, totalCount, page, size）
    private List<ReviewSummaryView> items;
    private Long totalCount;
    private int page;
    private int size;

    // Getters and Setters
    public List<ReviewSummaryView> getItems() {
        return items;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}