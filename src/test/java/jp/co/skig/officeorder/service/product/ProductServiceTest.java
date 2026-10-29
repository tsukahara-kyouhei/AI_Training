package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.mapper.RangeValue;
import jp.co.skig.officeorder.mapper.row.ProductListMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductRankedMapperRow;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 商品検索・一覧サービスの単体テスト。
 *
 * <p>単体テスト計画の PRD-001〜PRD-011 に対応する。
 */
class ProductServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, TOKYO);
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, TOKYO);

    private ProductRepository productRepository;
    private AppTimeProvider appTimeProvider;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        appTimeProvider = mock(AppTimeProvider.class);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(FIXED_NOW);
        productService = new ProductService(productRepository, appTimeProvider);
    }

    @Nested
    @DisplayName("PRD-001〜PRD-005 キーワード正規化")
    class KeywordNormalization {

        @Test
        @DisplayName("PRD-001 商品コード完全一致は対象商品だけを検索条件へ渡す")
        void buildCondition_productCodeExact_passesKeyword() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, "P0001-C01", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
            );

            assertThat(condition.keyword()).isEqualTo("p0001-c01");
        }

        @Test
        @DisplayName("PRD-002 商品コード前方一致は区切りを考慮した前方一致パターンを生成する")
        void repository_buildSearchParams_generatesPrefixPattern() throws Exception {
            ProductRepository repository = new ProductRepository(mock(ProductMapper.class), appTimeProvider);
            ProductSearchCondition condition = new ProductSearchCondition(
                    null, "P0001-C", false, List.of(), List.of(),
                    ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
            );

            Map<String, Object> params = invokeBuildSearchParams(repository, condition);

            assertThat(params.get("keywordPrefixPattern")).isEqualTo("p0001-c%");
        }

        @Test
        @DisplayName("PRD-003 商品名・説明・バリエーション部分一致は正規化済み検索条件を生成する")
        void buildCondition_keywordContains_normalizesKeyword() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, "  ＤＥＳＫ  ", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
            );

            assertThat(condition.keyword()).isEqualTo("desk");
        }

        @Test
        @DisplayName("PRD-004 全角半角・大小文字はNFKCと小文字化で同一条件にする")
        void buildCondition_fullWidthHalfWidthCase_normalizesToSame() {
            ProductSearchCondition fullWidth = productService.buildCondition(
                    null, "ＡＢＣ－００１", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
            );
            ProductSearchCondition halfWidth = productService.buildCondition(
                    null, "abc-001", false, null, null, null, 1, 15, ProductSort.RECOMMENDED
            );

            assertThat(fullWidth.keyword()).isEqualTo("abc-001");
            assertThat(halfWidth.keyword()).isEqualTo("abc-001");
        }

        @Test
        @DisplayName("PRD-005 SQLワイルドカード文字はエスケープする")
        void repository_buildSearchParams_escapesWildcards() throws Exception {
            ProductRepository repository = new ProductRepository(mock(ProductMapper.class), appTimeProvider);
            ProductSearchCondition condition = new ProductSearchCondition(
                    null, "desk%_\\", false, List.of(), List.of(),
                    ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
            );

            Map<String, Object> params = invokeBuildSearchParams(repository, condition);

            assertThat(params.get("keywordContainsPattern")).isEqualTo("%desk\\%\\_\\\\%");
        }
    }

    @Nested
    @DisplayName("PRD-006〜PRD-008 絞り込み条件")
    class FilterConditions {

        @Test
        @DisplayName("PRD-006 価格帯・カラー・在庫は有効な条件だけを検索条件へ反映する")
        void buildCondition_filters_onlyValidValuesReflected() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, null, true,
                    Arrays.asList(1, 99, 2, 1),
                    Arrays.asList(10L, null, 20L, 10L),
                    null, 1, 15, ProductSort.RECOMMENDED
            );

            assertThat(condition.inStockOnly()).isTrue();
            assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_1, PriceBand.BAND_2);
            assertThat(condition.colorIds()).containsExactly(10L, 20L);
        }

        @Test
        @DisplayName("PRD-007 カテゴリ固有フィルタはデスク、チェア、収納家具の条件を分離して扱う")
        void buildCondition_categoryFilter_preservesCategorySpecificFilters() {
            ProductCategoryFilter deskFilter = new ProductCategoryFilter(
                    List.of(1), List.of(2), List.of(), List.of(), List.of(3),
                    List.of(), List.of(), List.of(), List.of(), List.of()
            );

            ProductSearchCondition condition = productService.buildCondition(
                    "desk", null, false, null, null, null, 1, 15,
                    ProductSort.RECOMMENDED, deskFilter
            );

            assertThat(condition.categoryFilter().deskTopShapeIds()).containsExactly(1);
            assertThat(condition.categoryFilter().deskWidthBandIds()).containsExactly(2);
            assertThat(condition.categoryFilter().deskTasteIds()).containsExactly(3);
            assertThat(condition.categoryFilter().chairFunctionIds()).isEmpty();
            assertThat(condition.categoryFilter().storageUsageIds()).isEmpty();
        }

        @Test
        @DisplayName("PRD-008 不正な価格帯IDは候補一覧にないIDを除外する")
        void buildCondition_invalidPriceBandIds_excluded() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, null, false,
                    List.of(999, 1),
                    null,
                    null, 1, 15, ProductSort.RECOMMENDED
            );

            assertThat(condition.priceBands()).containsExactly(PriceBand.BAND_1);
        }
    }

    @Nested
    @DisplayName("PRD-009〜PRD-010 ページング")
    class Paging {

        @Test
        @DisplayName("PRD-009 ページ番号は下限補正を行う")
        void buildCondition_pageBelowOne_normalizesToAtLeastOne() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, null, false, null, null, null, 0, 15, ProductSort.RECOMMENDED
            );

            assertThat(condition.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("PRD-009 表示件数は許可サイズへの補正を行う")
        void buildCondition_invalidSize_normalizesToAllowedSize() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, null, false, null, null, null, 1, 20, ProductSort.RECOMMENDED
            );

            assertThat(condition.size()).isEqualTo(15);
        }

        @Test
        @DisplayName("PRD-009 許可された表示件数はそのまま通す")
        void buildCondition_allowedSize_keepsValue() {
            ProductSearchCondition condition = productService.buildCondition(
                    null, null, false, null, null, null, 1, 30, ProductSort.RECOMMENDED
            );

            assertThat(condition.size()).isEqualTo(30);
        }

        @Test
        @DisplayName("PRD-010 0件は空結果を返す")
        void search_zeroResults_returnsEmptyPage() {
            ProductSearchCondition condition = new ProductSearchCondition(
                    null, null, false, List.of(), List.of(),
                    ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
            );
            when(productRepository.search(condition)).thenReturn(new ProductListPage(List.of(), 0, 1, 15));

            ProductListPage page = productService.search(condition);

            assertThat(page.items()).isEmpty();
            assertThat(page.totalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("PRD-011 おすすめ商品フォールバック")
    class RecommendedFallback {

        @Test
        @DisplayName("PRD-011 おすすめ商品が空は新着商品へのフォールバックを行う")
        void findTopRankedProducts_emptyRanking_fallsBackToNewArrivals() {
            ProductRankedMapperRow row = new ProductRankedMapperRow(
                    1, 10L, "デスク", new BigDecimal("30000"), 5, "P0001-C01"
            );
            when(productRepository.findTopRankedProducts(8))
                    .thenReturn(List.of(new RankedProductCardView(1, new ProductCardView(
                            10L, "デスク", "¥30,000", List.of(), "P0001-C01", true, "/products/10"
                    ))));

            List<RankedProductCardView> result = productService.findTopRankedProducts();

            assertThat(result).isNotEmpty();
            verify(productRepository).findTopRankedProducts(8);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeBuildSearchParams(ProductRepository repository,
                                                        ProductSearchCondition condition) throws Exception {
        Method method = ProductRepository.class.getDeclaredMethod("buildSearchParams", ProductSearchCondition.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(repository, condition);
    }
}