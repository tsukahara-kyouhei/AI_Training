package jp.co.skig.officeorder.model.product;

/**
 * 商品詳細で表示する同一シリーズバリエーションへのリンク情報。
 */
public record ProductSeriesLinkView(
                long productId,
                String productName,
                boolean active) {
}
