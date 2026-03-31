package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService#normalizeSearchKeyword} と
 * {@link ProductListSearchService#buildCondition(String, String, boolean, List, List, String, int, int, ProductSort, ProductFilterOptionsBundle, List)}
 * のキーワード正規化動作に関する単体テスト。
 */
class ProductListSearchServiceKeywordNormalizationTest {

    private ProductService productService;
    private ProductFilterOptionService productFilterOptionService;
    private ProductListSearchService service;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productFilterOptionService = mock(ProductFilterOptionService.class);
        service = new ProductListSearchService(productService, productFilterOptionService);

        // デフォルトのスタブ設定
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(
                new ProductFilterOptionsBundle(List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of()));
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());

        // productService.buildCondition の返値をスタブ
        when(productService.buildCondition(
                isNull(), anyString(), anyBoolean(), anyList(), anyList(),
                anyString(), anyInt(), anyInt(), any(ProductSort.class), any(), any()
        )).thenAnswer(inv -> {
            String keyword = inv.getArgument(1);
            return new ProductSearchCondition(
                    null, keyword, false, List.of(), List.of(),
                    jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                    ProductSort.RECOMMENDED, 1, 15, null, null
            );
        });
        // overload without saleStartFrom
        when(productService.buildCondition(
                isNull(), anyString(), anyBoolean(), anyList(), anyList(),
                anyString(), anyInt(), anyInt(), any(ProductSort.class), any()
        )).thenAnswer(inv -> {
            String keyword = inv.getArgument(1);
            return new ProductSearchCondition(
                    null, keyword, false, List.of(), List.of(),
                    jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                    ProductSort.RECOMMENDED, 1, 15, null, null
            );
        });
    }

    @Test
    void buildConditionWithTasteNames_halfWidthDigitsInKeyword_normalizedToFullWidth() {
        ProductSearchCondition result = service.buildCondition(
                null, "desk100", false, List.of(), List.of(),
                "recommended", 1, 15, ProductSort.RECOMMENDED, null, List.of()
        );
        // "desk100" → "ｄｅｓｋ１００"
        assertThat(result.keyword()).isEqualTo("ｄｅｓｋ１００");
    }

    @Test
    void buildConditionWithTasteNames_halfWidthKatakanaInKeyword_normalizedToFullWidth() {
        ProductSearchCondition result = service.buildCondition(
                null, "ﾅﾁｭﾗﾙ", false, List.of(), List.of(),
                "recommended", 1, 15, ProductSort.RECOMMENDED, null, List.of()
        );
        // "ﾅﾁｭﾗﾙ" → "ナチュラル"
        assertThat(result.keyword()).isEqualTo("ナチュラル");
    }

    @Test
    void buildConditionWithTasteNames_nullKeyword_remainsNull() {
        when(productService.buildCondition(
                isNull(), isNull(), anyBoolean(), anyList(), anyList(),
                anyString(), anyInt(), anyInt(), any(ProductSort.class), any()
        )).thenReturn(new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED, 1, 15, null, null
        ));

        ProductSearchCondition result = service.buildCondition(
                null, null, false, List.of(), List.of(),
                "recommended", 1, 15, ProductSort.RECOMMENDED, null, List.of()
        );
        assertThat(result.keyword()).isNull();
    }

    @Test
    void buildConditionWithTasteNames_tasteNamesSetInResult() {
        List<String> tasteNames = List.of("ナチュラル", "モダン");
        when(productService.buildCondition(
                isNull(), isNull(), anyBoolean(), anyList(), anyList(),
                anyString(), anyInt(), anyInt(), any(ProductSort.class), any()
        )).thenReturn(new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED, 1, 15, null, null
        ));

        ProductSearchCondition result = service.buildCondition(
                null, null, false, List.of(), List.of(),
                "recommended", 1, 15, ProductSort.RECOMMENDED, null, tasteNames
        );
        assertThat(result.tasteNames()).containsExactlyInAnyOrder("ナチュラル", "モダン");
    }

    @Test
    void buildConditionWithTasteNames_nullTasteNames_isEmpty() {
        when(productService.buildCondition(
                isNull(), isNull(), anyBoolean(), anyList(), anyList(),
                anyString(), anyInt(), anyInt(), any(ProductSort.class), any()
        )).thenReturn(new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED, 1, 15, null, null
        ));

        ProductSearchCondition result = service.buildCondition(
                null, null, false, List.of(), List.of(),
                "recommended", 1, 15, ProductSort.RECOMMENDED, null, null
        );
        assertThat(result.tasteNames()).isEmpty();
    }
}
