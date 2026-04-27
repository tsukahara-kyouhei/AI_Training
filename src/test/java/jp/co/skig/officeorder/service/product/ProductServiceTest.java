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

    // ---- キーワード正規化 ----

    @Test
    void buildCondition_キーワードの前後空白はトリムされる() {
        ProductSearchCondition condition = service.buildCondition(
                null, "  desk  ", false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.keyword()).isEqualTo("desk");
    }

    @Test
    void buildCondition_nullキーワードはnullのまま() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.keyword()).isNull();
    }

    // ---- ページ補正 ----

    @Test
    void buildCondition_pageが0のとき1に補正される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 0, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_pageが負のとき1に補正される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, -5, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.page()).isEqualTo(1);
    }

    @Test
    void buildCondition_pageが正のときそのまま渡される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 3, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.page()).isEqualTo(3);
    }

    // ---- 表示件数補正 ----

    @Test
    void buildCondition_許可されていないsizeは15に補正される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 20,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.size()).isEqualTo(15);
    }

    @Test
    void buildCondition_size30は通過する() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 30,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.size()).isEqualTo(30);
    }

    @Test
    void buildCondition_size60は通過する() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 60,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.size()).isEqualTo(60);
    }

    // ---- 並び順解決 ----

    @Test
    void buildCondition_nullのsortは既定値になる() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.PRICE_ASC,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    @Test
    void buildCondition_不正なsort文字列は既定値になる() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                "invalid_sort", 1, 15,
                ProductSort.PRICE_DESC,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_DESC);
    }

    @Test
    void buildCondition_有効なsort文字列は解決される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                "price_asc", 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.sort()).isEqualTo(ProductSort.PRICE_ASC);
    }

    // ---- カラーID正規化 ----

    @Test
    void buildCondition_nullのcolorIdsは空リストになる() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), null,
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.colorIds()).isEmpty();
    }

    @Test
    void buildCondition_重複するcolorIdsは除去される() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(1L, 1L, 2L),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                ProductCategoryFilter.empty(),
                List.of(),
                null
        );
        assertThat(condition.colorIds()).containsExactly(1L, 2L);
    }

    // ---- カテゴリフィルタ正規化 ----

    @Test
    void buildCondition_nullのcategoryFilterはemptyになる() {
        ProductSearchCondition condition = service.buildCondition(
                null, null, false,
                List.of(), List.of(),
                null, 1, 15,
                ProductSort.RECOMMENDED,
                null,
                List.of(),
                null
        );
        assertThat(condition.categoryFilter().isEmpty()).isTrue();
    }
}
