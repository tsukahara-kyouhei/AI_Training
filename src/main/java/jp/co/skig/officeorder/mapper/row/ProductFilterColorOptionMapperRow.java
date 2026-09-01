package jp.co.skig.officeorder.mapper.row;

/**
 * 商品一覧のカラー絞り込みに表示する選択肢。
 */
public record ProductFilterColorOptionMapperRow(
                Long colorId,
                String colorName,
                String colorCode,
                String swatchType) {
}
