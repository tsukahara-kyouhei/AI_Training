package jp.co.skig.officeorder.model.member;

import java.util.List;

/**
 * お気に入り一覧のページング結果。
 */
public record MemberFavoritePage(
        List<MemberFavoriteView> items,
        long totalCount,
        int page,
        int size
) {
    /**
     * 総件数と1ページ件数から総ページ数を算出する。
     */
    public int totalPages() {
        if (totalCount <= 0) {
            return 1;
        }
        return (int) ((totalCount + size - 1) / size);
    }
}

