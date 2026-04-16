package jp.co.skig.officeorder.service.product;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository repository;
    private AppTimeProvider appTimeProvider;
    private ProductService sut;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-17T00:00:00Z"),
            ZoneId.of("Asia/Tokyo")
    );

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        appTimeProvider = new AppTimeProvider(FIXED_CLOCK);
        sut = new ProductService(repository, appTimeProvider);
    }

    // --- findTopNewArrivals ---

    @Test
    @DisplayName("findTopNewArrivals はリポジトリに件数 4 を渡す")
    void findTopNewArrivals_callsRepositoryWithLimit4() {
        when(repository.findNewestProducts(4)).thenReturn(List.of());

        sut.findTopNewArrivals();

        verify(repository).findNewestProducts(4);
    }

    // --- findTopRankedProducts ---

    @Test
    @DisplayName("findTopRankedProducts はリポジトリに件数 8 を渡す")
    void findTopRankedProducts_callsRepositoryWithLimit8() {
        when(repository.findTopRankedProducts(8)).thenReturn(List.of());

        sut.findTopRankedProducts();

        verify(repository).findTopRankedProducts(8);
    }

    // --- buildCondition（price bands / color IDs）---

    @Test
    @DisplayName("priceBandIds が null のとき空リストになる")
    void buildCondition_nullPriceBandIds_returnsEmptyBands() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "recommended", 1, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.priceBands()).isEmpty();
    }

    @Test
    @DisplayName("重複した colorId は重複排除される")
    void buildCondition_duplicateColorIds_deduplicates() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, List.of(1L, 1L, 2L), "recommended", 1, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.colorIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("null の colorId は除外される")
    void buildCondition_nullColorIdInList_excluded() {
        List<Long> colorIds = new java.util.ArrayList<>();
        colorIds.add(1L);
        colorIds.add(null);
        colorIds.add(2L);

        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, colorIds, "recommended", 1, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.colorIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    // --- normalize（page / size）---

    @Test
    @DisplayName("page が 0 のとき 1 に正規化される")
    void buildCondition_pageZero_normalizesToOne() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "recommended", 0, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.page()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"15", "30", "60"})
    @DisplayName("許可された表示件数（15/30/60）はそのまま使われる")
    void buildCondition_allowedSize_usesAsIs(int size) {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "recommended", 1, size, ProductSort.RECOMMENDED
        );

        assertThat(condition.size()).isEqualTo(size);
    }

    @Test
    @DisplayName("許可されていない表示件数はデフォルト 15 になる")
    void buildCondition_disallowedSize_defaultsTo15() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "recommended", 1, 20, ProductSort.RECOMMENDED
        );

        assertThat(condition.size()).isEqualTo(15);
    }

    // --- keyword ---

    @Test
    @DisplayName("keyword が null のとき null のまま")
    void buildCondition_nullKeyword_remainsNull() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.keyword()).isNull();
    }

    @Test
    @DisplayName("keyword の前後空白は trim される")
    void buildCondition_keywordWithSpaces_getsTrimmed() {
        ProductSearchCondition condition = sut.buildCondition(
                null, "  椅子  ", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
        );

        assertThat(condition.keyword()).isEqualTo("椅子");
    }

    // --- sort ---

    @Test
    @DisplayName("sort が null のときデフォルト sort が使われる")
    void buildCondition_nullSort_usesDefaultSort() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.NEWEST
        );

        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
    }

    // --- buildNewArrivalCondition ---

    @Test
    @DisplayName("buildNewArrivalCondition は saleStartFrom に現在から 6 か月前をセットする")
    void buildNewArrivalCondition_setsSaleStartFrom6MonthsAgo() {
        OffsetDateTime expected = OffsetDateTime.now(FIXED_CLOCK).minusMonths(6);

        ProductSearchCondition condition = sut.buildNewArrivalCondition(
                false, null, null, "newest", 1, 15
        );

        assertThat(condition.saleStartFrom()).isEqualTo(expected);
    }

    @Test
    @DisplayName("buildNewArrivalCondition のデフォルト sort は NEWEST")
    void buildNewArrivalCondition_usesNewestAsDefaultSort() {
        ProductSearchCondition condition = sut.buildNewArrivalCondition(
                false, null, null, null, 1, 15
        );

        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
    }

    // --- search ---

    @Test
    @DisplayName("search は normalize した条件をリポジトリに渡す")
    void search_passesNormalizedConditionToRepository() {
        ProductSearchCondition input = new ProductSearchCondition(
                null, null, false, List.of(), List.of(), ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED, -1, 15, null, null
        );
        ProductListPage page = new ProductListPage(List.of(), 0L, 1, 15);
        when(repository.search(any())).thenReturn(page);

        sut.search(input);

        ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
        verify(repository).search(captor.capture());
        // page は -1 → 1 に正規化されているはず
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    @DisplayName("buildCondition は 50ms 以内に完了する")
    void buildCondition_completesWithinTimeLimit() {
        sut.buildCondition(null, "テスト", true, List.of(1, 2), List.of(1L, 2L),
                "recommended", 1, 15, ProductSort.RECOMMENDED);
    }
}
