package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CatalogController の searchResults() における taste パラメーター処理に関するテスト。
 */
@ExtendWith(MockitoExtension.class)
class CatalogControllerSearchTest {

    @Mock private ProductService productService;
    @Mock private ProductListSearchService productListSearchService;
    @Mock private ProductFilterOptionService productFilterOptionService;
    @Mock private MemberService memberService;
    @Mock private MemberSessionService memberSessionService;

    private CatalogController sut;

    private static final ProductFilterOptionsBundle EMPTY_BUNDLE = new ProductFilterOptionsBundle(
            List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(),
            List.of(), List.of()
    );

    private static final ProductSearchCondition DUMMY_CONDITION = new ProductSearchCondition(
            null, null, false, List.of(), List.of(),
            ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
    );

    private static final ProductListPage EMPTY_PAGE = new ProductListPage(List.of(), 0L, 1, 15);

    @BeforeEach
    void setUp() {
        sut = new CatalogController(
                productService, productListSearchService, productFilterOptionService,
                memberService, memberSessionService
        );
    }

    @Test
    void searchResults_tasteパラメーターなしのとき_buildConditionにnullが渡されること() {
        stubCommonBehavior();

        Model model = new ConcurrentModel();
        sut.searchResults(null, false, null, null, null, null, 1, 15, model);

        ArgumentCaptor<List<String>> tasteCaptor = ArgumentCaptor.forClass(List.class);
        verify(productListSearchService).buildCondition(
                any(), any(), anyBoolean(), any(), any(),
                tasteCaptor.capture(),
                any(), anyInt(), anyInt(), any(ProductSort.class), any(ProductFilterOptionsBundle.class)
        );
        assertThat(tasteCaptor.getValue()).isNull();
    }

    @Test
    void searchResults_tasteパラメーターありのとき_buildConditionにtasteNamesが渡されること() {
        stubCommonBehavior();

        Model model = new ConcurrentModel();
        sut.searchResults(null, false, null, null, List.of("ナチュラル"), null, 1, 15, model);

        ArgumentCaptor<List<String>> tasteCaptor = ArgumentCaptor.forClass(List.class);
        verify(productListSearchService).buildCondition(
                any(), any(), anyBoolean(), any(), any(),
                tasteCaptor.capture(),
                any(), anyInt(), anyInt(), any(ProductSort.class), any(ProductFilterOptionsBundle.class)
        );
        assertThat(tasteCaptor.getValue()).containsExactly("ナチュラル");
    }

    @Test
    void searchResults_tasteOptionsがモデルに追加されること() {
        List<CategoryFilterOption> deskTastes = List.of(
                new CategoryFilterOption(1, "ナチュラル"),
                new CategoryFilterOption(2, "モダン")
        );
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), deskTastes,
                List.of(), List.of(), List.of(),
                List.of(), List.of()
        );
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        when(productListSearchService.buildCondition(
                any(), any(), anyBoolean(), any(), any(),
                nullable(List.class), any(), anyInt(), anyInt(),
                any(ProductSort.class), any(ProductFilterOptionsBundle.class)))
                .thenReturn(DUMMY_CONDITION);
        when(productListSearchService.searchWithPageCorrection(any()))
                .thenReturn(new ProductListSearchResult(DUMMY_CONDITION, EMPTY_PAGE));
        when(productListSearchService.calculateTotalPages(anyLong(), anyInt())).thenReturn(1);
        when(productFilterOptionService.allTasteDisplayNames(bundle))
                .thenReturn(List.of("ナチュラル", "モダン"));
        when(productFilterOptionService.resolveSelectedColorKeys(any(), any())).thenReturn(List.of());

        Model model = new ConcurrentModel();
        sut.searchResults(null, false, null, null, null, null, 1, 15, model);

        @SuppressWarnings("unchecked")
        List<String> tasteOptions = (List<String>) model.getAttribute("tasteOptions");
        assertThat(tasteOptions).containsExactly("ナチュラル", "モダン");
    }

    @Test
    void searchResults_selectedTasteNamesがnullのとき_空リストがモデルにセットされること() {
        stubCommonBehavior();

        Model model = new ConcurrentModel();
        sut.searchResults(null, false, null, null, null, null, 1, 15, model);

        @SuppressWarnings("unchecked")
        List<String> selected = (List<String>) model.getAttribute("selectedTasteNames");
        assertThat(selected).isEmpty();
    }

    @Test
    void searchResults_selectedTasteNamesが複数あるとき_そのままモデルにセットされること() {
        stubCommonBehavior();

        Model model = new ConcurrentModel();
        sut.searchResults(null, false, null, null, List.of("ナチュラル", "モダン"), null, 1, 15, model);

        @SuppressWarnings("unchecked")
        List<String> selected = (List<String>) model.getAttribute("selectedTasteNames");
        assertThat(selected).containsExactly("ナチュラル", "モダン");
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────

    private void stubCommonBehavior() {
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(EMPTY_BUNDLE);
        when(productListSearchService.buildCondition(
                any(), any(), anyBoolean(), any(), any(),
                nullable(List.class), any(), anyInt(), anyInt(),
                any(ProductSort.class), any(ProductFilterOptionsBundle.class)))
                .thenReturn(DUMMY_CONDITION);
        when(productListSearchService.searchWithPageCorrection(any()))
                .thenReturn(new ProductListSearchResult(DUMMY_CONDITION, EMPTY_PAGE));
        when(productListSearchService.calculateTotalPages(anyLong(), anyInt())).thenReturn(1);
        when(productFilterOptionService.allTasteDisplayNames(any())).thenReturn(List.of());
        when(productFilterOptionService.resolveSelectedColorKeys(any(), any())).thenReturn(List.of());
    }
}
