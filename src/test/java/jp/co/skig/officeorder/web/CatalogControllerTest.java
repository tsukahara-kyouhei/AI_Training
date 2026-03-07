package jp.co.skig.officeorder.web;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.product.ProductFilterOptionService;
import jp.co.skig.officeorder.service.product.ProductListSearchService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;
import static jp.co.skig.officeorder.testutil.ProductTestFixtures.emptyOptionsBundle;
import static jp.co.skig.officeorder.testutil.ProductTestFixtures.productCardView;
import static jp.co.skig.officeorder.testutil.ProductTestFixtures.productDetailView;

@WebMvcTest(controllers = CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductListSearchService productListSearchService;

    @MockitoBean
    private ProductFilterOptionService productFilterOptionService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * 新着一覧表示では、検索サービスが返した条件と一覧結果がそのままモデルへ反映されることを確認する。
     */
    @Test
    void newArrivals_rendersProductListUsingSearchResult() throws Exception {
        var optionsBundle = emptyOptionsBundle();
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                null,
                false,
                List.of(),
                List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.NEWEST,
                1,
                15,
                null
        );
        ProductListPage page = new ProductListPage(
                List.of(productCardView(1L, "Nordis ワークデスク", "/products/1")),
                1,
                1,
                15
        );
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(optionsBundle);
        when(productListSearchService.buildNewArrivalCondition(eq(false), eq(null), eq(null), eq(null), eq(1), eq(15), eq(optionsBundle)))
                .thenReturn(condition);
        when(productListSearchService.searchWithPageCorrection(condition))
                .thenReturn(new ProductListSearchResult(condition, page));
        when(productFilterOptionService.resolveSelectedColorKeys(List.of(), optionsBundle)).thenReturn(List.of());
        when(productListSearchService.calculateTotalPages(1L, 15)).thenReturn(1);

        mockMvc.perform(get("/products/new-arrivals"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/product-list-new-arrivals"))
                .andExpect(model().attribute("products", page.items()))
                .andExpect(model().attribute("totalCount", 1L))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 1));
    }

    /**
     * 商品詳細では、ログイン会員のお気に入り状態も含めて詳細モデルが表示されることを確認する。
     */
    @Test
    void productDetail_rendersDetailAndFavoriteStateForLoggedInMember() throws Exception {
        var detail = productDetailView();
        when(productService.findDetail(1L, false)).thenReturn(Optional.of(detail));
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(
                memberSessionUser(10L, "member@example.com", "山田", "花子")
        ));
        when(memberService.isFavorite(10L, 1L)).thenReturn(true);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/product-detail"))
                .andExpect(model().attribute("detail", detail))
                .andExpect(model().attribute("isFavorite", true))
                .andExpect(model().attribute("categoryLabel", "デスク"));
    }

    /**
     * 未ログインでお気に入り操作した場合は、元の詳細URLを redirect パラメータに含めてログイン画面へ送ることを確認する。
     */
    @Test
    void toggleFavorite_redirectsGuestToLoginWithEncodedReturnPath() throws Exception {
        when(productService.findDetail(1L, true)).thenReturn(Optional.of(productDetailView()));
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/products/1/favorite")
                        .param("stock", "out"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/products/1?stock%3Dout"));
    }

    /**
     * 最近見た商品 API では、不正値や重複を除去した最大4件のIDだけをサービスへ渡すことを確認する。
     */
    @Test
    void recentlyViewed_filtersMalformedAndDuplicateIdsBeforeDelegating() throws Exception {
        when(productService.findRecentlyViewedProducts(List.of(5L, 1L, 3L, 7L), 4)).thenReturn(List.of(
                productCardView(5L, "Nordis ワークデスク", "/products/5")
        ));

        mockMvc.perform(get("/products/recently-viewed")
                        .param("ids", "5,1,abc,1,-2,3,7,9"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].productId").value(5L));

        verify(productService).findRecentlyViewedProducts(List.of(5L, 1L, 3L, 7L), 4);
    }
}
