package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    private ProductFilterOptionService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductFilterOptionService(productFilterOptionRepository);
    }

    // ─── normalizePriceBandIds ───────────────────────────────────────────

    @Test
    void normalizePriceBandIds_null_input_returns_empty_list() {
        assertThat(sut.normalizePriceBandIds(null)).isEmpty();
    }

    @Test
    void normalizePriceBandIds_empty_list_returns_empty_list() {
        assertThat(sut.normalizePriceBandIds(List.of())).isEmpty();
    }

    @Test
    void normalizePriceBandIds_valid_ids_only_returns_those_ids() {
        assertThat(sut.normalizePriceBandIds(List.of(1, 2, 3))).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void normalizePriceBandIds_invalid_id_is_filtered_out() {
        assertThat(sut.normalizePriceBandIds(List.of(0, 1, 999))).containsExactly(1);
    }

    @Test
    void normalizePriceBandIds_null_element_is_filtered_out() {
        List<Integer> input = new java.util.ArrayList<>();
        input.add(1);
        input.add(null);
        input.add(2);
        assertThat(sut.normalizePriceBandIds(input)).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void normalizePriceBandIds_duplicate_ids_are_deduplicated() {
        assertThat(sut.normalizePriceBandIds(List.of(1, 1, 2))).containsExactlyInAnyOrder(1, 2);
    }

    // ─── normalizeColorKeys ──────────────────────────────────────────────

    @Test
    void normalizeColorKeys_null_input_returns_empty_list() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("1", "ホワイト", "#FFFFFF")
        ));

        // Act / Assert
        assertThat(sut.normalizeColorKeys(null, bundle)).isEmpty();
    }

    @Test
    void normalizeColorKeys_valid_key_returns_that_key() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("1", "ホワイト", "#FFFFFF"),
                new ColorFilterOption("2", "ブラック", "#000000")
        ));

        // Act
        List<String> result = sut.normalizeColorKeys(List.of("1"), bundle);

        // Assert
        assertThat(result).containsExactly("1");
    }

    @Test
    void normalizeColorKeys_key_not_in_options_is_filtered_out() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("1", "ホワイト", "#FFFFFF")
        ));

        // Act
        List<String> result = sut.normalizeColorKeys(List.of("999", "1"), bundle);

        // Assert
        assertThat(result).containsExactly("1");
    }

    @Test
    void normalizeColorKeys_duplicate_keys_are_deduplicated() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("1", "ホワイト", "#FFFFFF")
        ));

        // Act
        List<String> result = sut.normalizeColorKeys(List.of("1", "1"), bundle);

        // Assert
        assertThat(result).containsExactly("1");
    }

    // ─── resolveColorIds ─────────────────────────────────────────────────

    @Test
    void resolveColorIds_empty_keys_returns_empty_list() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of());

        // Act / Assert
        assertThat(sut.resolveColorIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void resolveColorIds_valid_numeric_key_returns_parsed_id() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("42", "ベージュ", "#F5F5DC")
        ));

        // Act
        List<Long> result = sut.resolveColorIds(List.of("42"), bundle);

        // Assert
        assertThat(result).containsExactly(42L);
    }

    @Test
    void resolveColorIds_non_numeric_key_is_filtered_out() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of(
                new ColorFilterOption("white", "ホワイト", "#FFFFFF")
        ));

        // Act
        List<Long> result = sut.resolveColorIds(List.of("white"), bundle);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─── allTasteDisplayNames ─────────────────────────────────────────────

    @Test
    void allTasteDisplayNames_no_duplicates_returns_all_in_desk_chair_storage_order() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(1, "シンプル")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(2, "ナチュラル")),
                List.of(),
                List.of(new CategoryFilterOption(3, "モダン"))
        );

        // Act
        List<String> result = sut.allTasteDisplayNames(bundle);

        // Assert
        assertThat(result).containsExactly("シンプル", "ナチュラル", "モダン");
    }

    @Test
    void allTasteDisplayNames_duplicates_across_categories_are_deduplicated_preserving_first_occurrence() {
        // Arrange - 「シンプル」がデスクとチェアの両方に存在する
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(1, "シンプル")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(2, "シンプル"), new CategoryFilterOption(3, "ナチュラル")),
                List.of(),
                List.of()
        );

        // Act
        List<String> result = sut.allTasteDisplayNames(bundle);

        // Assert
        assertThat(result).containsExactly("シンプル", "ナチュラル");
    }

    @Test
    void allTasteDisplayNames_all_categories_empty_returns_empty_list() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );

        // Act / Assert
        assertThat(sut.allTasteDisplayNames(bundle)).isEmpty();
    }

    // ─── resolveTasteFilter ──────────────────────────────────────────────

    @Test
    void resolveTasteFilter_null_display_names_returns_empty_filter() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of());

        // Act
        ProductCategoryFilter result = sut.resolveTasteFilter(null, bundle);

        // Assert
        assertThat(result).isEqualTo(ProductCategoryFilter.empty());
    }

    @Test
    void resolveTasteFilter_empty_display_names_returns_empty_filter() {
        // Arrange
        ProductFilterOptionsBundle bundle = buildBundle(List.of());

        // Act
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of(), bundle);

        // Assert
        assertThat(result).isEqualTo(ProductCategoryFilter.empty());
    }

    @Test
    void resolveTasteFilter_matching_desk_taste_populates_desk_taste_ids() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(10, "シンプル"), new CategoryFilterOption(11, "ナチュラル")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        // Act
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("シンプル"), bundle);

        // Assert
        assertThat(result.deskTasteIds()).containsExactly(10);
        assertThat(result.chairTasteIds()).isEmpty();
        assertThat(result.storageTasteIds()).isEmpty();
    }

    @Test
    void resolveTasteFilter_matching_across_all_categories_populates_respective_ids() {
        // Arrange
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(10, "シンプル")),
                List.of(),
                List.of(),
                List.of(new CategoryFilterOption(20, "シンプル")),
                List.of(),
                List.of(new CategoryFilterOption(30, "シンプル"))
        );

        // Act
        ProductCategoryFilter result = sut.resolveTasteFilter(List.of("シンプル"), bundle);

        // Assert
        assertThat(result.deskTasteIds()).containsExactly(10);
        assertThat(result.chairTasteIds()).containsExactly(20);
        assertThat(result.storageTasteIds()).containsExactly(30);
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private ProductFilterOptionsBundle buildBundle(List<ColorFilterOption> colorOptions) {
        return new ProductFilterOptionsBundle(
                colorOptions, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    // ─── loadOptionsBundle ───────────────────────────────────────────────

    @Test
    void loadOptionsBundle_calls_all_repository_find_methods() {
        // Arrange
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act
        ProductFilterOptionsBundle result = sut.loadOptionsBundle();

        // Assert
        assertThat(result).isNotNull();
        verify(productFilterOptionRepository).findActiveColorOptions();
        verify(productFilterOptionRepository).findActiveDeskTopShapeOptions();
    }

    // ─── buildDeskFilter (without bundle) ───────────────────────────────

    @Test
    void buildDeskFilter_without_bundle_calls_load_options_bundle() {
        // Arrange - stub all 8 repository calls made by loadOptionsBundle
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act
        ProductCategoryFilter result = sut.buildDeskFilter(List.of(), List.of(), List.of(), List.of(), List.of());

        // Assert
        assertThat(result).isNotNull();
        verify(productFilterOptionRepository).findActiveDeskTopShapeOptions();
    }

    // ─── buildChairFilter (without bundle) ──────────────────────────────

    @Test
    void buildChairFilter_without_bundle_calls_load_options_bundle() {
        // Arrange
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act
        ProductCategoryFilter result = sut.buildChairFilter(List.of(), List.of(), List.of());

        // Assert
        assertThat(result).isNotNull();
        verify(productFilterOptionRepository).findActiveChairFunctionOptions();
    }

    // ─── buildStorageFilter (without bundle) ────────────────────────────

    @Test
    void buildStorageFilter_without_bundle_calls_load_options_bundle() {
        // Arrange
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act
        ProductCategoryFilter result = sut.buildStorageFilter(List.of(), List.of());

        // Assert
        assertThat(result).isNotNull();
        verify(productFilterOptionRepository).findActiveStorageUsageOptions();
    }

    // ─── normalizeColorKeys/resolveColorIds/resolveSelectedColorKeys (without bundle) ──

    @Test
    void normalizeColorKeys_without_bundle_calls_load_options_bundle() {
        // Arrange
        ColorFilterOption option = new ColorFilterOption("1", "ホワイト", "#ffffff");
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of(option));
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act – bundle なしオーバーロードを呼ぶ
        List<String> result = sut.normalizeColorKeys(List.of("1"));

        // Assert
        assertThat(result).containsExactly("1");
    }

    @Test
    void resolveColorIds_without_bundle_calls_load_options_bundle() {
        // Arrange – "42" というキーを持つ ColorFilterOption をスタブ
        ColorFilterOption matchingOption = new ColorFilterOption("42", "ブルー", "#00f");
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of(matchingOption));
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act – bundle なしオーバーロードを呼ぶ
        List<Long> result = sut.resolveColorIds(List.of("42"));

        // Assert – normalizeColorKeys → "42" はカラーオプションに存在 → 42L にパース
        assertThat(result).containsExactly(42L);
    }

    @Test
    void resolveSelectedColorKeys_without_bundle_delegates_to_loadOptionsBundle() {
        // Arrange
        ColorFilterOption option = new ColorFilterOption("42", "ブルー", "#0000ff");
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of(option));
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        // Act – bundle なしオーバーロードを呼ぶ
        List<String> result = sut.resolveSelectedColorKeys(List.of(42L));

        // Assert
        assertThat(result).containsExactly("42");
    }

    @Test
    void resolveSelectedColorKeys_with_bundle_and_non_empty_ids_returns_matching_keys() {
        // Arrange
        ColorFilterOption white = new ColorFilterOption("1", "ホワイト", "#fff");
        ColorFilterOption blue  = new ColorFilterOption("2", "ブルー",   "#00f");
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(white, blue), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );

        // Act – 2 のみ選択
        List<String> result = sut.resolveSelectedColorKeys(List.of(2L), bundle);

        // Assert
        assertThat(result).containsExactly("2");
    }

    // ─── convenience option methods ──────────────────────────────────────

    @Test
    void convenience_option_methods_all_delegate_to_options_bundle() {
        // Arrange – loadOptionsBundle に使われる全 repository をスタブ
        // 各コンビニエンスメソッドが loadOptionsBundle() を呼ぶたびにリポジトリが呼ばれる。
        // UnnecessaryStubbingException を避けるため lenient スタブにする。
        CategoryFilterOption shapeOpt   = new CategoryFilterOption(1, "ストレート");
        CategoryFilterOption deskTaste  = new CategoryFilterOption(2, "モダン");
        CategoryFilterOption chairFunc  = new CategoryFilterOption(3, "肘掛あり");
        CategoryFilterOption chairMat   = new CategoryFilterOption(4, "メッシュ");
        CategoryFilterOption chairTaste = new CategoryFilterOption(5, "カジュアル");
        CategoryFilterOption storeUsage = new CategoryFilterOption(6, "本棚");
        CategoryFilterOption storeTaste = new CategoryFilterOption(7, "ナチュラル");
        ColorFilterOption    colorOpt   = new ColorFilterOption("10", "ブラック",   "#000");

        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of(colorOpt));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of(shapeOpt));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of(deskTaste));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of(chairFunc));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of(chairMat));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of(chairTaste));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of(storeUsage));
        org.mockito.Mockito.lenient().when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of(storeTaste));

        // Act + Assert 各コンビニエンスメソッドが bundle の対応リストを返すこと
        assertThat(sut.colorFilterOptions()).containsExactly(colorOpt);
        assertThat(sut.deskTopShapeOptions()).containsExactly(shapeOpt);
        assertThat(sut.deskTasteOptions()).containsExactly(deskTaste);
        assertThat(sut.chairFunctionOptions()).containsExactly(chairFunc);
        assertThat(sut.chairMaterialOptions()).containsExactly(chairMat);
        assertThat(sut.chairTasteOptions()).containsExactly(chairTaste);
        assertThat(sut.storageUsageOptions()).containsExactly(storeUsage);
        assertThat(sut.storageTasteOptions()).containsExactly(storeTaste);
    }

    // ─── parseLongOrNull (via resolveColorIds) — NumberFormatException path ──

    @Test
    void resolveColorIds_non_parseable_key_is_filtered_out_via_number_format_exception() {
        // Arrange – "abc" は Long.parseLong で NumberFormatException → null にフォールバック
        ColorFilterOption option = new ColorFilterOption("abc", "テスト", "#999");
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(option), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );

        // "abc" は normalizeColorKeys で option に存在 → resolveColorIds で parseLong に失敗 → 除外
        List<Long> result = sut.resolveColorIds(List.of("abc"), bundle);

        assertThat(result).isEmpty();  // NumberFormatException パスでフィルタされる
    }

    // ─── resolveSelectedColorKeys empty ids (L306) ──────────────────────

    @Test
    void resolveSelectedColorKeys_with_empty_ids_returns_empty_list() {
        // resolveSelectedColorKeys で colorIds が空 → L306 return List.of()
        ColorFilterOption option = new ColorFilterOption("1", "ホワイト", "#fff");
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(option), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );

        List<String> result = sut.resolveSelectedColorKeys(List.of(), bundle);

        assertThat(result).isEmpty();
    }

    // ─── extractOptionIds + normalizeIntegerOptions stream path (L404-407, L420-423) ──

    @Test
    void buildDeskFilter_with_non_empty_options_covers_extract_and_normalize_streams() {
        // extractOptionIds (L420-423) と normalizeIntegerOptions (L404-407) の stream 部分をカバー
        CategoryFilterOption shapeOpt = new CategoryFilterOption(1, "ストレート");
        CategoryFilterOption tasteOpt = new CategoryFilterOption(2, "モダン");
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(),         // colorOptions
                List.of(shapeOpt), // deskTopShapeOptions
                List.of(tasteOpt), // deskTasteOptions
                List.of(),         // chairFunctionOptions
                List.of(),         // chairMaterialOptions
                List.of(),         // chairTasteOptions
                List.of(),         // storageUsageOptions
                List.of()          // storageTasteOptions
        );

        // rawDeskTopShapeIds に 1 を渡す → extractOptionIds([shapeOpt]) = [1] → normalizeIntegerOptions([1], [1]) = [1]
        jp.co.skig.officeorder.model.product.ProductCategoryFilter result =
                sut.buildDeskFilter(List.of(1), List.of(), List.of(), List.of(), List.of(), bundle);

        assertThat(result.deskTopShapeIds()).containsExactly(1);
    }
}
