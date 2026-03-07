package jp.co.skig.officeorder.model.product;

import java.util.List;

/**
 * 商品一覧の1ページ分を表す表示モデル。
 */
public record ProductListPage(
        List<ProductCardView> items,
        long totalCount,
        int page,
        int size
) {
}

