package jp.co.skig.officeorder.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.product.ProductFilterOptionService;
import jp.co.skig.officeorder.service.product.ProductListSearchService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class CatalogControllerTest {

    private ProductService productService;
    private ProductListSearchService productListSearchService;
    private ProductFilterOptionService productFilterOptionService;
    private CatalogController controller;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productListSearchService = mock(ProductListSearchService.class);
        productFilterOptionService = mock(ProductFilterOptionService.class);
        MemberService memberService = mock(MemberService.class);
        MemberSessionService memberSessionService = mock(MemberSessionService.class);
        controller = new CatalogController(
                productService,
                productListSearchService,
                productFilterOptionService,
                memberService,
                memberSessionService
        );
    }

    @Test
    void searchResults_テイスト条件を組み立ててモデルへ反映する() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("101", "ホワイト", "#ffffff")),
                List.of(),
                List.of(new CategoryFilterOption(11, "モダン")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(21, "ヴィンテージ")),
                List.of(),
                List.of(new CategoryFilterOption(31, "モダン"))
        );
        ProductCategoryFilter searchTasteFilter = new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(11),
                List.of(),
                List.of(),
                List.of(21),
                List.of(),
                List.of()
        );
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                "デスク",
                true,
                List.of(),
                List.of(101L),
                searchTasteFilter,
                ProductSort.RECOMMENDED,
                1,
                15,
                null
        );
        ProductListSearchResult result = new ProductListSearchResult(condition, new ProductListPage(List.of(), 0, 1, 15));

        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        when(productFilterOptionService.normalizeSearchTasteNames(List.of("モダン", "ヴィンテージ"), bundle))
                .thenReturn(List.of("モダン", "ヴィンテージ"));
        when(productFilterOptionService.buildSearchTasteFilter(List.of("モダン", "ヴィンテージ"), bundle))
                .thenReturn(searchTasteFilter);
        when(productListSearchService.buildCondition(
                eq(null),
                eq("デスク"),
                eq(true),
                eq(List.of(1)),
                eq(List.of("101")),
                eq(searchTasteFilter),
                eq("recommended"),
                eq(1),
                eq(15),
                eq(ProductSort.RECOMMENDED),
                eq(bundle)
        )).thenReturn(condition);
        when(productListSearchService.searchWithPageCorrection(condition)).thenReturn(result);
        when(productFilterOptionService.buildSearchTasteOptions(bundle)).thenReturn(List.of("モダン", "ヴィンテージ"));
        when(productFilterOptionService.resolveSelectedColorKeys(List.of(101L), bundle)).thenReturn(List.of("101"));
        when(productListSearchService.calculateTotalPages(0, 15)).thenReturn(1);

        Model model = new ExtendedModelMap();

        String viewName = controller.searchResults(
                "デスク",
                List.of("モダン", "ヴィンテージ"),
                true,
                List.of(1),
                List.of("101"),
                "recommended",
                1,
                15,
                model
        );

        assertThat(viewName).isEqualTo("pages/product-list-search-results");
        assertThat(model.getAttribute("keyword")).isEqualTo("デスク");
        assertThat(model.getAttribute("tasteOptions")).isEqualTo(List.of("モダン", "ヴィンテージ"));
        assertThat(model.getAttribute("selectedTasteNames")).isEqualTo(List.of("モダン", "ヴィンテージ"));
        assertThat(model.getAttribute("selectedColorKeys")).isEqualTo(List.of("101"));
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);

        verify(productFilterOptionService).buildSearchTasteFilter(List.of("モダン", "ヴィンテージ"), bundle);
        verify(productListSearchService).buildCondition(
                eq(null),
                eq("デスク"),
                eq(true),
                eq(List.of(1)),
                eq(List.of("101")),
                eq(searchTasteFilter),
                eq("recommended"),
                eq(1),
                eq(15),
                eq(ProductSort.RECOMMENDED),
                eq(bundle)
        );
    }
}