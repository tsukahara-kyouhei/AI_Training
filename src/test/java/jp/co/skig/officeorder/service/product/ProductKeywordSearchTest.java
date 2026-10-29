package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.Test;

class ProductKeywordSearchTest {

    @Test
    void buildCondition_normalizes_keyword_with_nfkc_and_lowercase() {
        ProductService service = new ProductService(mock(ProductRepository.class), mock(AppTimeProvider.class));

        ProductSearchCondition condition = service.buildCondition(
                null,
                " ＡＢ－００１２ ",
                false,
                null,
                null,
                null,
                1,
                15,
                ProductSort.RECOMMENDED
        );

        assertThat(condition.keyword()).isEqualTo("ab-0012");
    }

    @Test
    void repository_buildSearchParams_generates_exact_prefix_and_contains_patterns() throws Exception {
        ProductRepository repository = new ProductRepository(mock(ProductMapper.class), mock(AppTimeProvider.class));
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                "P0001-C",
                false,
                List.of(),
                List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                1,
                15,
                null
        );

        Method method = ProductRepository.class.getDeclaredMethod("buildSearchParams", ProductSearchCondition.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) method.invoke(repository, condition);

        assertThat(params.get("keywordExactNormalized")).isEqualTo("p0001-c");
        assertThat(params.get("keywordPrefixPattern")).isEqualTo("p0001-c%");
        assertThat(params.get("keywordContainsPattern")).isEqualTo("%p0001-c%");
    }

    @Test
    void repository_buildSearchParams_rejects_non_prefix_boundaries() throws Exception {
        ProductRepository repository = new ProductRepository(mock(ProductMapper.class), mock(AppTimeProvider.class));
        Method method = ProductRepository.class.getDeclaredMethod("buildSearchParams", ProductSearchCondition.class);
        method.setAccessible(true);

        for (String keyword : List.of("P00010", "P0001XYZ", "P0001-", "%", "_")) {
            ProductSearchCondition condition = new ProductSearchCondition(
                    null,
                    keyword,
                    false,
                    List.of(),
                    List.of(),
                    ProductCategoryFilter.empty(),
                    ProductSort.RECOMMENDED,
                    1,
                    15,
                    null
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) method.invoke(repository, condition);
            assertThat(params.get("keywordPrefixPattern")).isNull();
        }
    }

    @Test
    void repository_buildSearchParams_escapes_like_wildcards_for_contains_search() throws Exception {
        ProductRepository repository = new ProductRepository(mock(ProductMapper.class), mock(AppTimeProvider.class));
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                "desk%_\\",
                false,
                List.of(),
                List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                1,
                15,
                null
        );

        Method method = ProductRepository.class.getDeclaredMethod("buildSearchParams", ProductSearchCondition.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) method.invoke(repository, condition);

        assertThat(params.get("keywordContainsPattern")).isEqualTo("%desk\\%\\_\\\\%");
    }
}
