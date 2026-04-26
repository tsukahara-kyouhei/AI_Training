package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ProductFilterOptionService#buildSearchTasteFilter} のユニットテスト。
 */
class ProductFilterOptionServiceTest {

  private ProductFilterOptionService service;

  /** テスト用の絞り込み候補群（デスクID:1,2 / チェアID:3 / 収納ID:4,5）。 */
  private ProductFilterOptionsBundle bundle;

  @BeforeEach
  void setUp() {
    service = new ProductFilterOptionService(mock(ProductFilterOptionRepository.class));
    bundle = new ProductFilterOptionsBundle(
        List.of(),
        List.of(),
        List.of(new CategoryFilterOption(1, "モダン"), new CategoryFilterOption(2, "ナチュラル")),
        List.of(),
        List.of(),
        List.of(new CategoryFilterOption(3, "シンプル")),
        List.of(),
        List.of(new CategoryFilterOption(4, "インダストリアル"), new CategoryFilterOption(5, "北欧")));
  }

  @Nested
  @DisplayName("buildSearchTasteFilter")
  class BuildSearchTasteFilter {

    @Test
    @DisplayName("有効なIDのみが結果に残る")
    void validIdsAreRetained() {
      ProductCategoryFilter result = service.buildSearchTasteFilter(
          List.of(1, 2),
          List.of(3),
          List.of(4),
          bundle);

      assertThat(result.deskTasteIds()).containsExactly(1, 2);
      assertThat(result.chairTasteIds()).containsExactly(3);
      assertThat(result.storageTasteIds()).containsExactly(4);
    }

    @Test
    @DisplayName("許可リストに存在しないIDは除外される")
    void invalidIdsAreRejected() {
      ProductCategoryFilter result = service.buildSearchTasteFilter(
          List.of(1, 99),
          List.of(999),
          List.of(4, 5, 888),
          bundle);

      assertThat(result.deskTasteIds()).containsExactly(1);
      assertThat(result.chairTasteIds()).isEmpty();
      assertThat(result.storageTasteIds()).containsExactly(4, 5);
    }

    @Test
    @DisplayName("全入力null → 全テイストが空リスト")
    void nullInputsProduceEmptyLists() {
      ProductCategoryFilter result = service.buildSearchTasteFilter(null, null, null, bundle);

      assertThat(result.deskTasteIds()).isEmpty();
      assertThat(result.chairTasteIds()).isEmpty();
      assertThat(result.storageTasteIds()).isEmpty();
    }

    @Test
    @DisplayName("テイスト以外のフィールドは空リストのまま")
    void nonTasteFieldsAreEmpty() {
      ProductCategoryFilter result = service.buildSearchTasteFilter(
          List.of(1), List.of(3), List.of(4), bundle);

      assertThat(result.deskTopShapeIds()).isEmpty();
      assertThat(result.deskWidthBandIds()).isEmpty();
      assertThat(result.deskDepthBandIds()).isEmpty();
      assertThat(result.deskHeightBandIds()).isEmpty();
      assertThat(result.chairFunctionIds()).isEmpty();
      assertThat(result.chairMaterialIds()).isEmpty();
      assertThat(result.storageUsageIds()).isEmpty();
    }
  }
}
