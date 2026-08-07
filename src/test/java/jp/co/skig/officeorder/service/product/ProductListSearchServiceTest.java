package jp.co.skig.officeorder.service.product;

import java.util.Arrays;
import java.util.List;

import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductListSearchServiceTest {

    private ProductService productService;
    private ProductFilterOptionService productFilterOptionService;
    private ProductListSearchService service;

    @BeforeEach
    void setUp() {
        productService = Mockito.mock(ProductService.class);
        productFilterOptionService = Mockito.mock(ProductFilterOptionService.class);
        service = new ProductListSearchService(productService, productFilterOptionService);
    }

    @Test
    void buildNewArrivalCondition_usesNormalizedColorAndPriceBandIds() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(productFilterOptionService.normalizePriceBandIds(Arrays.asList(1, null, 2))).thenReturn(List.of(1, 2));
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        when(productFilterOptionService.normalizeColorKeys(List.of("10", "invalid"), bundle)).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(List.of(), bundle)).thenReturn(List.of());
        when(productService.buildNewArrivalCondition(false, List.of(1, 2), List.of(), "recommended", 1, 15))
                .thenReturn(new ProductSearchCondition(
                        null, null, false, List.of(), List.of(), null, null, ProductSort.RECOMMENDED, 1, 15, null));

        ProductSearchCondition condition = service.buildNewArrivalCondition(false, Arrays.asList(1, null, 2),
                List.of("10", "invalid"), "recommended", 1, 15, null);

        assertEquals(ProductSort.RECOMMENDED, condition.sort());
        verify(productService).buildNewArrivalCondition(false, List.of(1, 2), List.of(), "recommended", 1, 15);
    }

    @Test
    void searchWithPageCorrection_requeriesWhenPageTooHigh() {
        ProductSearchCondition requested = new ProductSearchCondition(
                null, null, false, List.of(), List.of(), null, null, ProductSort.RECOMMENDED, 5, 10, null);
        ProductListPage firstPage = new ProductListPage(List.of(), 15, 5, 10);
        ProductListPage secondPage = new ProductListPage(List.of(), 15, 2, 10);
        when(productService.search(requested)).thenReturn(firstPage);
        when(productService.search(new ProductSearchCondition(
                null, null, false, List.of(), List.of(), null, null, ProductSort.RECOMMENDED, 2, 10, null)))
                .thenReturn(secondPage);

        ProductListSearchResult result = service.searchWithPageCorrection(requested);

        assertEquals(2, result.condition().page());
        assertEquals(secondPage, result.productPage());
    }
}
