package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.Test;

class ProductListSearchServiceTest {

        @Test
        void buildNewArrivalCondition_正常系_生入力を正規化してproductServiceへ委譲する() {
                ProductService productService = mock(ProductService.class);
                ProductFilterOptionService productFilterOptionService = mock(ProductFilterOptionService.class);
                ProductListSearchService service = new ProductListSearchService(productService,
                                productFilterOptionService);

                ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of());
                ProductSearchCondition expected = new ProductSearchCondition(
                                null,
                                null,
                                true,
                                List.of(),
                                List.of(11L),
                                ProductCategoryFilter.empty(),
                                ProductSort.NEWEST,
                                1,
                                15,
                                null);

                when(productFilterOptionService.normalizePriceBandIds(List.of(1, 2))).thenReturn(List.of(1, 2));
                when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
                when(productFilterOptionService.normalizeColorKeys(List.of("red"), bundle)).thenReturn(List.of("red"));
                when(productFilterOptionService.resolveColorIds(List.of("red"), bundle)).thenReturn(List.of(11L));
                when(productService.buildNewArrivalCondition(true, List.of(1, 2), List.of(11L), "newest", 2, 30))
                                .thenReturn(expected);

                ProductSearchCondition result = service.buildNewArrivalCondition(true, List.of(1, 2), List.of("red"),
                                "newest", 2, 30, bundle);

                assertThat(result).isSameAs(expected);
                verify(productService).buildNewArrivalCondition(true, List.of(1, 2), List.of(11L), "newest", 2, 30);
        }

        @Test
        void searchWithPageCorrection_正常系_存在しないページは最終ページへ補正する() {
                ProductService productService = mock(ProductService.class);
                ProductFilterOptionService productFilterOptionService = mock(ProductFilterOptionService.class);
                ProductListSearchService service = new ProductListSearchService(productService,
                                productFilterOptionService);

                ProductSearchCondition original = new ProductSearchCondition(
                                null,
                                "desk",
                                false,
                                List.of(),
                                List.of(),
                                ProductCategoryFilter.empty(),
                                ProductSort.RECOMMENDED,
                                3,
                                5,
                                null);
                ProductListPage firstPage = new ProductListPage(List.of(), 10, 3, 5);
                ProductListPage correctedPage = new ProductListPage(List.of(), 10, 2, 5);
                ProductSearchCondition corrected = new ProductSearchCondition(
                                null,
                                "desk",
                                false,
                                List.of(),
                                List.of(),
                                ProductCategoryFilter.empty(),
                                ProductSort.RECOMMENDED,
                                2,
                                5,
                                null);

                when(productService.search(original)).thenReturn(firstPage);
                when(productService.search(corrected)).thenReturn(correctedPage);

                ProductListSearchResult result = service.searchWithPageCorrection(original);

                assertThat(result.condition()).isEqualTo(corrected);
                assertThat(result.productPage()).isEqualTo(correctedPage);
                verify(productService).search(original);
                verify(productService).search(corrected);
        }

        @Test
        void calculateTotalPages_異常系_0件なら1を返す() {
                ProductService productService = mock(ProductService.class);
                ProductFilterOptionService productFilterOptionService = mock(ProductFilterOptionService.class);
                ProductListSearchService service = new ProductListSearchService(productService,
                                productFilterOptionService);

                assertThat(service.calculateTotalPages(0, 10)).isEqualTo(1);
        }
}
