package jp.co.skig.officeorder.model.product;

/**
 * 商品一覧の検索条件と結果ページを組にしたモデル。
 */
public record ProductListSearchResult(
        ProductSearchCondition condition,
        ProductListPage productPage
) {
}
