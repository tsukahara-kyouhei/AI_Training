package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    private ProductListSearchService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductListSearchService(productService, productFilterOptionService);
    }

    // ─── calculateTotalPages ─────────────────────────────────────────────

    @Test
    void calculateTotalPages_zero_total_count_returns_1() {
        assertThat(sut.calculateTotalPages(0, 15)).isEqualTo(1);
    }

    @Test
    void calculateTotalPages_negative_total_count_returns_1() {
        assertThat(sut.calculateTotalPages(-1, 15)).isEqualTo(1);
    }

    @Test
    void calculateTotalPages_exact_multiple_returns_correct_pages() {
        assertThat(sut.calculateTotalPages(30, 15)).isEqualTo(2);
    }

    @Test
    void calculateTotalPages_non_exact_multiple_rounds_up() {
        assertThat(sut.calculateTotalPages(31, 15)).isEqualTo(3);
    }

    @Test
    void calculateTotalPages_one_item_returns_1() {
        assertThat(sut.calculateTotalPages(1, 15)).isEqualTo(1);
    }

    // ─── searchWithPageCorrection ─────────────────────────────────────────

    @Test
    void searchWithPageCorrection_valid_page_returns_first_result_without_retry() {
        // Arrange
        ProductSearchCondition condition = buildCondition(1, 15);
        ProductListPage page = new ProductListPage(List.of(buildDummyProductCard()), 1L, 1, 15);
        when(productService.search(any())).thenReturn(page);

        // Act
        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        // Assert
        assertThat(result.productPage().items()).hasSize(1);
        verify(productService, times(1)).search(any());
    }

    @Test
    void searchWithPageCorrection_page_exceeds_total_pages_retries_with_last_page() {
        // Arrange
        ProductSearchCondition condition = buildCondition(5, 15);
        // 初回: 指定ページが存在しないため空リストが返る（総件数は15件 → 1ページのみ有効）
        ProductListPage emptyPage = new ProductListPage(List.of(), 15L, 5, 15);
        ProductListPage lastPage = new ProductListPage(List.of(buildDummyProductCard()), 15L, 1, 15);
        when(productService.search(any()))
                .thenReturn(emptyPage)
                .thenReturn(lastPage);

        // Act
        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        // Assert - 最終ページへ補正して再検索
        assertThat(result.condition().page()).isEqualTo(1);
        verify(productService, times(2)).search(any());
    }

    @Test
    void searchWithPageCorrection_empty_total_count_does_not_retry() {
        // Arrange
        ProductSearchCondition condition = buildCondition(2, 15);
        // 総件数 0 件の場合はリトライしない
        ProductListPage emptyPage = new ProductListPage(List.of(), 0L, 2, 15);
        when(productService.search(any())).thenReturn(emptyPage);

        // Act
        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        // Assert
        verify(productService, times(1)).search(any());
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private ProductSearchCondition buildCondition(int page, int size) {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, page, size, null
        );
    }

    private jp.co.skig.officeorder.model.product.ProductCardView buildDummyProductCard() {
        return new jp.co.skig.officeorder.model.product.ProductCardView(
                1L, "テスト商品", "¥10,000", List.of(), "PROD001", true, "/products/1"
        );
    }

    // ─── buildCondition (without optionsBundle) ─────────────────────────

    @Test
    void buildCondition_without_bundle_calls_loadOptionsBundle() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        org.mockito.Mockito.lenient().when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveTasteFilter(any(), any())).thenReturn(ProductCategoryFilter.empty());
        when(productService.buildCondition(any(), any(), any(Boolean.class), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(buildCondition(1, 15));

        // Act – optionsBundle = null を明示的に渡す (List<String> rawTasteNames引数ありで呼び分け)
        sut.buildCondition(null, null, false, null, null, (List<String>) null, null, 1, 15, ProductSort.RECOMMENDED, null);

        // Assert – null bundle → loadOptionsBundle() が呼ばれる
        verify(productFilterOptionService).loadOptionsBundle();
    }

    @Test
    void buildNewArrivalCondition_without_bundle_calls_loadOptionsBundle() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        org.mockito.Mockito.lenient().when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productService.buildNewArrivalCondition(any(Boolean.class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(buildCondition(1, 15));

        // Act – optionsBundle = null を明示的に渡す
        sut.buildNewArrivalCondition(false, null, null, null, 1, 15, null);

        // Assert – null bundle → loadOptionsBundle() が呼ばれる
        verify(productFilterOptionService).loadOptionsBundle();
    }

    @Test
    void buildConditionWithCategoryFilter_without_bundle_calls_loadOptionsBundle() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        org.mockito.Mockito.lenient().when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productService.buildCondition(any(), any(), any(Boolean.class), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(buildCondition(1, 15));

        // Act
        sut.buildCondition(null, null, false, null, null, ProductCategoryFilter.empty(),
                null, 1, 15, ProductSort.RECOMMENDED, null);

        verify(productFilterOptionService).loadOptionsBundle();
    }
}
