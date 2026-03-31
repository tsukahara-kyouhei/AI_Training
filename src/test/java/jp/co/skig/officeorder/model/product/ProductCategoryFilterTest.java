package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProductCategoryFilter} の単体テスト。
 * tasteIds フィールドの統合後の動作を検証する。
 */
class ProductCategoryFilterTest {

    // -----------------------------------------------------------------------
    // empty()
    // -----------------------------------------------------------------------

    @Test
    void empty_tasteIds_isEmpty() {
        ProductCategoryFilter filter = ProductCategoryFilter.empty();
        assertThat(filter.tasteIds()).isEmpty();
    }

    @Test
    void empty_allFieldsAreEmpty() {
        ProductCategoryFilter filter = ProductCategoryFilter.empty();
        assertThat(filter.deskTopShapeIds()).isEmpty();
        assertThat(filter.deskWidthBandIds()).isEmpty();
        assertThat(filter.deskDepthBandIds()).isEmpty();
        assertThat(filter.deskHeightBandIds()).isEmpty();
        assertThat(filter.chairFunctionIds()).isEmpty();
        assertThat(filter.chairMaterialIds()).isEmpty();
        assertThat(filter.storageUsageIds()).isEmpty();
        assertThat(filter.tasteIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // isEmpty()
    // -----------------------------------------------------------------------

    @Test
    void isEmpty_returnsTrueForEmpty() {
        assertThat(ProductCategoryFilter.empty().isEmpty()).isTrue();
    }

    @Test
    void isEmpty_returnsFalseWhenTasteIdsIsNonEmpty() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(1, 2)
        );
        assertThat(filter.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_returnsFalseWhenOnlyDeskTopShapeIdsIsNonEmpty() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(1), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of()
        );
        assertThat(filter.isEmpty()).isFalse();
    }

    // -----------------------------------------------------------------------
    // normalize()
    // -----------------------------------------------------------------------

    @Test
    void normalize_nullTasteIds_returnsEmpty() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                null
        );
        assertThat(filter.normalize().tasteIds()).isEmpty();
    }

    @Test
    void normalize_tasteIds_deduplicatesAndFiltersNull() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Arrays.asList(1, null, 1, 2)
        );
        assertThat(filter.normalize().tasteIds())
                .containsExactlyInAnyOrder(1, 2)
                .hasSize(2);
    }
}
