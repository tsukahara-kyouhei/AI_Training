package jp.co.skig.officeorder.model.review;

import java.util.List;

/**
 * もっと見る API のレスポンス。
 */
public record ReviewListResponse(
        List<ReviewView> items,
        boolean hasNext
) {
}
