package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.Test;

class ProductFilterOptionServiceTest {

    @Test
    void buildSearchFilterNormalizesTasteIdsForKeywordSearch() {
        ProductFilterOptionRepository repository = mock(ProductFilterOptionRepository.class);
        ProductFilterOptionService service = new ProductFilterOptionService(repository);

        ProductFilterOptionsBundle optionsBundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(1, "ベーシック")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(2, "カジュアル")),
                List.of(),
                List.of(new CategoryFilterOption(3, "シンプル"))
        );

        ProductCategoryFilter filter = service.buildSearchFilter(List.of(1), List.of(2), List.of(3), optionsBundle);

        assertThat(filter.deskTasteIds()).containsExactly(1);
        assertThat(filter.chairTasteIds()).containsExactly(2);
        assertThat(filter.storageTasteIds()).containsExactly(3);
    }
}
