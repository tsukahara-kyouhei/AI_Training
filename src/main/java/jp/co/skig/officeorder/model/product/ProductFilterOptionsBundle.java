package jp.co.skig.officeorder.model.product;

import java.util.List;

/**
 * 商品一覧に表示する絞り込み候補一式をまとめたモデル。
 */
public record ProductFilterOptionsBundle(
        List<ColorFilterOption> colorOptions,
        List<CategoryFilterOption> deskTopShapeOptions,
        List<CategoryFilterOption> deskTasteOptions,
        List<CategoryFilterOption> chairFunctionOptions,
        List<CategoryFilterOption> chairMaterialOptions,
        List<CategoryFilterOption> chairTasteOptions,
        List<CategoryFilterOption> storageUsageOptions,
        List<CategoryFilterOption> storageTasteOptions,
        List<String> searchTasteOptions
) {

    public ProductFilterOptionsBundle {
        colorOptions = immutableOrEmpty(colorOptions);
        deskTopShapeOptions = immutableOrEmpty(deskTopShapeOptions);
        deskTasteOptions = immutableOrEmpty(deskTasteOptions);
        chairFunctionOptions = immutableOrEmpty(chairFunctionOptions);
        chairMaterialOptions = immutableOrEmpty(chairMaterialOptions);
        chairTasteOptions = immutableOrEmpty(chairTasteOptions);
        storageUsageOptions = immutableOrEmpty(storageUsageOptions);
        storageTasteOptions = immutableOrEmpty(storageTasteOptions);
        searchTasteOptions = immutableOrEmpty(searchTasteOptions);
    }

    private static <T> List<T> immutableOrEmpty(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
