package jp.co.skig.officeorder.model.product;

/**
 * カラー絞り込みに表示する選択肢。
 */
public record ColorFilterOption(
        String key,
        String label,
        String colorCode
) {
}
