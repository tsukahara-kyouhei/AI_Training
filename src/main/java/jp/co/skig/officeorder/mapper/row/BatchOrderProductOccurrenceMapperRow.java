package jp.co.skig.officeorder.mapper.row;

/**
 * おすすめ関連商品再計算で使う、注文内の商品出現を表す1行。
 */
public record BatchOrderProductOccurrenceMapperRow(
                Long orderId,
                Long productId) {
}
