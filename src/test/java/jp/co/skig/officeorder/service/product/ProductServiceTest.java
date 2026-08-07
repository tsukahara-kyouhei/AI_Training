package jp.co.skig.officeorder.service.product;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private AppTimeProvider appTimeProvider;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        appTimeProvider = Mockito.mock(AppTimeProvider.class);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(OffsetDateTime.parse("2025-01-01T12:00:00Z"));
        service = new ProductService(productRepository, appTimeProvider);
    }

    @Test
    void findTopNewArrivals_shouldDelegateToRepository() {
        List<ProductCardView> expected = List.of();
        when(productRepository.findNewestProducts(4)).thenReturn(expected);

        assertEquals(expected, service.findTopNewArrivals());
        verify(productRepository).findNewestProducts(4);
    }

    @Test
    void findTopRankedProducts_shouldDelegateToRepository() {
        List<RankedProductCardView> expected = List.of();
        when(productRepository.findTopRankedProducts(8)).thenReturn(expected);

        assertEquals(expected, service.findTopRankedProducts());
        verify(productRepository).findTopRankedProducts(8);
    }

    @Test
    void buildCondition_handlesInvalidPageAndSizeAndSort() {
        ProductSearchCondition condition = service.buildCondition(
                null,
                " keyword ",
                true,
                List.of(1, 2),
                List.of(10L, 20L),
                "unknown",
                -1,
                999,
                ProductSort.PRICE_ASC);

        assertEquals(1, condition.page());
        assertEquals(15, condition.size());
        assertEquals(ProductSort.PRICE_ASC, condition.sort());
        assertEquals("keyword", condition.keyword());
    }

    @Test
    void search_usesNormalizedConditionAndReturnsPage() {
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                null,
                false,
                List.of(),
                List.of(),
                null,
                null,
                ProductSort.RECOMMENDED,
                1,
                5,
                null);
        ProductListPage expectedPage = new ProductListPage(List.of(), 0, 1, 15);
        when(productRepository.search(any(ProductSearchCondition.class))).thenReturn(expectedPage);

        ProductListPage actualPage = service.search(condition);

        assertEquals(expectedPage, actualPage);
    }
}
