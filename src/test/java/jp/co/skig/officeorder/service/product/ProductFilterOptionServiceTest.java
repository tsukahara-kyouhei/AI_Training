package jp.co.skig.officeorder.service.product;

import java.util.Arrays;
import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductFilterOptionServiceTest {

    private final ProductFilterOptionRepository repository = mock(ProductFilterOptionRepository.class);
    private final ProductFilterOptionService service = new ProductFilterOptionService(repository);

    /**
     * 価格帯IDの正規化で、未定義値と重複値が除去されることを確認する。
     */
    @Test
    void normalizePriceBandIds_keepsOnlyKnownDistinctIds() {
        List<Integer> normalized = service.normalizePriceBandIds(Arrays.asList(1, null, 1, 6, 99));

        assertThat(normalized).containsExactly(1, 6);
    }

    /**
     * カラーキーの正規化と color_id 解決が、画面で選択可能な候補だけを対象に行われることを確認する。
     */
    @Test
    void normalizeColorKeys_andResolveColorIds_followConfiguredOptions() {
        ProductFilterOptionsBundle options = optionsBundle();

        List<String> colorKeys = service.normalizeColorKeys(Arrays.asList("1", null, "1", "2", "99"), options);
        List<Long> colorIds = service.resolveColorIds(List.of("1", "x", "2", "2"), options);

        assertThat(colorKeys).containsExactly("1", "2");
        assertThat(colorIds).containsExactly(1L, 2L);
    }

    /**
     * 選択済みカラーIDを画面再表示用のキーへ戻す際に、表示順が候補定義順に揃うことを確認する。
     */
    @Test
    void resolveSelectedColorKeys_returnsKeysInDisplayOrder() {
        List<String> selected = service.resolveSelectedColorKeys(List.of(2L, 1L, 9L), optionsBundle());

        assertThat(selected).containsExactly("1", "2");
    }

    /**
     * デスク絞り込み条件の組み立てで、許容されない値と重複値が除去されることを確認する。
     */
    @Test
    void buildDeskFilter_removesUnknownValuesAndDuplicates() {
        ProductCategoryFilter filter = service.buildDeskFilter(
                List.of(1, 2, 2, 99),
                List.of(1, 1, 11, 12),
                List.of(1, 6, 9),
                List.of(1, 4, 7),
                List.of(10, 10, 99),
                optionsBundle()
        );

        assertThat(filter.deskTopShapeIds()).containsExactly(1, 2);
        assertThat(filter.deskWidthBandIds()).containsExactly(1, 11);
        assertThat(filter.deskDepthBandIds()).containsExactly(1, 6);
        assertThat(filter.deskHeightBandIds()).containsExactly(1, 4);
        assertThat(filter.deskTasteIds()).containsExactly(10);
        assertThat(filter.isEmpty()).isFalse();
    }

    /**
     * チェアと収納家具の絞り込み条件が、それぞれのカテゴリに定義された許容値だけで構築されることを確認する。
     */
    @Test
    void buildChairAndStorageFilter_useEachCategoryAllowedValuesOnly() {
        ProductFilterOptionsBundle options = optionsBundle();

        ProductCategoryFilter chair = service.buildChairFilter(
                List.of(20, 20, 99),
                List.of(30, 31, 99),
                Arrays.asList(40, null, 99),
                options
        );
        ProductCategoryFilter storage = service.buildStorageFilter(
                List.of(50, 50, 99),
                List.of(60, 99),
                options
        );

        assertThat(chair.chairFunctionIds()).containsExactly(20);
        assertThat(chair.chairMaterialIds()).containsExactly(30, 31);
        assertThat(chair.chairTasteIds()).containsExactly(40);
        assertThat(storage.storageUsageIds()).containsExactly(50);
        assertThat(storage.storageTasteIds()).containsExactly(60);
    }

    private ProductFilterOptionsBundle optionsBundle() {
        return new ProductFilterOptionsBundle(
                List.of(
                        new ColorFilterOption("1", "ホワイト", "#ffffff"),
                        new ColorFilterOption("2", "ブラック", "#000000")
                ),
                List.of(
                        new CategoryFilterOption(1, "角形"),
                        new CategoryFilterOption(2, "L字型")
                ),
                List.of(new CategoryFilterOption(10, "モダン")),
                List.of(new CategoryFilterOption(20, "肘付き")),
                List.of(
                        new CategoryFilterOption(30, "メッシュ"),
                        new CategoryFilterOption(31, "木製")
                ),
                List.of(new CategoryFilterOption(40, "シンプル")),
                List.of(new CategoryFilterOption(50, "書類収納")),
                List.of(new CategoryFilterOption(60, "ナチュラル"))
        );
    }
}
