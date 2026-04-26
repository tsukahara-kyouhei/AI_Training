package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ProductService#getTopRankedProducts} のユニットテスト。
 */
class ProductServiceTest {

  private ProductRepository repository;
  private ProductService service;

  @BeforeEach
  void setUp() {
    repository = mock(ProductRepository.class);
    service = new ProductService(repository, mock(AppTimeProvider.class));
  }

  @Test
  @DisplayName("getTopRankedProducts(limit) は repository.findTopRankedProducts(limit) に委譲する")
  void getTopRankedProductsDelegatesToRepository() {
    RankedProductCardView ranked = new RankedProductCardView(1,
        new ProductCardView(10L, "テストデスク", "¥10,000", List.of(), "P001", true, "/products/10"));
    when(repository.findTopRankedProducts(5)).thenReturn(List.of(ranked));

    List<RankedProductCardView> result = service.getTopRankedProducts(5);

    verify(repository).findTopRankedProducts(5);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).rank()).isEqualTo(1);
    assertThat(result.get(0).product().productId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("findTopRankedProducts() は TOP_RANKED_LIMIT=8 で委譲する")
  void findTopRankedProductsUsesDefaultLimit() {
    when(repository.findTopRankedProducts(8)).thenReturn(List.of());

    service.findTopRankedProducts();

    verify(repository).findTopRankedProducts(8);
  }
}
