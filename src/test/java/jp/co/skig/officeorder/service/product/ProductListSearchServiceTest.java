package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    ProductService productService;

    @Mock
    ProductFilterOptionService productFilterOptionService;

    ProductListSearchService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductListSearchService(productService, productFilterOptionService);
    }

    // ── calculateTotalPages ───────────────────────────────────────────────

    @Test
    void 総件数0の場合は総ページ数1が返ること() {
        assertThat(sut.calculateTotalPages(0, 15)).isEqualTo(1);
    }

    @Test
    void 負の総件数の場合は総ページ数1が返ること() {
        assertThat(sut.calculateTotalPages(-1, 15)).isEqualTo(1);
    }

    @Test
    void 総件数がサイズと同じ場合は1ページが返ること() {
        assertThat(sut.calculateTotalPages(15, 15)).isEqualTo(1);
    }

    @Test
    void 総件数がサイズPlusOneの場合は2ページが返ること() {
        assertThat(sut.calculateTotalPages(16, 15)).isEqualTo(2);
    }

    @Test
    void 切り上げ計算で正しい総ページ数が返ること() {
        // 46件 / 15件 = 3.07... → 4ページ
        assertThat(sut.calculateTotalPages(46, 15)).isEqualTo(4);
    }

    // ── searchWithPageCorrection ──────────────────────────────────────────

    @Test
    void 通常検索では検索が1回呼ばれること() {
        ProductSearchCondition condition = buildCondition(1, 15);
        ProductListPage page = new ProductListPage(List.of(), 10L, 1, 15);
        when(productService.search(condition)).thenReturn(page);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        assertThat(result.condition()).isEqualTo(condition);
        verify(productService, times(1)).search(any());
    }

    @Test
    void 存在するページ番号の場合はページ補正が行われないこと() {
        // 1ページ目で10件あり、space.items()は空でない
        ProductSearchCondition condition = buildCondition(1, 15);
        ProductListPage page = new ProductListPage(
                List.of(new jp.co.skig.officeorder.model.product.ProductCardView(
                        1L, "テスト商品", "¥1,000", List.of(), "P001", true, "/products/1")),
                10L, 1, 15
        );
        when(productService.search(condition)).thenReturn(page);

        sut.searchWithPageCorrection(condition);

        // 補正なし: search は 1回だけ
        verify(productService, times(1)).search(any());
    }

    @Test
    void ページ超過時は最終ページへ補正して再検索されること() {
        // 第5ページを要求したが、該当ページが空でtotalCount>0
        // totalCount=10, size=15 → 1ページしかない → 第1ページへ補正
        ProductSearchCondition condition = buildCondition(5, 15);
        ProductListPage emptyPage = new ProductListPage(List.of(), 10L, 5, 15);
        ProductListPage correctedPage = new ProductListPage(List.of(), 10L, 1, 15);
        // 1回目はcondition(page=5)で呼ばれ空を返す、2回目はcorrected(page=1)で呼ばれる
        when(productService.search(any())).thenReturn(emptyPage, correctedPage);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        // 補正ありで search が2回呼ばれる
        verify(productService, times(2)).search(any());
        // 補正後の条件でpage=1になっていること
        assertThat(result.condition().page()).isEqualTo(1);
    }

    @Test
    void totalCountが0の場合はページ補正が行われないこと() {
        // totalCount=0 のとき、ページ補正しない
        ProductSearchCondition condition = buildCondition(5, 15);
        ProductListPage emptyPage = new ProductListPage(List.of(), 0L, 5, 15);
        when(productService.search(condition)).thenReturn(emptyPage);

        ProductListSearchResult result = sut.searchWithPageCorrection(condition);

        verify(productService, times(1)).search(any());
        assertThat(result.condition()).isEqualTo(condition);
    }

    // ── buildCondition ────────────────────────────────────────────────────

    @Test
    void buildConditionでpriceBandIds正規化が呼ばれること() {
        org.mockito.Mockito.lenient().when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(productFilterOptionService.loadOptionsBundle()).thenReturn(
                new jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle(
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of())
        );

        sut.buildCondition(null, null, false, List.of(1, 99), null,
                "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED, null);

        verify(productFilterOptionService).normalizePriceBandIds(List.of(1, 99));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ProductSearchCondition buildCondition(int page, int size) {
        return new ProductSearchCondition(
                null, null, false, List.of(), List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED,
                page, size, null
        );
    }
}
