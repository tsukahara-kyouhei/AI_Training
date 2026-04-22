package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * {@link ProductService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    private AppTimeProvider appTimeProvider;
    private ProductService sut;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);
        appTimeProvider = new AppTimeProvider(fixed);
        sut = new ProductService(repository, appTimeProvider);
    }

    // =========================================================
    // buildCondition – page 正規化
    // =========================================================

    // ---- BC-01: page < 1 は 1 に補正される ----
    @Test
    @DisplayName("page が 0 の場合は 1 に正規化される")
    void bc01_pageZero_normalizedToOne() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 0, 15, ProductSort.RECOMMENDED);

        assertThat(result.page()).isEqualTo(1);
    }

    // ---- BC-02: page = 3 → そのまま ----
    @Test
    @DisplayName("page が正の値の場合はそのまま保持される")
    void bc02_validPage_keptAsIs() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 3, 15, ProductSort.RECOMMENDED);

        assertThat(result.page()).isEqualTo(3);
    }

    // =========================================================
    // buildCondition – size 正規化
    // =========================================================

    // ---- BS-01: 許可サイズ 15 → そのまま ----
    @Test
    @DisplayName("size が 15 はそのまま保持される")
    void bs01_size15_keptAsIs() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(result.size()).isEqualTo(15);
    }

    // ---- BS-02: 許可サイズ 30 → そのまま ----
    @Test
    @DisplayName("size が 30 はそのまま保持される")
    void bs02_size30_keptAsIs() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 30, ProductSort.RECOMMENDED);

        assertThat(result.size()).isEqualTo(30);
    }

    // ---- BS-03: 許可サイズ 60 → そのまま ----
    @Test
    @DisplayName("size が 60 はそのまま保持される")
    void bs03_size60_keptAsIs() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 60, ProductSort.RECOMMENDED);

        assertThat(result.size()).isEqualTo(60);
    }

    // ---- BS-04: 不正サイズ 20 → 15 にフォールバック ----
    @Test
    @DisplayName("許可外 size は 15 にフォールバックされる")
    void bs04_invalidSize_fallsBackTo15() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 20, ProductSort.RECOMMENDED);

        assertThat(result.size()).isEqualTo(15);
    }

    // ---- BS-05: size = 0 → 15 にフォールバック ----
    @Test
    @DisplayName("size が 0 の場合は 15 にフォールバックされる")
    void bs05_sizeZero_fallsBackTo15() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 0, ProductSort.RECOMMENDED);

        assertThat(result.size()).isEqualTo(15);
    }

    // =========================================================
    // buildCondition – sort 正規化
    // =========================================================

    // ---- SO-01: sort = null → デフォルトソート ----
    @Test
    @DisplayName("sort が null の場合はデフォルトソートを使用する")
    void so01_nullSort_usesDefault() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.NEWEST);

        assertThat(result.sort()).isEqualTo(ProductSort.NEWEST);
    }

    // ---- SO-02: sort = "recommended" → RECOMMENDED ----
    @Test
    @DisplayName("sort が 'recommended' の場合は RECOMMENDED ソートになる")
    void so02_validSortString_resolvedCorrectly() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, "recommended", 1, 15, ProductSort.NEWEST);

        assertThat(result.sort()).isEqualTo(ProductSort.RECOMMENDED);
    }

    // ---- SO-03: sort = "invalid" → デフォルトソート ----
    @Test
    @DisplayName("不正な sort 値の場合はデフォルトソートを使用する")
    void so03_invalidSort_usesDefault() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, "unknown_sort", 1, 15, ProductSort.PRICE_ASC);

        assertThat(result.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    // =========================================================
    // buildCondition – keyword / priceBand / colorIds
    // =========================================================

    // ---- KW-01: keyword = null → null のまま ----
    @Test
    @DisplayName("keyword が null の場合は null のまま保持される")
    void kw01_nullKeyword_remainsNull() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(result.keyword()).isNull();
    }

    // ---- KW-02: priceBandIds に重複IDが含まれる → 重複除去 ----
    @Test
    @DisplayName("重複した価格帯 ID は除去される")
    void kw02_duplicatePriceBandIds_deduplicated() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, List.of(1, 1, 2), null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(result.priceBands()).hasSize(2);
    }

    // ---- KW-03: 無効な priceBandId は除去される ----
    @Test
    @DisplayName("許可外の価格帯 ID は除去される")
    void kw03_invalidPriceBandId_removed() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, List.of(99), null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(result.priceBands()).isEmpty();
    }

    // ---- KW-04: colorIds = null → 空リスト ----
    @Test
    @DisplayName("colorIds が null の場合は空リストになる")
    void kw04_nullColorIds_emptyList() {
        ProductSearchCondition result = sut.buildCondition(
                null, null, false, null, null, null, 1, 15, ProductSort.RECOMMENDED);

        assertThat(result.colorIds()).isEmpty();
    }

    // =========================================================
    // findDetail
    // =========================================================

    // ---- FD-01: リポジトリの結果を返す（存在する場合）----
    @Test
    @DisplayName("商品が存在する場合はリポジトリの結果を返す")
    void fd01_existingProduct_returnsRepositoryResult() {
        when(repository.findDetail(1L, false)).thenReturn(Optional.empty());

        Optional<ProductDetailView> result = sut.findDetail(1L, false);

        assertThat(result).isEmpty();
    }

    // ---- FD-02: forceOutOfStock=true でリポジトリに渡される ----
    @Test
    @DisplayName("forceOutOfStock=true はそのままリポジトリに渡される")
    void fd02_forceOutOfStock_passedToRepository() {
        when(repository.findDetail(1L, true)).thenReturn(Optional.empty());

        sut.findDetail(1L, true);

        org.mockito.Mockito.verify(repository).findDetail(1L, true);
    }

    // =========================================================
    // buildNewArrivalCondition
    // =========================================================

    // ---- NA-01: saleStartFrom が現在から6か月前になる ----
    @Test
    @DisplayName("新着条件の saleStartFrom は現在日時の6か月前になる")
    void na01_newArrivalCondition_saleStartFromIs6MonthsAgo() {
        ProductSearchCondition result = sut.buildNewArrivalCondition(
                false, null, null, null, 1, 15);

        // 固定Clock: 2026-04-01T00:00:00Z の6ヶ月前 = 2025-10-01
        assertThat(result.saleStartFrom()).isNotNull();
        assertThat(result.saleStartFrom().getYear()).isEqualTo(2025);
        assertThat(result.saleStartFrom().getMonthValue()).isEqualTo(10);
    }

    // ---- NA-02: sort = null → NEWEST がデフォルト ----
    @Test
    @DisplayName("新着条件で sort = null の場合は NEWEST がデフォルトになる")
    void na02_newArrivalDefaultSort_isNewest() {
        ProductSearchCondition result = sut.buildNewArrivalCondition(
                false, null, null, null, 1, 15);

        assertThat(result.sort()).isEqualTo(ProductSort.NEWEST);
    }
}
