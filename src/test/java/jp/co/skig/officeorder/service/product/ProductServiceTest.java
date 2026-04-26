package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @Mock
    AppTimeProvider appTimeProvider;

    @InjectMocks
    ProductService productService;

    // --- findTopNewArrivals ---

    @Test
    void findTopNewArrivals_delegatesWithFourLimit() {
        List<ProductCardView> expected = List.of();
        when(repository.findNewestProducts(4)).thenReturn(expected);

        List<ProductCardView> result = productService.findTopNewArrivals();

        assertThat(result).isSameAs(expected);
        verify(repository).findNewestProducts(4);
    }

    // --- findTopRankedProducts ---

    @Test
    void findTopRankedProducts_delegatesWithEightLimit() {
        List<RankedProductCardView> expected = List.of();
        when(repository.findTopRankedProducts(8)).thenReturn(expected);

        List<RankedProductCardView> result = productService.findTopRankedProducts();

        assertThat(result).isSameAs(expected);
        verify(repository).findTopRankedProducts(8);
    }

    // --- search ---

    @Test
    void search_normalizesAndDelegatesToRepository() {
        ProductSearchCondition input = new ProductSearchCondition(
                "desk", null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.NEWEST, 1, 15, null);
        ProductListPage expected = new ProductListPage(List.of(), 0L, 1, 15);
        when(repository.search(any())).thenReturn(expected);

        ProductListPage result = productService.search(input);

        assertThat(result).isSameAs(expected);
        verify(repository).search(any());
    }

    // --- findDetail ---

    @Test
    void findDetail_delegatesToRepository() {
        when(repository.findDetail(101L, false)).thenReturn(Optional.empty());

        Optional<jp.co.skig.officeorder.model.product.ProductDetailView> result =
                productService.findDetail(101L, false);

        assertThat(result).isEmpty();
        verify(repository).findDetail(101L, false);
    }

    // --- findRecentlyViewedProducts ---

    @Test
    void findRecentlyViewedProducts_delegatesToRepository() {
        List<ProductCardView> expected = List.of();
        when(repository.findByProductIds(List.of(1L, 2L), 5)).thenReturn(expected);

        List<ProductCardView> result = productService.findRecentlyViewedProducts(List.of(1L, 2L), 5);

        assertThat(result).isSameAs(expected);
        verify(repository).findByProductIds(List.of(1L, 2L), 5);
    }

    // --- buildCondition (page normalization) ---

    @Test
    void buildCondition_zeroPage_normalizesToOne() {
        ProductSearchCondition condition = productService.buildCondition(
                "desk", null, false, null, null, null, 0, 15, ProductSort.NEWEST);

        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_negativePage_normalizesToOne() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, -10, 15, ProductSort.NEWEST);

        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_validPage_keepsPage() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 3, 15, ProductSort.NEWEST);

        assertThat(condition.page()).isEqualTo(3);
    }

    // --- buildCondition (size normalization) ---

    @Test
    void buildCondition_invalidSize_defaultsToFifteen() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 1, 99, ProductSort.NEWEST);

        assertThat(condition.size()).isEqualTo(15);
    }

    @Test
    void buildCondition_validSizeThirty_keepsThirty() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 1, 30, ProductSort.NEWEST);

        assertThat(condition.size()).isEqualTo(30);
    }

    // --- buildCondition (sort normalization) ---

    @Test
    void buildCondition_nullSort_usesDefaultSort() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.PRICE_ASC);

        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    @Test
    void buildCondition_invalidSortString_usesDefaultSort() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, "invalid_sort", 1, 15, ProductSort.NEWEST);

        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
    }

    @Test
    void buildCondition_validSortString_usesThatSort() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, "price_asc", 1, 15, ProductSort.NEWEST);

        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    // --- buildCondition (priceBandIds normalization) ---

    @Test
    void buildCondition_nullPriceBandIds_returnsEmptyBands() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.NEWEST);

        assertThat(condition.priceBands()).isEmpty();
    }

    @Test
    void buildCondition_validPriceBandIds_returnsBands() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, List.of(1, 2), null, null, 1, 15, ProductSort.NEWEST);

        assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_1, PriceBand.BAND_2);
    }

    @Test
    void buildCondition_duplicatePriceBandIds_deduplicates() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, List.of(1, 1, 2), null, null, 1, 15, ProductSort.NEWEST);

        assertThat(condition.priceBands()).hasSize(2);
    }

    // --- buildCondition (colorIds normalization) ---

    @Test
    void buildCondition_nullColorIds_returnsEmptyList() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.NEWEST);

        assertThat(condition.colorIds()).isEmpty();
    }

    @Test
    void buildCondition_duplicateColorIds_deduplicates() {
        ProductSearchCondition condition = productService.buildCondition(
                null, null, false, null, List.of(10L, 10L, 20L), null, 1, 15, ProductSort.NEWEST);

        assertThat(condition.colorIds()).containsExactly(10L, 20L);
    }

    // --- buildNewArrivalCondition ---

    @Test
    void buildNewArrivalCondition_setSaleStartFromToSixMonthsAgo() {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-15T09:00:00+09:00");
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(now);

        ProductSearchCondition condition = productService.buildNewArrivalCondition(
                false, null, null, null, 1, 15);

        assertThat(condition.saleStartFrom()).isEqualTo(now.minusMonths(6));
    }
}
