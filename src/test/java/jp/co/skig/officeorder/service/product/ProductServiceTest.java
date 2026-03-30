package jp.co.skig.officeorder.service.product;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductService の単体テスト。
 *
 * <p>商品検索条件正規化（ページサイズ・ページ番号補正）、
 * 各取得メソッドのリポジトリ委譲、limit 定数値を検証する。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private AppTimeProvider appTimeProvider;

    private ProductService productService;

    /** テスト用固定日時 */
    private static final OffsetDateTime FIXED_NOW =
            OffsetDateTime.of(2026, 3, 30, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, appTimeProvider);
    }

    // -----------------------------------------------------------------------
    // 正常系：search と条件正規化
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("search が正規化済み条件でリポジトリへ委譲し結果を返す")
    void search_delegatesToRepositoryWithNormalizedCondition_andReturnsResult() {
        ProductSearchCondition condition = buildCondition(15, 1);
        ProductListPage expectedPage = new ProductListPage(List.of(), 0, 1, 15);
        when(repository.search(any())).thenReturn(expectedPage);

        ProductListPage result = productService.search(condition);

        assertThat(result).isEqualTo(expectedPage);
        verify(repository).search(any());
    }

    @Test
    @DisplayName("許可外ページサイズが15（最小許可値）へ丸められる")
    void search_normalizesPageSizeTo15_whenInvalidSizeProvided() {
        ProductSearchCondition condition = buildCondition(20, 1);  // 20は不正
        when(repository.search(any())).thenReturn(new ProductListPage(List.of(), 0, 1, 15));

        productService.search(condition);

        ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
        verify(repository).search(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(15);
    }

    @Test
    @DisplayName("ページ番号0以下が1に補正される")
    void search_normalizesPageToOne_whenPageZeroOrNegative() {
        ProductSearchCondition condition = buildCondition(15, 0);  // page=0は不正
        when(repository.search(any())).thenReturn(new ProductListPage(List.of(), 0, 1, 15));

        productService.search(condition);

        ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
        verify(repository).search(captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // 正常系：最近見た商品
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findRecentlyViewedProducts が商品IDとlimitをリポジトリへ渡す")
    void findRecentlyViewedProducts_delegatesWithProductIdsAndLimit() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(repository.findByProductIds(eq(ids), eq(5))).thenReturn(List.of());

        productService.findRecentlyViewedProducts(ids, 5);

        verify(repository).findByProductIds(ids, 5);
    }

    // -----------------------------------------------------------------------
    // 正常系：商品詳細
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findDetail でリポジトリの結果をそのまま返す")
    void findDetail_returnsRepositoryResultAsIs() {
        Optional<ProductDetailView> expected = Optional.empty();
        when(repository.findDetail(anyLong(), anyBoolean())).thenReturn(expected);

        Optional<ProductDetailView> result = productService.findDetail(1L, false);

        assertThat(result).isSameAs(expected);
    }

    // -----------------------------------------------------------------------
    // 境界値：limit 定数値
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findTopNewArrivals がlimit=4でリポジトリを呼び出す")
    void findTopNewArrivals_callsRepositoryWithLimit4() {
        when(repository.findNewestProducts(4)).thenReturn(List.of());

        productService.findTopNewArrivals();

        verify(repository).findNewestProducts(4);
    }

    @Test
    @DisplayName("findTopRankedProducts がlimit=8でリポジトリを呼び出す")
    void findTopRankedProducts_callsRepositoryWithLimit8() {
        when(repository.findTopRankedProducts(8)).thenReturn(List.of());

        productService.findTopRankedProducts();

        verify(repository).findTopRankedProducts(8);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    private ProductSearchCondition buildCondition(int size, int page) {
        return new ProductSearchCondition(
                null, null, false,
                List.of(), List.of(), List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                page, size, null
        );
    }
}
