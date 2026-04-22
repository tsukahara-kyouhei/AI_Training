package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    private ProductService productService;
    private ProductListSearchService sut;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);
        AppTimeProvider appTimeProvider = new AppTimeProvider(fixed);
        productService = new ProductService(productRepository, appTimeProvider);
        sut = new ProductListSearchService(productService, productFilterOptionService);
    }

    // =========================================================
    // calculateTotalPages
    // =========================================================

    // ---- TP-01: totalCount = 0 → 1ページ ----
    @Test
    @DisplayName("件数が 0 の場合は 1 ページを返す")
    void tp01_zeroCount_returnsOne() {
        assertThat(sut.calculateTotalPages(0, 15)).isEqualTo(1);
    }

    // ---- TP-02: totalCount = 負数 → 1ページ ----
    @Test
    @DisplayName("件数が負数の場合は 1 ページを返す")
    void tp02_negativeCount_returnsOne() {
        assertThat(sut.calculateTotalPages(-1, 15)).isEqualTo(1);
    }

    // ---- TP-03: ちょうど割り切れる場合 (30件/15件) → 2ページ ----
    @Test
    @DisplayName("件数がサイズで割り切れる場合は正確なページ数を返す")
    void tp03_exactMultiple_returnsExactPageCount() {
        assertThat(sut.calculateTotalPages(30, 15)).isEqualTo(2);
    }

    // ---- TP-04: 割り切れない場合 (31件/15件) → 3ページ（切り上げ）----
    @Test
    @DisplayName("件数がサイズで割り切れない場合は切り上げページ数を返す")
    void tp04_nonExactMultiple_roundsUp() {
        assertThat(sut.calculateTotalPages(31, 15)).isEqualTo(3);
    }

    // ---- TP-05: 1件/15件 → 1ページ ----
    @Test
    @DisplayName("件数が 1 件の場合は 1 ページを返す")
    void tp05_oneItem_returnsOnePage() {
        assertThat(sut.calculateTotalPages(1, 15)).isEqualTo(1);
    }

    // ---- TP-06: ちょうど上限ページ最後 (15件/15件) → 1ページ ----
    @Test
    @DisplayName("件数がちょうど 1 ページ分の場合は 1 ページを返す")
    void tp06_exactlyOnePage_returnsOne() {
        assertThat(sut.calculateTotalPages(15, 15)).isEqualTo(1);
    }

    // =========================================================
    // searchWithPageCorrection
    // =========================================================

    // ---- SW-01: ページ超過なし → そのまま返す ----
    @Test
    @DisplayName("ページ超過がない場合は補正せずそのまま返す")
    void sw01_noPageExceeded_returnsFirstResult() {
        ProductSearchCondition condition = makeCondition(1, 15);
        ProductListPage firstPage = new ProductListPage(List.of(makeCard()), 10L, 1, 15);
        when(productRepository.search(any())).thenReturn(firstPage);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        assertThat(result.productPage().items()).hasSize(1);
        verify(productRepository, times(1)).search(any());
    }

    // ---- SW-02: ページ超過 (items空 + totalCount>0 + page>totalPages) → 最終ページで再検索 ----
    @Test
    @DisplayName("ページ超過時は最終ページへ補正して再検索する")
    void sw02_pageExceeded_correctsToLastPage() {
        // page=5 で検索するが実際は 2ページ分しかない
        ProductSearchCondition condition = makeCondition(5, 15);
        // 初回: 空 (超過), totalCount=20 → 2ページ
        ProductListPage exceeded = new ProductListPage(List.of(), 20L, 5, 15);
        // 補正後: page=2 で再検索
        ProductListPage corrected = new ProductListPage(List.of(makeCard()), 20L, 2, 15);
        when(productRepository.search(any()))
                .thenReturn(exceeded)
                .thenReturn(corrected);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        // 2回検索が呼ばれる
        verify(productRepository, times(2)).search(any());
        // 補正後の条件のページ番号
        assertThat(result.condition().page()).isEqualTo(2);
    }

    // ---- SW-03: totalCount=0 のとき items が空でも補正しない ----
    @Test
    @DisplayName("totalCount が 0 の場合はページ補正しない")
    void sw03_zeroTotalCount_noCorrection() {
        ProductSearchCondition condition = makeCondition(1, 15);
        ProductListPage emptyPage = new ProductListPage(List.of(), 0L, 1, 15);
        when(productRepository.search(any())).thenReturn(emptyPage);

        sut.searchWithPageCorrection(condition);

        verify(productRepository, times(1)).search(any());
    }

    // =========================================================
    // buildNewArrivalCondition
    // =========================================================

    // ---- BN-01: productFilterOptionService の正規化が呼ばれる ----
    @Test
    @DisplayName("新着条件組み立てで価格帯・カラーの正規化が呼ばれる")
    void bn01_buildNewArrival_callsFilterNormalization() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());

        sut.buildNewArrivalCondition(false, null, null, null, 1, 15, bundle);

        verify(productFilterOptionService).normalizePriceBandIds(null);
        verify(productFilterOptionService).normalizeColorKeys(any(), any());
        verify(productFilterOptionService).resolveColorIds(any(), any());
    }

    // ---- BN-02: optionsBundle = null のとき loadOptionsBundle が呼ばれる ----
    @Test
    @DisplayName("optionsBundle が null の場合は loadOptionsBundle を呼ぶ")
    void bn02_nullOptionsBundle_callsLoadOptionsBundle() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.loadOptionsBundle()).thenReturn(bundle);
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());

        sut.buildNewArrivalCondition(false, null, null, null, 1, 15, null);

        verify(productFilterOptionService).loadOptionsBundle();
    }

    // =========================================================
    // helpers
    // =========================================================

    private ProductSearchCondition makeCondition(int page, int size) {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(), null,
                ProductSort.RECOMMENDED, page, size, null, List.of()
        );
    }

    private ProductCardView makeCard() {
        return new ProductCardView(1L, "テスト商品", "¥5,000", null, "P001", true, "/products/1");
    }
}
