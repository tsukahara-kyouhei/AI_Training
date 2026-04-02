package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductFilterOptionServiceTest {

    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        service = new ProductFilterOptionService(mock(ProductFilterOptionRepository.class));
    }

    @Test
    void buildSearchTasteOptions_カテゴリ横断で重複を除去して順序を維持する() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(1, "モダン"),
                        new CategoryFilterOption(2, "ナチュラル")
                ),
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(3, "モダン"),
                        new CategoryFilterOption(4, "  ヴィンテージ  ")
                ),
                List.of(),
                List.of(
                        new CategoryFilterOption(5, "ナチュラル"),
                        new CategoryFilterOption(6, "")
                )
        );

        List<String> actual = service.buildSearchTasteOptions(bundle);

        assertThat(actual).containsExactly("モダン", "ナチュラル", "ヴィンテージ");
    }

    @Test
    void normalizeSearchTasteNames_候補外と重複を除去する() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(1, "モダン")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(2, "ヴィンテージ")),
                List.of(),
                List.of(new CategoryFilterOption(3, "ナチュラル"))
        );

        List<String> actual = service.normalizeSearchTasteNames(
                List.of(" モダン ", "ヴィンテージ", "不正値", "モダン", "   "),
                bundle
        );

        assertThat(actual).containsExactly("モダン", "ヴィンテージ");
    }

    @Test
    void buildSearchTasteFilter_同じテイスト名をカテゴリ別IDへ変換する() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(11, "モダン"),
                        new CategoryFilterOption(12, "ナチュラル")
                ),
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(21, "ヴィンテージ"),
                        new CategoryFilterOption(22, "モダン")
                ),
                List.of(),
                List.of(
                        new CategoryFilterOption(31, "モダン"),
                        new CategoryFilterOption(32, "カジュアル")
                )
        );

        ProductCategoryFilter actual = service.buildSearchTasteFilter(List.of("モダン", "ヴィンテージ"), bundle);

        assertThat(actual.deskTasteIds()).containsExactly(11);
        assertThat(actual.chairTasteIds()).containsExactly(21, 22);
        assertThat(actual.storageTasteIds()).containsExactly(31);
        assertThat(actual.deskTopShapeIds()).isEmpty();
        assertThat(actual.storageUsageIds()).isEmpty();
    }
}