package jp.co.skig.officeorder.model.product;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 商品一覧検索に使用する正規化済み条件。
 */
public record ProductSearchCondition(
                String categoryId,
                String keyword,
                boolean inStockOnly,
                List<PriceBand> priceBands,
                List<Long> colorIds,
                ProductCategoryFilter categoryFilter,
                ProductCategoryFilter searchTasteFilter,
                ProductSort sort,
                int page,
                int size,
                OffsetDateTime saleStartFrom) {
}
