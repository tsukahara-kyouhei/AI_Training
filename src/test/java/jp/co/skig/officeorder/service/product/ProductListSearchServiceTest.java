package jp.co.skig.officeorder.service.product;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductListSearchService の単体テスト。
 *
 * <p>検索条件組み立て時のページサイズ補正、カラーキー除外、価格帯ID除外、
 * ページ超過時の再検索、新着商品のデフォルトソート設定を検証する。
 *
 * <p>ProductService と ProductFilterOptionService は実装を使用し、
 * リポジトリ層のみ Mock で差し替える。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AppTimeProvider appTimeProvider;

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    private ProductService productService;
    private ProductFilterOptionService productFilterOptionService;
    private ProductListSearchService productListSearchService;

    private static final OffsetDateTime FIXED_NOW =
            OffsetDateTime.of(2026, 3, 30, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, appTimeProvider);
        productFilterOptionService = new ProductFilterOptionService(productFilterOptionRepository);
        productListSearchService = new ProductListSearchService(productService, productFilterOptionService);
    }

    // -----------------------------------------------------------------------
    // 正常系：条件組み立て
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buildCondition で不正ページサイズが15に補正される")
    void buildCondition_normalizesPageSizeTo15_whenInvalidSizeProvided() {
        // size=20 は許可外 → 15 に補正される
        ProductSearchCondition result = productListSearchService.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 20,
                ProductSort.RECOMMENDED,
                buildEmptyBundle()
        );

        assertThat(result.size()).isEqualTo(15);
    }

    @Test
    @DisplayName("buildCondition で許可外カラーキーが除外される")
    void buildCondition_excludesInvalidColorKeys_fromResultCondition() {
        // colorOptions に "1" だけ存在 → "invalid-color" は除外される
        ProductFilterOptionsBundle bundle = buildBundleWithColors(
                new ColorFilterOption("1", "ホワイト", "#FFFFFF")
        );

        ProductSearchCondition result = productListSearchService.buildCondition(
                null, null, false,
                List.of(), List.of("1", "invalid-color"),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                bundle
        );

        assertThat(result.colorIds()).containsExactly(1L);
    }

    @Test
    @DisplayName("buildCondition で許可外価格帯IDが除外される")
    void buildCondition_excludesInvalidPriceBandIds_fromResultCondition() {
        // PriceBand ID は 1〜6 が有効。99 は無効 → 除外
        ProductSearchCondition result = productListSearchService.buildCondition(
                null, null, false,
                List.of(1, 2, 99), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                buildEmptyBundle()
        );

        assertThat(result.priceBands()).hasSize(2);
        assertThat(result.priceBands().stream().map(pb -> pb.id()).toList())
                .containsExactlyInAnyOrder(1, 2);
    }

    // -----------------------------------------------------------------------
    // 正常系：searchWithPageCorrection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchWithPageCorrection で総件数0件はページ補正せずそのまま返す")
    void searchWithPageCorrection_returnsFirstResult_whenTotalCountIsZero() {
        ProductListPage emptyPage = new ProductListPage(List.of(), 0, 1, 15);
        when(productRepository.search(any())).thenReturn(emptyPage);

        ProductSearchCondition condition = buildSearchCondition(1, 15);
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);

        assertThat(result.productPage().totalCount()).isEqualTo(0);
        // 総件数0なら再検索しない（search は1回だけ呼ばれる）
        verify(productRepository, times(1)).search(any());
    }

    @Test
    @DisplayName("searchWithPageCorrection でページ超過時は最終ページへ再検索される")
    void searchWithPageCorrection_researchesWithLastPage_whenPageExceedsTotal() {
        // 総件数=15, ページサイズ=15 → 総ページ数=1
        // ページ3を要求 → 空リストが返り → ページ1へ再検索
        ProductListPage outOfBoundPage = new ProductListPage(List.of(), 15L, 3, 15);
        ProductListPage lastPage = new ProductListPage(List.of(), 15L, 1, 15);
        when(productRepository.search(any()))
                .thenReturn(outOfBoundPage)  // 1回目
                .thenReturn(lastPage);        // 2回目（補正後）

        ProductSearchCondition condition = buildSearchCondition(3, 15);
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);

        // 再検索が行われ、補正後条件でページ番号が1になっている
        assertThat(result.condition().page()).isEqualTo(1);
        verify(productRepository, times(2)).search(any());
    }

    // -----------------------------------------------------------------------
    // 正常系：buildNewArrivalCondition のデフォルトソート
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buildNewArrivalCondition のデフォルトソートがNEWESTになる")
    void buildNewArrivalCondition_hasDefaultSortNewest() {
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(FIXED_NOW);

        ProductSearchCondition result = productListSearchService.buildNewArrivalCondition(
                false, List.of(), List.of(),
                null,  // sort=null → デフォルト
                1, 15,
                buildEmptyBundle()
        );

        assertThat(result.sort()).isEqualTo(ProductSort.NEWEST);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    private ProductSearchCondition buildSearchCondition(int page, int size) {
        return new ProductSearchCondition(
                null, null, false,
                List.of(), List.of(), List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                page, size, null
        );
    }

    private ProductFilterOptionsBundle buildEmptyBundle() {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithColors(ColorFilterOption... colors) {
        return new ProductFilterOptionsBundle(
                List.of(colors),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
