package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProductService#buildCondition} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private AppTimeProvider appTimeProvider;

    @InjectMocks
    private ProductService service;

    @Test
    void buildCondition_短縮オーバーロードはtasteIdsが空になる() {
        // 9引数の短縮形を使用
        ProductSearchCondition condition = service.buildCondition(
                "DESK", null, false,
                List.of(), List.of(),
                "recommended", 1, 15,
                ProductSort.RECOMMENDED
        );
        assertThat(condition.tasteIds()).isEmpty();
    }

    @Test
    void buildCondition_nullのtasteIdsは空リストに正規化される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                null,
                null
        );
        assertThat(condition.tasteIds()).isEmpty();
    }

    @Test
    void buildCondition_tasteIdsが正しく設定される() {
        List<Long> tasteIds = List.of(1L, 2L);
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                tasteIds,
                null
        );
        assertThat(condition.tasteIds()).containsExactly(1L, 2L);
    }
}
