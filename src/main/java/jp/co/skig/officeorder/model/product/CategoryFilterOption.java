package jp.co.skig.officeorder.model.product;

/**
 * カテゴリ固有絞り込みに表示する選択肢。
 */
public record CategoryFilterOption(
        int id,
        String label
) {
}
