package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    @Mock
    private ProductFilterOptionsBundle optionsBundle;

    @Mock
    private ProductSearchCondition dummyCondition;

    private ProductListSearchService service;

    @BeforeEach
    void setUp() {
        service = new ProductListSearchService(productService, productFilterOptionService);
    }

    @Nested
    @DisplayName("検索条件組み立てテスト (buildCondition / buildNewArrivalCondition)")
    class BuildConditionTest {

        @Test
        @DisplayName("buildCondition(カテゴリフィルターなし): 生の絞り込み値を正規化してProductServiceへ引き渡すこと")
        void buildCondition_withoutCategoryFilter() {
            List<Integer> rawPriceBandIds = List.of(1, 2);
            List<String> rawColorKeys = List.of("red");
            List<Integer> normalizedPriceBands = List.of(1, 2);
            List<String> normalizedColorKeys = List.of("red");
            List<Long> colorIds = List.of(10L);

            when(productFilterOptionService.normalizePriceBandIds(rawPriceBandIds)).thenReturn(normalizedPriceBands);
            when(productFilterOptionService.normalizeColorKeys(rawColorKeys, optionsBundle)).thenReturn(normalizedColorKeys);
            when(productFilterOptionService.resolveColorIds(normalizedColorKeys, optionsBundle)).thenReturn(colorIds);
            when(productService.buildCondition(
                    eq("desk"), eq("パソコンデスク"), eq(true), eq(normalizedPriceBands),
                    eq(colorIds), eq("recommended"), eq(1), eq(20), eq(ProductSort.RECOMMENDED),
                    any(ProductCategoryFilter.class)
            )).thenReturn(dummyCondition);

            ProductSearchCondition result = service.buildCondition(
                    "desk", "パソコンデスク", true, rawPriceBandIds, rawColorKeys,
                    "recommended", 1, 20, ProductSort.RECOMMENDED, optionsBundle
            );

            assertThat(result).isEqualTo(dummyCondition);
            verify(productFilterOptionService, never()).loadOptionsBundle();
        }

        @Test
        @DisplayName("buildCondition(optionsBundleがnullの場合): モックからloadOptionsBundleを呼び出して解決すること")
        void buildCondition_withNullOptionsBundle() {
            when(productFilterOptionService.loadOptionsBundle()).thenReturn(optionsBundle);
            when(productFilterOptionService.normalizePriceBandIds(null)).thenReturn(Collections.emptyList());
            when(productFilterOptionService.normalizeColorKeys(null, optionsBundle)).thenReturn(Collections.emptyList());
            when(productFilterOptionService.resolveColorIds(Collections.emptyList(), optionsBundle)).thenReturn(Collections.emptyList());

            service.buildCondition("desk", null, false, null, null, "recommended", 1, 20, ProductSort.RECOMMENDED, null);

            verify(productFilterOptionService).loadOptionsBundle();
        }

        @Test
        @DisplayName("buildNewArrivalCondition: 新着商品用の検索条件を正常に組み立てること")
        void buildNewArrivalCondition() {
            List<Integer> rawPriceBands = List.of(1);
            List<String> rawColorKeys = List.of("blue");
            List<Long> colorIds = List.of(20L);

            when(productFilterOptionService.normalizePriceBandIds(rawPriceBands)).thenReturn(rawPriceBands);
            when(productFilterOptionService.normalizeColorKeys(rawColorKeys, optionsBundle)).thenReturn(rawColorKeys);
            when(productFilterOptionService.resolveColorIds(rawColorKeys, optionsBundle)).thenReturn(colorIds);
            when(productService.buildNewArrivalCondition(true, rawPriceBands, colorIds, "newest", 1, 10))
                    .thenReturn(dummyCondition);

            ProductSearchCondition result = service.buildNewArrivalCondition(
                    true, rawPriceBands, rawColorKeys, "newest", 1, 10, optionsBundle
            );

            assertThat(result).isEqualTo(dummyCondition);
        }
    }

    @Nested
    @DisplayName("ページ補正付き検索テスト (searchWithPageCorrection)")
    class SearchWithPageCorrectionTest {

        @Test
        @DisplayName("指定ページに結果が存在する場合: 再検索を行わずに初回結果を返すこと")
        void noCorrectionNeeded() {
            ProductSearchCondition condition = new ProductSearchCondition(
                    "desk", null, false, null, null, null, null, 1, 10, null
            );
            ProductCardView dummyCard = org.mockito.Mockito.mock(ProductCardView.class);
            ProductListPage pageResult = new ProductListPage(List.of(dummyCard), 15L, 1, 10);

            when(productService.search(condition)).thenReturn(pageResult);

            ProductListSearchResult result = service.searchWithPageCorrection(condition);

            assertThat(result.condition()).isEqualTo(condition);
            assertThat(result.productPage()).isEqualTo(pageResult);
            verify(productService).search(condition);
        }

        @Test
        @DisplayName("総件数>0 かつ 指定ページが総ページ数を超過して空の場合: 最終ページへ自動補正して再検索すること")
        void correctToLastPageWhenExceeded() {
            ProductSearchCondition condition = new ProductSearchCondition(
                    "desk", null, false, null, null, null, null, 5, 10, null
            );
            ProductCardView dummyCard = org.mockito.Mockito.mock(ProductCardView.class);
            ProductListPage emptyPageResult = new ProductListPage(Collections.emptyList(), 25L, 5, 10);
            ProductListPage correctedPageResult = new ProductListPage(List.of(dummyCard), 25L, 3, 10);

            // 条件分岐を一括で処理するスタブに統一（UnnecessaryStubbing の解消）
            when(productService.search(any(ProductSearchCondition.class))).thenAnswer(invocation -> {
                ProductSearchCondition c = invocation.getArgument(0);
                if (c.page() == 3) {
                    return correctedPageResult;
                }
                return emptyPageResult;
            });

            ProductListSearchResult result = service.searchWithPageCorrection(condition);

            assertThat(result.condition().page()).isEqualTo(3);
            assertThat(result.productPage()).isEqualTo(correctedPageResult);
        }

        @Test
        @DisplayName("検索結果が0件の場合: ページ補正を行わずそのまま返すこと")
        void zeroTotalCount_noCorrection() {
            ProductSearchCondition condition = new ProductSearchCondition(
                    "desk", null, false, null, null, null, null, 2, 10, null
            );
            ProductListPage zeroResult = new ProductListPage(Collections.emptyList(), 0L, 2, 10);

            when(productService.search(condition)).thenReturn(zeroResult);

            ProductListSearchResult result = service.searchWithPageCorrection(condition);

            assertThat(result.condition().page()).isEqualTo(2);
            assertThat(result.productPage().totalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("総ページ数計算テスト (calculateTotalPages)")
    class CalculateTotalPagesTest {

        @Test
        @DisplayName("0件以下の場合は1ページを返すこと")
        void zeroOrNegativeTotalCount() {
            assertThat(service.calculateTotalPages(0, 10)).isEqualTo(1);
            assertThat(service.calculateTotalPages(-5, 10)).isEqualTo(1);
        }

        @Test
        @DisplayName("割り切れる件数の計算が正しいこと")
        void exactMultiplePages() {
            assertThat(service.calculateTotalPages(20, 10)).isEqualTo(2);
            assertThat(service.calculateTotalPages(10, 10)).isEqualTo(1);
        }

        @Test
        @DisplayName("端数が出る件数の計算（切り上げ）が正しいこと")
        void fractionPages() {
            assertThat(service.calculateTotalPages(21, 10)).isEqualTo(3);
            assertThat(service.calculateTotalPages(1, 10)).isEqualTo(1);
        }
    }
}
