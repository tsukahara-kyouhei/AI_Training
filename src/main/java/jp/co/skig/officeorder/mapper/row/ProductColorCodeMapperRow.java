package jp.co.skig.officeorder.mapper.row;

/**
 * 商品カードに表示するカラーコードの1行。
 */
public record ProductColorCodeMapperRow(
                Long productId,
                String colorCode,
                Integer sortOrder) {
}
