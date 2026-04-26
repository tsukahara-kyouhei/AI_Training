package jp.co.skig.officeorder.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * {@link ProductRepository#search} 経由で {@code buildSearchParams()} の
 * keyword・hasTasteSearchFilter マッピングを検証するユニットテスト。
 */
class ProductRepositoryTest {

  private ProductMapper productMapper;
  private ProductRepository repository;

  @BeforeEach
  void setUp() {
    productMapper = mock(ProductMapper.class);
    AppTimeProvider timeProvider = mock(AppTimeProvider.class);
    when(timeProvider.nowOffsetDateTime()).thenReturn(OffsetDateTime.parse("2024-01-01T00:00:00+09:00"));
    repository = new ProductRepository(productMapper, timeProvider);
    // countProducts が 0 を返すとページネーション計算をスキップできる
    when(productMapper.countProducts(anyMap())).thenReturn(0L);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> captureParams() {
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    org.mockito.Mockito.verify(productMapper).countProducts(captor.capture());
    return captor.getValue();
  }

  private ProductSearchCondition simpleCondition(String keyword, ProductCategoryFilter filter) {
    return new ProductSearchCondition(
        null, keyword, false, List.of(), List.of(),
        filter, ProductSort.RECOMMENDED, 1, 15, null);
  }

  @Nested
  @DisplayName("keyword / keywordLike パラメータ")
  class KeywordParams {

    @Test
    @DisplayName("通常キーワード → keyword は trim 済み、keywordLike は %keyword%")
    void normalKeyword() {
      repository.search(simpleCondition("  デスク  ", ProductCategoryFilter.empty()));

      Map<String, Object> params = captureParams();
      assertThat(params.get("keyword")).isEqualTo("デスク");
      assertThat(params.get("keywordLike")).isEqualTo("%デスク%");
    }

    @Test
    @DisplayName("null キーワード → keyword, keywordLike ともに null")
    void nullKeyword() {
      repository.search(simpleCondition(null, ProductCategoryFilter.empty()));

      Map<String, Object> params = captureParams();
      assertThat(params.get("keyword")).isNull();
      assertThat(params.get("keywordLike")).isNull();
    }

    @Test
    @DisplayName("空白のみキーワード → keyword, keywordLike ともに null")
    void blankKeyword() {
      repository.search(simpleCondition("   ", ProductCategoryFilter.empty()));

      Map<String, Object> params = captureParams();
      assertThat(params.get("keyword")).isNull();
      assertThat(params.get("keywordLike")).isNull();
    }
  }

  @Nested
  @DisplayName("hasTasteSearchFilter パラメータ")
  class TasteSearchFilter {

    @Test
    @DisplayName("カテゴリなし かつ テイスト選択あり → true")
    void trueWhenNoCategoryAndTasteSelected() {
      ProductCategoryFilter filter = new ProductCategoryFilter(
          List.of(), List.of(), List.of(), List.of(),
          List.of(1), // deskTasteIds
          List.of(), List.of(), List.of(), List.of(), List.of());
      repository.search(simpleCondition(null, filter));

      Map<String, Object> params = captureParams();
      assertThat(params.get("hasTasteSearchFilter")).isEqualTo(true);
    }

    @Test
    @DisplayName("カテゴリあり のときはテイスト選択があっても false")
    void falseWhenCategoryIsSet() {
      ProductSearchCondition condition = new ProductSearchCondition(
          "desk", null, false, List.of(), List.of(),
          new ProductCategoryFilter(
              List.of(), List.of(), List.of(), List.of(),
              List.of(1), List.of(), List.of(), List.of(), List.of(), List.of()),
          ProductSort.RECOMMENDED, 1, 15, null);
      repository.search(condition);

      Map<String, Object> params = captureParams();
      assertThat(params.get("hasTasteSearchFilter")).isEqualTo(false);
    }

    @Test
    @DisplayName("カテゴリなし かつ テイスト未選択 → false")
    void falseWhenNoCategoryAndNoTaste() {
      repository.search(simpleCondition(null, ProductCategoryFilter.empty()));

      Map<String, Object> params = captureParams();
      assertThat(params.get("hasTasteSearchFilter")).isEqualTo(false);
    }

    @Test
    @DisplayName("chairTaste 選択のみでも true")
    void trueWithChairTasteOnly() {
      ProductCategoryFilter filter = new ProductCategoryFilter(
          List.of(), List.of(), List.of(), List.of(), List.of(),
          List.of(), List.of(),
          List.of(3), // chairTasteIds
          List.of(), List.of());
      repository.search(simpleCondition(null, filter));

      Map<String, Object> params = captureParams();
      assertThat(params.get("hasTasteSearchFilter")).isEqualTo(true);
    }
  }
}
