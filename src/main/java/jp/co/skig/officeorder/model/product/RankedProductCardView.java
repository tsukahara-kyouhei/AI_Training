package jp.co.skig.officeorder.model.product;

/**
 * 順位付きの売れ筋ランキング・おすすめ関連商品カード表示モデル。
 */
public record RankedProductCardView(
                int rank,
                ProductCardView product) {
}
