package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ProductListSearchService} のユニットテスト。
 *
 * <p>
 * P-01〜P-09 のテストケースを網羅する。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceTest {

  private ProductService productService;
  private ProductListSearchService service;

  @BeforeEach
  void setUp() {
    productService = mock(ProductService.class);
    ProductFilterOptionService filterOptionService = mock(ProductFilterOptionService.class);
    service = new ProductListSearchService(productService, filterOptionService);
  }

  // ============================================================
  // P-01〜P-06: calculateTotalPages
  // ============================================================

  @Nested
  @DisplayName("calculateTotalPages")
  class CalculateTotalPages {

    @Test
    @DisplayName("P-01: totalCount=0 のとき 1 を返す（0件でも最低1ページ）")
    void totalCountZero_returnsOne() {
      assertThat(service.calculateTotalPages(0L, 15)).isEqualTo(1);
    }

    @Test
    @DisplayName("P-02: totalCount=1 のとき 1 を返す（1件は1ページ）")
    void totalCountOne_returnsOne() {
      assertThat(service.calculateTotalPages(1L, 15)).isEqualTo(1);
    }

    @Test
    @DisplayName("P-03: totalCount=15（ちょうど1ページ分）のとき 1 を返す")
    void totalCountEqualsSize_returnsOne() {
      assertThat(service.calculateTotalPages(15L, 15)).isEqualTo(1);
    }

    @Test
    @DisplayName("P-04: totalCount=16（1ページを1件超える）のとき 2 を返す")
    void totalCountSizePlusOne_returnsTwo() {
      assertThat(service.calculateTotalPages(16L, 15)).isEqualTo(2);
    }

    @Test
    @DisplayName("P-05: totalCount=30（ちょうど2ページ分）のとき 2 を返す")
    void totalCountTwoPages_returnsTwo() {
      assertThat(service.calculateTotalPages(30L, 15)).isEqualTo(2);
    }

    @Test
    @DisplayName("P-06: totalCount=31 のとき 3 を返す")
    void totalCountThreePages_returnsThree() {
      assertThat(service.calculateTotalPages(31L, 15)).isEqualTo(3);
    }
  }

  // ============================================================
  // P-07〜P-09: searchWithPageCorrection
  // ============================================================

  @Nested
  @DisplayName("searchWithPageCorrection")
  class SearchWithPageCorrection {

    @Test
    @DisplayName("P-07: 1ページ目に結果ありのとき補正なし（search は1回だけ呼ばれる）")
    void firstPageWithResults_noCorrection() {
      ProductSearchCondition condition = condition(1, 5);
      ProductListPage page = new ProductListPage(
          List.of(mock(jp.co.skig.officeorder.model.product.ProductCardView.class)), 10L, 1, 5);
      when(productService.search(condition)).thenReturn(page);

      ProductListSearchResult result = service.searchWithPageCorrection(condition);

      verify(productService, times(1)).search(any());
      assertThat(result.productPage()).isEqualTo(page);
    }

    @Test
    @DisplayName("P-08: totalCount=0 のとき補正なし（search は1回だけ）")
    void totalCountZero_noCorrection() {
      ProductSearchCondition condition = condition(5, 5);
      ProductListPage page = new ProductListPage(List.of(), 0L, 5, 5);
      when(productService.search(condition)).thenReturn(page);

      ProductListSearchResult result = service.searchWithPageCorrection(condition);

      verify(productService, times(1)).search(any());
      assertThat(result.productPage().totalCount()).isZero();
    }

    @Test
    @DisplayName("P-09: ページ超過かつ件数あり・空結果のとき最終ページに補正して再検索（search は2回）")
    void pageOverflow_correctedToLastPage() {
      // page=5, size=5, totalCount=10 → totalPages=2
      // page(5) > totalPages(2) かつ items.isEmpty() → 補正して page=2 で再検索
      ProductSearchCondition condition = condition(5, 5);
      ProductListPage emptyPage = new ProductListPage(List.of(), 10L, 5, 5);
      ProductListPage correctedPage = new ProductListPage(
          List.of(mock(jp.co.skig.officeorder.model.product.ProductCardView.class)), 10L, 2, 5);

      when(productService.search(any())).thenReturn(emptyPage, correctedPage);

      ProductListSearchResult result = service.searchWithPageCorrection(condition);

      ArgumentCaptor<ProductSearchCondition> captor = ArgumentCaptor.forClass(ProductSearchCondition.class);
      verify(productService, times(2)).search(captor.capture());
      // 2回目の呼び出しは page=2 に補正されている
      assertThat(captor.getAllValues().get(1).page()).isEqualTo(2);
      assertThat(result.productPage()).isEqualTo(correctedPage);
    }

    /** テスト用の検索条件を生成する。 */
    private ProductSearchCondition condition(int page, int size) {
      return new ProductSearchCondition(
          null, null, false, List.of(), List.of(),
          ProductCategoryFilter.empty(), ProductSort.NEWEST,
          page, size, null);
    }
  }
}
