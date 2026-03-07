package jp.co.skig.officeorder.model.product;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCategoryFilterTest {

    /**
     * 初期状態のカテゴリ絞り込み条件が、全項目未選択として構築されることを確認する。
     */
    @Test
    void empty_returnsFilterWithoutSelections() {
        ProductCategoryFilter filter = ProductCategoryFilter.empty();

        assertThat(filter.isEmpty()).isTrue();
        assertThat(filter.deskTopShapeIds()).isEmpty();
        assertThat(filter.storageTasteIds()).isEmpty();
    }

    /**
     * カテゴリ固有条件の正規化で、null 値と重複値が除去されることを確認する。
     */
    @Test
    void normalize_removesNullsAndDuplicates() {
        ProductCategoryFilter normalized = new ProductCategoryFilter(
                Arrays.asList(1, null, 1, 2),
                List.of(1, 1, 2),
                null,
                List.of(),
                null,
                List.of(3, 3),
                null,
                null,
                Arrays.asList(9, null, 9),
                List.of()
        ).normalize();

        assertThat(normalized.deskTopShapeIds()).containsExactly(1, 2);
        assertThat(normalized.deskWidthBandIds()).containsExactly(1, 2);
        assertThat(normalized.chairFunctionIds()).containsExactly(3);
        assertThat(normalized.storageUsageIds()).containsExactly(9);
    }
}
