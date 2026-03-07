package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductListSearchServiceTest {

    private final ProductService productService = mock(ProductService.class);
    private final ProductFilterOptionService productFilterOptionService = mock(ProductFilterOptionService.class);
    private final ProductListSearchService service =
            new ProductListSearchService(productService, productFilterOptionService);

    /**
     * 新着一覧の検索条件構築で、価格帯とカラーの生入力値が正規化されたうえで ProductService に渡されることを確認する。
     */
    @Test
    void buildNewArrivalCondition_normalizesPriceBandsAndColorsBeforeDelegating() {
        ProductFilterOptionsBundle bundle = optionsBundle();
        ProductSearchCondition expected = new ProductSearchCondition(
                null,
                null,
                true,
                List.of(),
                List.of(1L, 2L),
                ProductCategoryFilter.empty(),
                ProductSort.NEWEST,
                1,
                15,
                null
        );
        when(productFilterOptionService.normalizePriceBandIds(List.of(1, 99))).thenReturn(List.of(1));
        when(productFilterOptionService.normalizeColorKeys(List.of("1", "99"), bundle)).thenReturn(List.of("1"));
        when(productFilterOptionService.resolveColorIds(List.of("1"), bundle)).thenReturn(List.of(1L));
        when(productService.buildNewArrivalCondition(true, List.of(1), List.of(1L), "newest", 1, 15))
                .thenReturn(expected);

        ProductSearchCondition actual = service.buildNewArrivalCondition(
                true,
                List.of(1, 99),
                List.of("1", "99"),
                "newest",
                1,
                15,
                bundle
        );

        assertThat(actual).isSameAs(expected);
        verify(productService).buildNewArrivalCondition(true, List.of(1), List.of(1L), "newest", 1, 15);
    }

    /**
     * 存在しないページ番号が指定された場合は、最終ページへ補正して再検索することを確認する。
     */
    @Test
    void searchWithPageCorrection_retriesWithLastPageWhenRequestedPageIsOutOfRange() {
        ProductSearchCondition requested = new ProductSearchCondition(
                "desks",
                null,
                false,
                List.of(),
                List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                3,
                15,
                null
        );
        ProductListPage first = new ProductListPage(List.of(), 20, 3, 15);
        ProductListPage correctedPage = new ProductListPage(List.of(), 20, 2, 15);
        when(productService.search(any()))
                .thenReturn(first)
                .thenReturn(correctedPage);

        ProductListSearchResult result = service.searchWithPageCorrection(requested);

        assertThat(result.condition().page()).isEqualTo(2);
        assertThat(result.productPage()).isSameAs(correctedPage);
    }

    /**
     * 総ページ数の計算が、0件時の下限1ページと端数切り上げの両方を満たすことを確認する。
     */
    @Test
    void calculateTotalPages_returnsOneForZeroAndRoundsUpForPositiveCounts() {
        assertThat(service.calculateTotalPages(0, 15)).isEqualTo(1);
        assertThat(service.calculateTotalPages(31, 15)).isEqualTo(3);
    }

    private ProductFilterOptionsBundle optionsBundle() {
        return new ProductFilterOptionsBundle(
                List.of(
                        new ColorFilterOption("1", "ホワイト", "#ffffff"),
                        new ColorFilterOption("2", "ブラック", "#000000")
                ),
                List.of(new CategoryFilterOption(1, "角形")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
