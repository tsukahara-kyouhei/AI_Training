package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ProductCategoryFilter} のモデル検証。
 */
class ProductCategoryFilterTest {

    // 6-B-1
    @Test
    void emptyReturnsAllEmptyLists() {
        ProductCategoryFilter filter = ProductCategoryFilter.empty();
        assertThat(filter.deskTopShapeIds()).isEmpty();
        assertThat(filter.deskWidthBandIds()).isEmpty();
        assertThat(filter.deskDepthBandIds()).isEmpty();
        assertThat(filter.deskHeightBandIds()).isEmpty();
        assertThat(filter.deskTasteIds()).isEmpty();
        assertThat(filter.chairFunctionIds()).isEmpty();
        assertThat(filter.chairMaterialIds()).isEmpty();
        assertThat(filter.chairTasteIds()).isEmpty();
        assertThat(filter.storageUsageIds()).isEmpty();
        assertThat(filter.storageTasteIds()).isEmpty();
        assertThat(filter.searchTasteNames()).isEmpty();
    }

    // 6-B-2
    @Test
    void normalizeRemovesNullAndBlankFromSearchTasteNames() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                Arrays.asList("ベーシック", null, "", "モダン")
        );
        ProductCategoryFilter normalized = filter.normalize();
        assertThat(normalized.searchTasteNames()).containsExactlyInAnyOrder("ベーシック", "モダン");
    }

    // 6-B-3
    @Test
    void normalizeRemovesDuplicatesFromSearchTasteNames() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("ベーシック", "ベーシック", "モダン")
        );
        ProductCategoryFilter normalized = filter.normalize();
        assertThat(normalized.searchTasteNames()).containsExactlyInAnyOrder("ベーシック", "モダン");
        assertThat(normalized.searchTasteNames()).hasSize(2);
    }

    // 6-B-4
    @Test
    void normalizeReturnsImmutableSearchTasteNames() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new ArrayList<>(List.of("ベーシック"))
        );
        ProductCategoryFilter normalized = filter.normalize();
        assertThatThrownBy(() -> normalized.searchTasteNames().add("追加"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // 6-B-5
    @Test
    void isEmptyReturnsTrueWhenAllFieldsEmpty() {
        assertThat(ProductCategoryFilter.empty().isEmpty()).isTrue();
    }

    // 6-B-6
    @Test
    void isEmptyReturnsFalseWhenOnlySearchTasteNamesHasValues() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("ベーシック")
        );
        assertThat(filter.isEmpty()).isFalse();
    }
}
