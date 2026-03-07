package jp.co.skig.officeorder.service.product;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-07T00:00:00Z"), ZoneOffset.UTC);

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository, new AppTimeProvider(FIXED_CLOCK));

    /**
     * 新着一覧条件の組み立てで、直近6か月条件・ページ補正・表示件数補正が反映されることを確認する。
     */
    @Test
    void buildNewArrivalCondition_setsNewestSortAndSixMonthWindow() {
        ProductSearchCondition condition = service.buildNewArrivalCondition(
                true,
                List.of(1, 1, 99),
                Arrays.asList(5L, 5L, null, 7L),
                "price_desc",
                0,
                99
        );

        assertThat(condition.inStockOnly()).isTrue();
        assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_1);
        assertThat(condition.colorIds()).containsExactly(5L, 7L);
        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_DESC);
        assertThat(condition.page()).isEqualTo(1);
        assertThat(condition.size()).isEqualTo(15);
        assertThat(condition.saleStartFrom()).isEqualTo(OffsetDateTime.parse("2025-09-07T00:00:00Z"));
    }

    /**
     * 一般一覧条件の組み立てで、カテゴリ固有条件の正規化と既定並び順の補完が行われることを確認する。
     */
    @Test
    void buildCondition_normalizesCategoryFilterSortPageAndSize() {
        ProductSearchCondition condition = service.buildCondition(
                "desks",
                "  desk  ",
                false,
                Arrays.asList(2, null, 2),
                List.of(1L, 1L, 3L),
                "unknown",
                -2,
                60,
                ProductSort.NEWEST,
                new ProductCategoryFilter(
                        Arrays.asList(1, 1, null),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(condition.categoryId()).isEqualTo("desks");
        assertThat(condition.keyword()).isEqualTo("desk");
        assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_2);
        assertThat(condition.colorIds()).containsExactly(1L, 3L);
        assertThat(condition.categoryFilter().deskTopShapeIds()).containsExactly(1);
        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
        assertThat(condition.page()).isEqualTo(1);
        assertThat(condition.size()).isEqualTo(60);
    }

    /**
     * 検索実行時には、未補完の検索条件ではなく正規化済み条件がリポジトリへ渡されることを確認する。
     */
    @Test
    void search_passesNormalizedConditionToRepository() {
        when(repository.search(any())).thenReturn(new ProductListPage(List.of(), 0, 1, 15));

        service.search(new ProductSearchCondition(
                "chairs",
                "keyword",
                true,
                null,
                null,
                null,
                null,
                0,
                999,
                null
        ));

        ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
        verify(repository).search(captor.capture());
        ProductSearchCondition actual = captor.getValue();
        assertThat(actual.page()).isEqualTo(1);
        assertThat(actual.size()).isEqualTo(15);
        assertThat(actual.priceBands()).isEmpty();
        assertThat(actual.colorIds()).isEmpty();
        assertThat(actual.categoryFilter()).isEqualTo(ProductCategoryFilter.empty());
        assertThat(actual.sort()).isEqualTo(ProductSort.RECOMMENDED);
    }
}
