package jp.co.skig.officeorder.model.product;

import java.util.List;

/**
 * カテゴリ固有の絞り込み条件をまとめて保持するモデル。
 */
public record ProductCategoryFilter(
        List<Integer> deskTopShapeIds,
        List<Integer> deskWidthBandIds,
        List<Integer> deskDepthBandIds,
        List<Integer> deskHeightBandIds,
        List<Integer> deskTasteIds,
        List<Integer> chairFunctionIds,
        List<Integer> chairMaterialIds,
        List<Integer> chairTasteIds,
        List<Integer> storageUsageIds,
        List<Integer> storageTasteIds) {

    /**
     * 何も選択されていない初期状態の絞り込み条件を返す。
     */
    public static ProductCategoryFilter empty() {
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * null 排除と重複除去を行い、検索処理で扱いやすい形へ正規化する。
     */
    public ProductCategoryFilter normalize() {
        return new ProductCategoryFilter(
                normalizeList(deskTopShapeIds),
                normalizeList(deskWidthBandIds),
                normalizeList(deskDepthBandIds),
                normalizeList(deskHeightBandIds),
                normalizeList(deskTasteIds),
                normalizeList(chairFunctionIds),
                normalizeList(chairMaterialIds),
                normalizeList(chairTasteIds),
                normalizeList(storageUsageIds),
                normalizeList(storageTasteIds));
    }

    /**
     * カテゴリ固有の絞り込み条件が1つも選択されていないかを判定する。
     */
    public boolean isEmpty() {
        return deskTopShapeIds.isEmpty()
                && deskWidthBandIds.isEmpty()
                && deskDepthBandIds.isEmpty()
                && deskHeightBandIds.isEmpty()
                && deskTasteIds.isEmpty()
                && chairFunctionIds.isEmpty()
                && chairMaterialIds.isEmpty()
                && chairTasteIds.isEmpty()
                && storageUsageIds.isEmpty()
                && storageTasteIds.isEmpty();
    }

    private static List<Integer> normalizeList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null)
                .distinct()
                .toList();
    }
}
