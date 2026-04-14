package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductListSearchService の rawTasteNames を使った buildCondition に関するテスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceSearchTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    private ProductListSearchService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductListSearchService(productService, productFilterOptionService);
    }

    @Test
    void buildCondition_rawTasteNamesがnullのとき_resolveTasteFilterにnullが渡されること() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveTasteFilter(isNull(), eq(bundle)))
                .thenReturn(ProductCategoryFilter.empty());
        when(productService.buildCondition(
                any(), any(), anyBoolean(), any(), any(), any(), anyInt(), anyInt(),
                any(ProductSort.class), any(ProductCategoryFilter.class)))
                .thenReturn(dummyCondition());

        sut.buildCondition(null, null, false, null, null, (List<String>) null,
                null, 1, 15, ProductSort.RECOMMENDED, bundle);

        verify(productFilterOptionService).resolveTasteFilter(isNull(), eq(bundle));
    }

    @Test
    void buildCondition_rawTasteNamesにナチュラルを渡したとき_resolveTasteFilterがそれを受け取ること() {
        ProductFilterOptionsBundle bundle = bundleWithDeskTaste(1, "ナチュラル");

        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveTasteFilter(any(), any()))
                .thenReturn(ProductCategoryFilter.empty());
        when(productService.buildCondition(
                any(), any(), anyBoolean(), any(), any(), any(), anyInt(), anyInt(),
                any(ProductSort.class), any(ProductCategoryFilter.class)))
                .thenReturn(dummyCondition());

        sut.buildCondition(null, null, false, null, null, List.of("ナチュラル"),
                null, 1, 15, ProductSort.RECOMMENDED, bundle);

        verify(productFilterOptionService).resolveTasteFilter(eq(List.of("ナチュラル")), eq(bundle));
    }

    @Test
    void buildCondition_categoryFilter付きオーバーロードはrawTasteNamesを受け取らないこと() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        ProductCategoryFilter filter = ProductCategoryFilter.empty();

        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productService.buildCondition(
                any(), any(), anyBoolean(), any(), any(), any(), anyInt(), anyInt(),
                any(ProductSort.class), any(ProductCategoryFilter.class)))
                .thenReturn(dummyCondition());

        // categoryFilter 付きオーバーロードは taste パラメーター不要で呼べる
        ProductSearchCondition result = sut.buildCondition(null, null, false, null, null,
                filter, null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(result).isNotNull();
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────

    private static ProductFilterOptionsBundle emptyBundle() {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of()
        );
    }

    private static ProductFilterOptionsBundle bundleWithDeskTaste(int id, String label) {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(),
                List.of(new CategoryFilterOption(id, label)),
                List.of(), List.of(), List.of(),
                List.of(), List.of()
        );
    }

    private static ProductCategoryFilter tasteFilter(
            List<Integer> deskTasteIds,
            List<Integer> chairTasteIds,
            List<Integer> storageTasteIds) {
        return new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(), deskTasteIds,
                List.of(), List.of(), chairTasteIds, List.of(), storageTasteIds
        );
    }

    private static ProductSearchCondition dummyCondition() {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
        );
    }
}
