package jp.co.skig.officeorder.service.product;

import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private AppTimeProvider appTimeProvider;

    private ProductService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductService(repository, appTimeProvider);
    }

    // ─── buildCondition - page normalization ────────────────────────────

    @Test
    void buildCondition_page_zero_is_normalized_to_1() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 0, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_negative_page_is_normalized_to_1() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, -3, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_positive_page_is_kept_as_is() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 3, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.page()).isEqualTo(3);
    }

    // ─── buildCondition - size normalization ────────────────────────────

    @Test
    void buildCondition_size_15_is_accepted() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.size()).isEqualTo(15);
    }

    @Test
    void buildCondition_size_30_is_accepted() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 30, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.size()).isEqualTo(30);
    }

    @Test
    void buildCondition_size_60_is_accepted() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 60, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.size()).isEqualTo(60);
    }

    @Test
    void buildCondition_invalid_size_falls_back_to_15() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 999, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.size()).isEqualTo(15);
    }

    // ─── buildCondition - sort normalization ────────────────────────────

    @Test
    void buildCondition_null_sort_uses_default_sort() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.NEWEST
        );

        // Assert
        assertThat(result.sort()).isEqualTo(ProductSort.NEWEST);
    }

    @Test
    void buildCondition_invalid_sort_string_uses_default_sort() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, "invalid_sort", 1, 15, ProductSort.NEWEST
        );

        // Assert
        assertThat(result.sort()).isEqualTo(ProductSort.NEWEST);
    }

    @Test
    void buildCondition_valid_sort_string_is_resolved() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, "price_asc", 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    // ─── buildCondition - price band normalization ───────────────────────

    @Test
    void buildCondition_null_price_band_list_results_in_empty_bands() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.priceBands()).isEmpty();
    }

    @Test
    void buildCondition_invalid_price_band_id_is_filtered_out() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, List.of(0, 999), null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.priceBands()).isEmpty();
    }

    @Test
    void buildCondition_valid_price_band_ids_are_included() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, List.of(1, 2), null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.priceBands()).extracting(PriceBand::id).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void buildCondition_duplicate_price_band_ids_are_deduplicated() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, List.of(1, 1, 2), null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.priceBands()).hasSize(2);
    }

    // ─── buildCondition - keyword normalization ──────────────────────────

    @Test
    void buildCondition_keyword_with_surrounding_spaces_is_trimmed() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, "  テスト  ", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.keyword()).isEqualTo("テスト");
    }

    @Test
    void buildCondition_null_keyword_stays_null() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.keyword()).isNull();
    }

    // ─── buildCondition - color ids normalization ────────────────────────

    @Test
    void buildCondition_null_color_ids_results_in_empty_list() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.colorIds()).isEmpty();
    }

    @Test
    void buildCondition_null_element_in_color_ids_is_filtered_out() {
        // Arrange
        List<Long> colorIds = new java.util.ArrayList<>();
        colorIds.add(1L);
        colorIds.add(null);
        colorIds.add(2L);

        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, colorIds, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.colorIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void buildCondition_duplicate_color_ids_are_deduplicated() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, List.of(1L, 1L, 2L), null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert
        assertThat(result.colorIds()).hasSize(2);
    }

    // ─── buildCondition - category filter ───────────────────────────────

    @Test
    void buildCondition_null_category_filter_is_replaced_with_empty_filter() {
        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED,
                (ProductCategoryFilter) null
        );

        // Assert
        assertThat(result.categoryFilter()).isEqualTo(ProductCategoryFilter.empty());
    }

    // ─── repository delegate methods ─────────────────────────────────────

    @Test
    void findTopNewArrivals_delegates_to_repository() {
        when(repository.findNewestProducts(4)).thenReturn(List.of());
        sut.findTopNewArrivals();
        verify(repository).findNewestProducts(4);
    }

    @Test
    void findTopRankedProducts_delegates_to_repository() {
        when(repository.findTopRankedProducts(anyInt())).thenReturn(List.of());
        sut.findTopRankedProducts();
        verify(repository).findTopRankedProducts(anyInt());
    }

    @Test
    void search_delegates_to_repository_with_normalized_condition() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );
        when(repository.search(any())).thenReturn(
                new jp.co.skig.officeorder.model.product.ProductListPage(List.of(), 0L, 1, 15)
        );

        sut.search(condition);
        verify(repository).search(any());
    }

    @Test
    void findRecentlyViewedProducts_delegates_to_repository() {
        when(repository.findByProductIds(any(), anyInt())).thenReturn(List.of());
        sut.findRecentlyViewedProducts(List.of(1L, 2L), 5);
        verify(repository).findByProductIds(any(), eq(5));
    }

    @Test
    void findDetail_delegates_to_repository() {
        when(repository.findDetail(anyLong(), eq(false))).thenReturn(Optional.empty());
        sut.findDetail(10L, false);
        verify(repository).findDetail(10L, false);
    }

    @Test
    void buildNewArrivalCondition_uses_6_month_sale_start_from_cutoff() {
        // Arrange
        java.time.OffsetDateTime now = java.time.OffsetDateTime.parse("2026-04-14T09:00:00+09:00");
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(now);

        // Act
        ProductSearchCondition result = sut.buildNewArrivalCondition(
                false, null, null, null, 1, 15
        );

        // Assert – saleStartFrom が現在時刻から6か月前になっていること
        assertThat(result.saleStartFrom()).isNotNull();
        assertThat(result.saleStartFrom()).isBefore(now);
    }

    @Test
    void buildCondition_null_id_in_price_band_list_is_skipped() {
        // Arrange – null を含む priceBandIds リスト
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        ids.add(null);
        ids.add(1);

        // Act
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, ids, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        // Assert – null Id は continue でスキップされ valid なものだけ含まれる
        assertThat(result.priceBands()).extracting(PriceBand::id).containsExactly(1);
    }
}
