package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductFilterOptionService の allTasteDisplayNames / resolveTasteFilter に関するテスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTasteTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    private ProductFilterOptionService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductFilterOptionService(productFilterOptionRepository);
    }

    // ─── allTasteDisplayNames ───────────────────────────────────────────

    @Test
    void allTasteDisplayNames_3カテゴリに同名テイストがある場合_重複排除して1件のみ返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル")),
                List.of(opt(2, "ナチュラル")),
                List.of(opt(3, "ナチュラル"))
        );
        assertThat(sut.allTasteDisplayNames(bundle)).containsExactly("ナチュラル");
    }

    @Test
    void allTasteDisplayNames_全カテゴリのテイストを統合してdeskの順序を優先して返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル"), opt(2, "モダン")),
                List.of(opt(3, "シンプル"), opt(4, "モダン")),
                List.of(opt(5, "ナチュラル"), opt(6, "シンプル"))
        );
        assertThat(sut.allTasteDisplayNames(bundle))
                .containsExactly("ナチュラル", "モダン", "シンプル");
    }

    @Test
    void allTasteDisplayNames_全カテゴリのテイストが空のとき_空リストを返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(List.of(), List.of(), List.of());
        assertThat(sut.allTasteDisplayNames(bundle)).isEmpty();
    }

    @Test
    void allTasteDisplayNames_deskTasteOptionsが空でchairに要素があるとき_chairの要素が返ること() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(),
                List.of(opt(1, "シンプル")),
                List.of()
        );
        assertThat(sut.allTasteDisplayNames(bundle)).containsExactly("シンプル");
    }

    // ─── resolveTasteFilter ─────────────────────────────────────────────

    @Test
    void resolveTasteFilter_displayNamesがnullのとき_emptyなProductCategoryFilterを返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(List.of(), List.of(), List.of());
        ProductCategoryFilter result = sut.resolveTasteFilter(null, bundle);
        assertThat(result).isEqualTo(ProductCategoryFilter.empty());
    }

    @Test
    void resolveTasteFilter_displayNamesが空リストのとき_emptyなProductCategoryFilterを返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(List.of(), List.of(), List.of());
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of(), bundle);
        assertThat(result).isEqualTo(ProductCategoryFilter.empty());
    }

    @Test
    void resolveTasteFilter_deskにのみ存在するテイスト名を渡したとき_deskTasteIdsにのみIDがセットされること() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル")),
                List.of(),
                List.of()
        );
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("ナチュラル"), bundle);
        assertThat(result.deskTasteIds()).containsExactly(1);
        assertThat(result.chairTasteIds()).isEmpty();
        assertThat(result.storageTasteIds()).isEmpty();
    }

    @Test
    void resolveTasteFilter_存在しないテイスト名を渡したとき_空リストのフィルターを返すこと() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル")),
                List.of(),
                List.of()
        );
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("存在しない"), bundle);
        assertThat(result.deskTasteIds()).isEmpty();
        assertThat(result.chairTasteIds()).isEmpty();
        assertThat(result.storageTasteIds()).isEmpty();
    }

    @Test
    void resolveTasteFilter_複数テイストを渡したとき_複数IDが解決されること() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル"), opt(2, "モダン")),
                List.of(),
                List.of()
        );
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("ナチュラル", "モダン"), bundle);
        assertThat(result.deskTasteIds()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void resolveTasteFilter_3カテゴリ共通のテイスト名を渡したとき_各カテゴリのIDがそれぞれにセットされること() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル")),
                List.of(opt(3, "ナチュラル")),
                List.of(opt(5, "ナチュラル"))
        );
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("ナチュラル"), bundle);
        assertThat(result.deskTasteIds()).containsExactly(1);
        assertThat(result.chairTasteIds()).containsExactly(3);
        assertThat(result.storageTasteIds()).containsExactly(5);
    }

    @Test
    void resolveTasteFilter_taste以外のfilterフィールドは全て空リストであること() {
        ProductFilterOptionsBundle bundle = bundleOf(
                List.of(opt(1, "ナチュラル")),
                List.of(),
                List.of()
        );
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("ナチュラル"), bundle);
        assertThat(result.deskTopShapeIds()).isEmpty();
        assertThat(result.deskWidthBandIds()).isEmpty();
        assertThat(result.deskDepthBandIds()).isEmpty();
        assertThat(result.deskHeightBandIds()).isEmpty();
        assertThat(result.chairFunctionIds()).isEmpty();
        assertThat(result.chairMaterialIds()).isEmpty();
        assertThat(result.storageUsageIds()).isEmpty();
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────

    private static CategoryFilterOption opt(int id, String label) {
        return new CategoryFilterOption(id, label);
    }

    private static ProductFilterOptionsBundle bundleOf(
            List<CategoryFilterOption> deskTastes,
            List<CategoryFilterOption> chairTastes,
            List<CategoryFilterOption> storageTastes) {
        return new ProductFilterOptionsBundle(
                List.of(), // colorOptions
                List.of(), // deskTopShapeOptions
                deskTastes,
                List.of(), // chairFunctionOptions
                List.of(), // chairMaterialOptions
                chairTastes,
                List.of(), // storageUsageOptions
                storageTastes
        );
    }
}
