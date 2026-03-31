package jp.co.skig.officeorder.model.product;

import java.util.List;

/**
 * 商品一覧に表示する絞り込み候補一式をまとめたモデル。
 */
public record ProductFilterOptionsBundle(
        List<ColorFilterOption> colorOptions,
        List<CategoryFilterOption> deskTopShapeOptions,
        List<CategoryFilterOption> chairFunctionOptions,
        List<CategoryFilterOption> chairMaterialOptions,
        List<CategoryFilterOption> storageUsageOptions,
        List<CategoryFilterOption> tasteOptions
) {

    public ProductFilterOptionsBundle {
        colorOptions = immutableOrEmpty(colorOptions);
        deskTopShapeOptions = immutableOrEmpty(deskTopShapeOptions);
        chairFunctionOptions = immutableOrEmpty(chairFunctionOptions);
        chairMaterialOptions = immutableOrEmpty(chairMaterialOptions);
        storageUsageOptions = immutableOrEmpty(storageUsageOptions);
        tasteOptions = immutableOrEmpty(tasteOptions);
    }

    private static <T> List<T> immutableOrEmpty(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
