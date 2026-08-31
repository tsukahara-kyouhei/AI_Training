package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private ProductRepository repository;
    private AppTimeProvider appTimeProvider;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        appTimeProvider = mock(AppTimeProvider.class);
        service = new ProductService(repository, appTimeProvider);
    }

    @Test
    void search_正常系_検索条件をそのままrepositoryへ渡す() {
        ProductSearchCondition condition = new ProductSearchCondition(
                "desk",
                "office",
                true,
                List.of(PriceBand.BAND_2),
                List.of(10L),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                2,
                30,
                null);
        ProductListPage expected = new ProductListPage(List.of(), 0, 2, 30);
        when(repository.search(condition)).thenReturn(expected);

        ProductListPage result = service.search(condition);

        assertThat(result).isSameAs(expected);
        verify(repository).search(condition);
    }

    @Test
    void buildCondition_正常系_価格帯と色は正規化される() {
        ProductSearchCondition condition = service.buildCondition(
                "desk",
                "  office desk  ",
                true,
                Arrays.asList(1, 2, 99, null),
                Arrays.asList(10L, 10L, 20L, null),
                "price_asc",
                0,
                60,
                ProductSort.RECOMMENDED);

        assertThat(condition.keyword()).isEqualTo("office desk");
        assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_1, PriceBand.BAND_2);
        assertThat(condition.colorIds()).containsExactly(10L, 20L);
        assertThat(condition.page()).isEqualTo(1);
        assertThat(condition.size()).isEqualTo(60);
        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    @Test
    void buildNewArrivalCondition_正常系_新着条件に販売開始日下限を付与する() {
        OffsetDateTime now = OffsetDateTime.of(2024, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(now);

        ProductSearchCondition condition = service.buildNewArrivalCondition(
                true,
                List.of(3),
                List.of(11L),
                "newest",
                1,
                15);

        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
        assertThat(condition.saleStartFrom()).isEqualTo(now.minusMonths(6));
        assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_3);
        assertThat(condition.colorIds()).containsExactly(11L);
    }

    @Test
    void buildCondition_異常系_未許可サイズなら初期サイズに戻す() {
        ProductSearchCondition condition = service.buildCondition(
                null,
                null,
                false,
                List.of(),
                List.of(),
                null,
                1,
                999,
                ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(15);
        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void findDetail_正常系_商品詳細をrepositoryから返す() {
        ProductDetailView detail = mock(ProductDetailView.class);
        when(repository.findDetail(7L, true)).thenReturn(Optional.of(detail));

        Optional<ProductDetailView> result = service.findDetail(7L, true);

        assertThat(result).containsSame(detail);
        verify(repository).findDetail(7L, true);
    }

    @Test
    void findTopNewArrivals_正常系_新着商品を返す() {
        List<ProductCardView> expected = List.of(mock(ProductCardView.class));
        when(repository.findNewestProducts(4)).thenReturn(expected);

        List<ProductCardView> result = service.findTopNewArrivals();

        assertThat(result).isSameAs(expected);
    }
}
