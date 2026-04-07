package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import jp.co.skig.officeorder.service.product.ProductFilterOptionService;
import jp.co.skig.officeorder.service.product.ProductListSearchService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link CatalogController#searchResults} のスライステスト。
 */
@WebMvcTest(CatalogController.class)
class CatalogControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductListSearchService productListSearchService;

    @MockBean
    private ProductFilterOptionService productFilterOptionService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private MemberSessionService memberSessionService;

    @MockBean
    private CartService cartService;

    @MockBean
    private AnnouncementService announcementService;

    @MockBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    private static final ProductFilterOptionsBundle EMPTY_BUNDLE =
            new ProductFilterOptionsBundle(null, null, null, null, null, null, null, null,
                    List.of("ベーシック", "モダン"));

    private ProductSearchCondition emptyCondition() {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
        );
    }

    private void stubDefaults() {
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(EMPTY_BUNDLE);
        when(productFilterOptionService.buildSearchCategoryFilter(any(), any()))
                .thenReturn(ProductCategoryFilter.empty());
        ProductSearchCondition cond = emptyCondition();
        when(productListSearchService.buildCondition(
                isNull(), any(), any(boolean.class), any(), any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(cond);
        when(productListSearchService.searchWithPageCorrection(any()))
                .thenReturn(new ProductListSearchResult(cond,
                        new ProductListPage(List.of(), 0, 1, 15)));
    }

    // 6-G-1
    @Test
    @WithMockUser
    void searchResultsReturnsCorrectView() throws Exception {
        stubDefaults();
        mockMvc.perform(get("/products/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/product-list-search-results"));
    }

    // 6-G-2
    @Test
    @WithMockUser
    void tasteParamIsPassedToBuildSearchCategoryFilter() throws Exception {
        stubDefaults();
        mockMvc.perform(get("/products/search").param("taste", "ベーシック").param("taste", "モダン"))
                .andExpect(status().isOk());

        verify(productFilterOptionService).buildSearchCategoryFilter(
                eq(List.of("ベーシック", "モダン")), any());
    }

    // 6-G-3
    @Test
    @WithMockUser
    void searchTasteOptionsIsAddedToModel() throws Exception {
        stubDefaults();
        mockMvc.perform(get("/products/search"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("searchTasteOptions"));
    }

    // 6-G-4
    @Test
    @WithMockUser
    void selectedTasteNamesIsAddedToModel() throws Exception {
        stubDefaults();
        mockMvc.perform(get("/products/search"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("selectedTasteNames"));
    }

    // 6-G-5
    @Test
    @WithMockUser
    void keywordIsAddedToModel() throws Exception {
        stubDefaults();
        mockMvc.perform(get("/products/search").param("q", "デスク"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("keyword"));
    }
}
