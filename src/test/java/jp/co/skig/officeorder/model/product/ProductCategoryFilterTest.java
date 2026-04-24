package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProductCategoryFilter} の単体テスト。
 */
class ProductCategoryFilterTest {

    @Test
    void empty_全フィールドが空リストを返す() {
        ProductCategoryFilter f = ProductCategoryFilter.empty();
        assertThat(f.deskTopShapeIds()).isEmpty();
        assertThat(f.deskWidthBandIds()).isEmpty();
        assertThat(f.deskDepthBandIds()).isEmpty();
        assertThat(f.deskHeightBandIds()).isEmpty();
        assertThat(f.chairFunctionIds()).isEmpty();
        assertThat(f.chairMaterialIds()).isEmpty();
        assertThat(f.storageUsageIds()).isEmpty();
        assertThat(f.tasteIds()).isEmpty();
    }

    @Test
    void isEmpty_全フィールドが空のときtrueを返す() {
        assertThat(ProductCategoryFilter.empty().isEmpty()).isTrue();
    }

    @Test
    void isEmpty_tasteIdsに値があるときfalseを返す() {
        ProductCategoryFilter f = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(1L)
        );
        assertThat(f.isEmpty()).isFalse();
    }

    @Test
    void normalize_重複は除去される() {
        ProductCategoryFilter f = new ProductCategoryFilter(
                List.of(1, 1, 2), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(10L, 10L, 20L)
        );
        ProductCategoryFilter normalized = f.normalize();
        assertThat(normalized.deskTopShapeIds()).containsExactly(1, 2);
        assertThat(normalized.tasteIds()).containsExactly(10L, 20L);
    }

    @Test
    void normalize_nullを含むリストはnullが除去される() {
        List<Integer> withNull = Arrays.asList(1, null, 2);
        List<Long> withNullLong = Arrays.asList(10L, null, 20L);
        ProductCategoryFilter f = new ProductCategoryFilter(
                withNull, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                withNullLong
        );
        ProductCategoryFilter normalized = f.normalize();
        assertThat(normalized.deskTopShapeIds()).containsExactly(1, 2);
        assertThat(normalized.tasteIds()).containsExactly(10L, 20L);
    }
}
