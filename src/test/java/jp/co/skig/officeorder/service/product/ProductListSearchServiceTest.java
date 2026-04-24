package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService#buildCondition} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    @InjectMocks
    private ProductListSearchService service;

    private static final ProductFilterOptionsBundle EMPTY_BUNDLE =
            new ProductFilterOptionsBundle(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private static final ProductSearchCondition DUMMY_CONDITION = new ProductSearchCondition(
            null, null, false, List.of(), List.of(), List.of(),
            ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
    );

    @BeforeEach
    void setUp() {
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildCondition_nullのtasteIdsは空リストとしてproductServiceに渡される() {
        ArgumentCaptor<List<Long>> tasteCaptor = ArgumentCaptor.forClass(List.class);
        when(productService.buildCondition(
                any(), any(), anyBoolean(), anyList(), anyList(),
                any(), anyInt(), anyInt(), any(), any(), tasteCaptor.capture(), any()
        )).thenReturn(DUMMY_CONDITION);

        service.buildCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null,
                null, 1, 15, ProductSort.RECOMMENDED, EMPTY_BUNDLE
        );

        assertThat(tasteCaptor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildCondition_tasteIdsがproductServiceにそのまま渡される() {
        ArgumentCaptor<List<Long>> tasteCaptor = ArgumentCaptor.forClass(List.class);
        when(productService.buildCondition(
                any(), any(), anyBoolean(), anyList(), anyList(),
                any(), anyInt(), anyInt(), any(), any(), tasteCaptor.capture(), any()
        )).thenReturn(DUMMY_CONDITION);

        List<Long> tasteIds = List.of(1L, 2L);
        service.buildCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), tasteIds,
                null, 1, 15, ProductSort.RECOMMENDED, EMPTY_BUNDLE
        );

        assertThat(tasteCaptor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildCondition_短縮形はtasteIdsが空リストとして渡される() {
        ArgumentCaptor<List<Long>> tasteCaptor = ArgumentCaptor.forClass(List.class);
        when(productService.buildCondition(
                any(), any(), anyBoolean(), anyList(), anyList(),
                any(), anyInt(), anyInt(), any(), any(), tasteCaptor.capture(), any()
        )).thenReturn(DUMMY_CONDITION);

        service.buildCondition(
                null, null, false, List.of(), List.of(),
                null, 1, 15, ProductSort.RECOMMENDED, EMPTY_BUNDLE
        );

        assertThat(tasteCaptor.getValue()).isEmpty();
    }
}

