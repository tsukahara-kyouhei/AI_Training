package jp.co.skig.officeorder.service.product;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService} の単体テスト（calculateTotalPages・searchWithPageCorrection 中心）。
 *
 * <p>keyword 正規化テストは ProductListSearchServiceKeywordNormalizationTest で実施済み。
 */
class ProductListSearchServiceTest {

    private ProductService productService;
    private ProductFilterOptionService productFilterOptionService;
    private ProductListSearchService sut;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productFilterOptionService = mock(ProductFilterOptionService.class);
        sut = new ProductListSearchService(productService, productFilterOptionService);
    }

    // --- calculateTotalPages ---

    @Test
    @DisplayName("総件数 0 のとき 1 を返す")
    void calculateTotalPages_zeroCounts_returnsOne() {
        assertThat(sut.calculateTotalPages(0L, 15)).isEqualTo(1);
    }

    @Test
    @DisplayName("総件数が負のとき 1 を返す")
    void calculateTotalPages_negativeCount_returnsOne() {
        assertThat(sut.calculateTotalPages(-5L, 15)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
        "15, 15, 1",    // ちょうど 1 ページ
        "16, 15, 2",    // 1 件超過で 2 ページ
        "30, 15, 2",    // ちょうど 2 ページ
        "31, 15, 3",    // 1 件超過で 3 ページ
        "1, 30, 1",     // 1 件で 1 ページ
        "60, 30, 2"     // ちょうど 2 ページ
    })
    @DisplayName("総件数と表示件数から正確なページ数を計算する")
    void calculateTotalPages_variousCounts_returnsCorrectPageCount(long totalCount, int size, int expected) {
        assertThat(sut.calculateTotalPages(totalCount, size)).isEqualTo(expected);
    }

    // --- searchWithPageCorrection ---

    @Test
    @DisplayName("ページが範囲内のとき初回検索結果をそのまま返す")
    void searchWithPageCorrection_withinPage_returnsFirstResult() {
        ProductSearchCondition condition = buildCondition(1, 15);
        ProductListPage page = new ProductListPage(List.of(mock(jp.co.skig.officeorder.model.product.ProductCardView.class)), 1L, 1, 15);
        when(productService.search(condition)).thenReturn(page);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        assertThat(result.condition()).isEqualTo(condition);
        assertThat(result.productPage()).isEqualTo(page);
        verify(productService, times(1)).search(any());
    }

    @Test
    @DisplayName("ページ超過（items が空かつ totalCount > 0）のとき最終ページで再検索する")
    void searchWithPageCorrection_pageExceedsTotal_retriesWithLastPage() {
        ProductSearchCondition condition = buildCondition(5, 15);
        // totalCount=15 → 総ページ数=1, page=5 で超過 → 最終ページ 1 で再検索
        ProductListPage overPage = new ProductListPage(List.of(), 15L, 5, 15);
        ProductListPage lastPage = new ProductListPage(
                List.of(mock(jp.co.skig.officeorder.model.product.ProductCardView.class)), 15L, 1, 15
        );
        when(productService.search(condition)).thenReturn(overPage);
        when(productService.search(any())).thenReturn(overPage, lastPage);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        assertThat(result.productPage()).isEqualTo(lastPage);
        assertThat(result.condition().page()).isEqualTo(1);
        verify(productService, times(2)).search(any());
    }

    @Test
    @DisplayName("totalCount が 0 のときページ超過でも再検索しない")
    void searchWithPageCorrection_totalCountZero_noRetry() {
        ProductSearchCondition condition = buildCondition(5, 15);
        ProductListPage emptyResult = new ProductListPage(List.of(), 0L, 5, 15);
        when(productService.search(condition)).thenReturn(emptyResult);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        assertThat(result.condition()).isEqualTo(condition);
        verify(productService, times(1)).search(any());
    }

    // --- buildNewArrivalCondition ---

    @Test
    @DisplayName("buildNewArrivalCondition は productFilterOptionService で絞り込み候補を解決してから条件を組み立てる")
    void buildNewArrivalCondition_resolvesOptionsBundle() {
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        when(productService.buildNewArrivalCondition(any(Boolean.class), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(buildCondition(1, 15));

        sut.buildNewArrivalCondition(false, null, null, null, 1, 15, null);

        verify(productService).buildNewArrivalCondition(
                false, List.of(), List.of(), null, 1, 15
        );
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    @DisplayName("calculateTotalPages は 50ms 以内に完了する")
    void calculateTotalPages_completesWithinTimeLimit() {
        for (int i = 0; i < 10000; i++) {
            sut.calculateTotalPages(i * 7L, 15);
        }
    }

    // --- ヘルパ ---

    private ProductSearchCondition buildCondition(int page, int size) {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                page, size, null, List.of()
        );
    }
}
