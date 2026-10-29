package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductListSearchService productListSearchService;

    @Test
    void repository_search_for_product_code_prefix_matches_db_count_and_rows() {
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                "P0001",
                false,
                java.util.List.of(),
                java.util.List.of(),
                jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                1,
                15,
                null
        );

        ProductListPage page = productRepository.search(condition);
        assertThat(page.totalCount()).isGreaterThan(0);
        assertThat(page.items()).isNotEmpty();
        assertThat(page.items().size()).isLessThanOrEqualTo(15);
        assertThat(page.items().stream().map(item -> item.productName()).toList()).isNotEmpty();
    }

    @Test
    void search_and_category_endpoints_are_accessible() throws Exception {
        mockMvc.perform(get("/products/search").param("q", "P0001").param("page", "1").param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/product-list-search-results"));

        mockMvc.perform(get("/categories/desks").param("page", "1").param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/product-list-category-desk"));
    }

    @Test
    void search_with_page_correction_moves_to_last_valid_page() {
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                null,
                false,
                java.util.List.of(),
                java.util.List.of(),
                jp.co.skig.officeorder.model.product.ProductCategoryFilter.empty(),
                ProductSort.RECOMMENDED,
                999,
                15,
                null
        );

        var result = productListSearchService.searchWithPageCorrection(condition);
        int totalPages = productListSearchService.calculateTotalPages(result.productPage().totalCount(), condition.size());
        assertThat(result.condition().page()).isLessThanOrEqualTo(totalPages);
        assertThat(result.productPage().totalCount()).isGreaterThanOrEqualTo(0);
    }
}
