package jp.co.skig.officeorder.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.product.ProductFilterOptionService;
import jp.co.skig.officeorder.service.product.ProductListSearchService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;

/**
 * {@link CatalogController#searchResults} のユニットテスト。
 */
@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

  @Mock
  private ProductService productService;
  @Mock
  private ProductListSearchService productListSearchService;
  @Mock
  private ProductFilterOptionService productFilterOptionService;
  @Mock
  private MemberService memberService;
  @Mock
  private MemberSessionService memberSessionService;

  @InjectMocks
  private CatalogController controller;

  private ProductFilterOptionsBundle emptyBundle;
  private ProductSearchCondition dummyCondition;
  private ProductListPage emptyPage;

  @BeforeEach
  void setUp() {
    emptyBundle = new ProductFilterOptionsBundle(
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    dummyCondition = new ProductSearchCondition(
        null, null, false, List.of(), List.of(),
        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null);
    emptyPage = new ProductListPage(List.of(), 0, 1, 15);

    when(productFilterOptionService.loadOptionsBundle()).thenReturn(emptyBundle);
  }

  private Model newModel() {
    return new ExtendedModelMap();
  }

  @Nested
  @DisplayName("キーワード長バリデーション")
  class KeywordLengthValidation {

    @Test
    @DisplayName("101文字のキーワードはエラーを返す")
    void keywordOver100CharsReturnsError() {
      String longKeyword = "あ".repeat(101);
      Model model = newModel();

      String view = controller.searchResults(
          longKeyword, false, null, null, null, null, null,
          null, 1, 15, model);

      assertThat(view).isEqualTo("pages/product-list-search-results");
      assertThat(model.getAttribute("keywordTooLong")).isEqualTo(true);
      assertThat(model.getAttribute("products")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("ちょうど100文字のキーワードは通過する")
    void keyword100CharsIsAllowed() {
      String keyword100 = "あ".repeat(100);
      stubNormalSearch();
      Model model = newModel();

      String view = controller.searchResults(
          keyword100, false, null, null, null, null, null,
          null, 1, 15, model);

      assertThat(view).isEqualTo("pages/product-list-search-results");
      assertThat(model.getAttribute("keywordTooLong")).isEqualTo(false);
    }
  }

  @Nested
  @DisplayName("NFKC正規化")
  class NfkcNormalization {

    @Test
    @DisplayName("半角カタカナは全角へ正規化されて buildCondition に渡される")
    void halfWidthKatakanaIsNormalized() {
      stubNormalSearch();
      Model model = newModel();

      controller.searchResults(
          "ｱｲｳ", false, null, null, null, null, null,
          null, 1, 15, model);

      ArgumentCaptor<String> kwCaptor = ArgumentCaptor.forClass(String.class);
      verify(productListSearchService).buildCondition(
          isNull(),
          kwCaptor.capture(),
          eq(false),
          isNull(),
          isNull(),
          any(ProductCategoryFilter.class),
          isNull(),
          eq(1),
          eq(15),
          eq(ProductSort.RECOMMENDED),
          eq(emptyBundle));
      assertThat(kwCaptor.getValue()).isEqualTo("アイウ");
    }

    @Test
    @DisplayName("前後の空白は strip される")
    void leadingTrailingSpacesAreTrimmed() {
      stubNormalSearch();
      Model model = newModel();

      controller.searchResults(
          "  デスク  ", false, null, null, null, null, null,
          null, 1, 15, model);

      ArgumentCaptor<String> kwCaptor = ArgumentCaptor.forClass(String.class);
      verify(productListSearchService).buildCondition(
          isNull(), kwCaptor.capture(), eq(false),
          isNull(), isNull(),
          any(ProductCategoryFilter.class),
          isNull(), eq(1), eq(15),
          eq(ProductSort.RECOMMENDED), eq(emptyBundle));
      assertThat(kwCaptor.getValue()).isEqualTo("デスク");
    }
  }

  @Nested
  @DisplayName("テイストフィルタのモデル設定")
  class TasteFilterModel {

    @Test
    @DisplayName("tasteFilter のIDがモデルに反映される")
    void tasteIdsAreAddedToModel() {
      ProductCategoryFilter tasteFilter = new ProductCategoryFilter(
          List.of(), List.of(), List.of(), List.of(),
          List.of(1, 2), // deskTaste
          List.of(), List.of(),
          List.of(3), // chairTaste
          List.of(),
          List.of(4) // storageTaste
      );
      when(productFilterOptionService.buildSearchTasteFilter(any(), any(), any(), any()))
          .thenReturn(tasteFilter);
      when(productListSearchService.buildCondition(
          any(), any(), anyBoolean(), any(), any(),
          any(), any(), anyInt(), anyInt(), any(), any()))
          .thenReturn(dummyCondition);
      when(productListSearchService.searchWithPageCorrection(any()))
          .thenReturn(new ProductListSearchResult(dummyCondition, emptyPage));
      when(productListSearchService.calculateTotalPages(anyLong(), anyInt())).thenReturn(0);
      when(productFilterOptionService.resolveSelectedColorKeys(any(), any())).thenReturn(List.of());

      Model model = newModel();
      controller.searchResults(
          null, false, null, null,
          List.of(1, 2), List.of(3), List.of(4),
          null, 1, 15, model);

      assertThat(model.getAttribute("deskTasteIds")).isEqualTo(List.of(1, 2));
      assertThat(model.getAttribute("chairTasteIds")).isEqualTo(List.of(3));
      assertThat(model.getAttribute("storageTasteIds")).isEqualTo(List.of(4));
    }

    @Test
    @DisplayName("0件のとき suggestedProducts がモデルに追加される")
    void suggestedProductsAddedWhenZeroResults() {
      stubNormalSearch();
      when(productService.getTopRankedProducts(8)).thenReturn(List.of());
      Model model = newModel();

      controller.searchResults(
          "存在しない商品", false, null, null, null, null, null,
          null, 1, 15, model);

      assertThat(model.containsAttribute("suggestedProducts")).isTrue();
    }
  }

  /** 通常の検索成功シナリオ（0件）のスタブをまとめて設定。 */
  private void stubNormalSearch() {
    when(productFilterOptionService.buildSearchTasteFilter(any(), any(), any(), any()))
        .thenReturn(ProductCategoryFilter.empty());
    when(productListSearchService.buildCondition(
        any(), any(), anyBoolean(), any(), any(),
        any(), any(), anyInt(), anyInt(), any(), any()))
        .thenReturn(dummyCondition);
    when(productListSearchService.searchWithPageCorrection(any()))
        .thenReturn(new ProductListSearchResult(dummyCondition, emptyPage));
    when(productListSearchService.calculateTotalPages(anyLong(), anyInt())).thenReturn(0);
    when(productFilterOptionService.resolveSelectedColorKeys(any(), any())).thenReturn(List.of());
    when(productService.getTopRankedProducts(8)).thenReturn(List.of());
  }
}
