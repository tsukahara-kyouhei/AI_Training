package jp.co.skig.officeorder.mapper.row;

/**
 * 同一シリーズバリエーションへのリンク表示に使う商品行。
 */
public record ProductSeriesLinkMapperRow(
        Long productId,
        String productName
) {
}
