package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private AppTimeProvider appTimeProvider;

    @Mock
    private SearchKeywordNormalizer searchKeywordNormalizer;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, appTimeProvider, searchKeywordNormalizer);
    }

    // --- テスト用のダミーレコード生成ヘルパーメソッド ---

    private ProductCardView createDummyProductCardView() {
        return new ProductCardView(
                1L,
                "dummyCode",
                "dummyName",
                Collections.emptyList(),
                "dummyImage",
                true,
                "dummyCategory"
        );
    }

    private RankedProductCardView createDummyRankedProductCardView() {
        return new RankedProductCardView(
                1,
                createDummyProductCardView()
        );
    }

    private ProductDetailView createDummyProductDetailView() {
        return new ProductDetailView(
                1L,
                "dummyCode",
                "dummyName",
                "dummyDesc",
                true,
                10L,
                "dummyCategory",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "dummyUnit",
                BigDecimal.ZERO,
                "dummyMaker",
                BigDecimal.ZERO,
                "dummyBrand",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                true
        );
    }

    private ProductListPage createDummyProductListPage() {
        return new ProductListPage(
                List.of(createDummyProductCardView()),
                1L,
                1,
                15
        );
    }

    // --- テストケース ---

    @Nested
    @DisplayName("新着・ランキング取得テスト")
    class TopProductsTest {

        @Test
        @DisplayName("findTopNewArrivals: 新着商品を4件取得できること")
        void findTopNewArrivals() {
            List<ProductCardView> expected = List.of(createDummyProductCardView());
            when(repository.findNewestProducts(4)).thenReturn(expected);

            List<ProductCardView> actual = productService.findTopNewArrivals();

            assertThat(actual).isEqualTo(expected);
            verify(repository).findNewestProducts(4);
        }

        @Test
        @DisplayName("findTopRankedProducts: 売れ筋ランキングを8件取得できること")
        void findTopRankedProducts() {
            List<RankedProductCardView> expected = List.of(createDummyRankedProductCardView());
            when(repository.findTopRankedProducts(8)).thenReturn(expected);

            List<RankedProductCardView> actual = productService.findTopRankedProducts();

            assertThat(actual).isEqualTo(expected);
            verify(repository).findTopRankedProducts(8);
        }
    }

    @Nested
    @DisplayName("商品詳細・閲覧履歴テスト")
    class DetailAndHistoryTest {

        @Test
        @DisplayName("findDetail: 正しいパラメータでリポジトリが呼び出されること")
        void findDetail() {
            ProductDetailView detailView = createDummyProductDetailView();
            when(repository.findDetail(100L, true)).thenReturn(Optional.of(detailView));

            Optional<ProductDetailView> actual = productService.findDetail(100L, true);

            assertThat(actual).contains(detailView);
            verify(repository).findDetail(100L, true);
        }

        @Test
        @DisplayName("findRecentlyViewedProducts: 正しいパラメータでリポジトリが呼び出されること")
        void findRecentlyViewedProducts() {
            List<Long> ids = List.of(1L, 2L, 3L);
            List<ProductCardView> expected = List.of(createDummyProductCardView());
            when(repository.findByProductIds(ids, 5)).thenReturn(expected);

            List<ProductCardView> actual = productService.findRecentlyViewedProducts(ids, 5);

            assertThat(actual).isEqualTo(expected);
            verify(repository).findByProductIds(ids, 5);
        }
    }

    @Nested
    @DisplayName("検索条件構築テスト")
    class BuildConditionTest {

        @Test
        @DisplayName("buildNewArrivalCondition: 基準時刻から6か月前がセットされること")
        void buildNewArrivalCondition() {
            OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00+09:00");
            when(appTimeProvider.nowOffsetDateTime()).thenReturn(now);
            when(searchKeywordNormalizer.normalize(null)).thenReturn(null);

            ProductSearchCondition condition = productService.buildNewArrivalCondition(
                    true, List.of(1), List.of(10L), "newest", 1, 15
            );

            assertThat(condition.saleStartFrom()).isEqualTo(now.minusMonths(6));
            assertThat(condition.inStockOnly()).isTrue();
            assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
        }

        @Test
        @DisplayName("buildCondition: ページ番号と表示件数が正常に正規化されること")
        void buildCondition_normalization() {
            when(searchKeywordNormalizer.normalize(" テスト ")).thenReturn("テスト");

            ProductSearchCondition condition = productService.buildCondition(
                    "CAT_01", " テスト ", false, null, null, "invalid_sort", 0, 99, ProductSort.RECOMMENDED
            );

            assertThat(condition.page()).isEqualTo(1);
            assertThat(condition.size()).isEqualTo(15);
            assertThat(condition.keyword()).isEqualTo("テスト");
            assertThat(condition.sort()).isEqualTo(ProductSort.RECOMMENDED);
        }
    }

    @Nested
    @DisplayName("検索処理テスト")
    class SearchTest {

        @Test
        @DisplayName("search: 入力された検索条件が正規化されてリポジトリに渡されること")
        void search() {
            ProductSearchCondition rawCondition = new ProductSearchCondition(
                    "CAT_01", "keyword", true, null, null, null, null, -1, 30, null
            );
            ProductListPage expectedPage = createDummyProductListPage();
            when(repository.search(any())).thenReturn(expectedPage);

            ProductListPage actual = productService.search(rawCondition);

            assertThat(actual).isEqualTo(expectedPage);

            ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
            verify(repository).search(captor.capture());

            ProductSearchCondition normalized = captor.getValue();
            assertThat(normalized.page()).isEqualTo(1);
            assertThat(normalized.size()).isEqualTo(30);
            assertThat(normalized.sort()).isEqualTo(ProductSort.RECOMMENDED);
        }
    }
}
