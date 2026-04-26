package jp.co.skig.officeorder.service.product;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductService} の単体テスト。
 *
 * <p>重点テスト: {@code normalize()} による page/size/sort の補正ロジック。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    AppTimeProvider appTimeProvider;

    ProductService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductService(productRepository, appTimeProvider);
    }

    // ── normalize: page補正 ──────────────────────────────────────────────

    @Test
    void ページ番号0はbuildCondition後に1に補正されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 0, 15, ProductSort.RECOMMENDED);

        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void 負のページ番号はbuildCondition後に1に補正されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", -5, 15, ProductSort.RECOMMENDED);

        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void 正のページ番号はそのまま維持されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 3, 30, ProductSort.RECOMMENDED);

        assertThat(condition.page()).isEqualTo(3);
    }

    // ── normalize: size補正 ──────────────────────────────────────────────

    @Test
    void 許可された表示件数15はそのまま通ること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(15);
    }

    @Test
    void 許可された表示件数30はそのまま通ること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 30, ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(30);
    }

    @Test
    void 許可された表示件数60はそのまま通ること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 60, ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(60);
    }

    @Test
    void 許可外の表示件数はデフォルト15に補正されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 20, ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(15);
    }

    @Test
    void 表示件数0はデフォルト15に補正されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 0, ProductSort.RECOMMENDED);

        assertThat(condition.size()).isEqualTo(15);
    }

    // ── normalize: sort補正 ──────────────────────────────────────────────

    @Test
    void sortがnullの場合はデフォルトソートが適用されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.sort()).isEqualTo(ProductSort.RECOMMENDED);
    }

    @Test
    void 無効なsort文字列はデフォルトソートに補正されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "INVALID_SORT", 1, 15, ProductSort.NEWEST);

        assertThat(condition.sort()).isEqualTo(ProductSort.NEWEST);
    }

    // ── buildCondition: keyword ──────────────────────────────────────────

    @Test
    void キーワードの前後空白はトリムされること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, "  テスト  ", false, null, null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.keyword()).isEqualTo("テスト");
    }

    @Test
    void nullキーワードはnullのまま維持されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.keyword()).isNull();
    }

    // ── buildCondition: priceBandIds ─────────────────────────────────────

    @Test
    void 有効な価格帯IDはPriceBandに変換されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, List.of(1, 2), null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.priceBands()).hasSize(2);
    }

    @Test
    void 無効な価格帯IDは除外されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, List.of(99), null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.priceBands()).isEmpty();
    }

    @Test
    void 重複した価格帯IDは1件に統一されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, List.of(1, 1), null, "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.priceBands()).hasSize(1);
    }

    // ── buildCondition: colorIds ─────────────────────────────────────────

    @Test
    void 重複したカラーIDは1件に統一されること() {
        ProductSearchCondition condition = sut.buildCondition(
                null, null, false, null, List.of(1L, 1L, 2L), "RECOMMENDED", 1, 15, ProductSort.RECOMMENDED);

        assertThat(condition.colorIds()).hasSize(2);
    }

    // ── findTopNewArrivals ────────────────────────────────────────────────

    @Test
    void findTopNewArrivalsでリポジトリが呼ばれること() {
        when(productRepository.findNewestProducts(4)).thenReturn(List.of());

        sut.findTopNewArrivals();

        verify(productRepository).findNewestProducts(4);
    }

    // ── findTopRankedProducts ─────────────────────────────────────────────

    @Test
    void findTopRankedProductsで上限8件でリポジトリが呼ばれること() {
        when(productRepository.findTopRankedProducts(8)).thenReturn(List.of());

        sut.findTopRankedProducts();

        verify(productRepository).findTopRankedProducts(8);
    }

    // ── buildNewArrivalCondition ──────────────────────────────────────────

    @Test
    void buildNewArrivalConditionで販売開始日時の下限が6ヶ月前になること() {
        OffsetDateTime now = OffsetDateTime.of(2025, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(now);

        ProductSearchCondition condition = sut.buildNewArrivalCondition(
                false, null, null, null, 1, 15);

        assertThat(condition.saleStartFrom()).isNotNull();
        assertThat(condition.saleStartFrom()).isEqualTo(now.minusMonths(6));
    }
}
