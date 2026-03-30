package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ProductFilterOptionService の単体テスト。
 *
 * <p>デスク一覧のフィルター組み立て（幅レンジID境界値・許可外除外）、
 * チェア一覧のフィルター組み立て（素材ID除外）、
 * 収納家具一覧の全ID null 時の空フィルター返却、
 * loadOptionsBundle によるリポジトリ委譲を検証する。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    private ProductFilterOptionService productFilterOptionService;

    @BeforeEach
    void setUp() {
        productFilterOptionService = new ProductFilterOptionService(productFilterOptionRepository);
    }

    // -----------------------------------------------------------------------
    // 正常系：buildDeskFilter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buildDeskFilter で許可外幅レンジIDが除外される")
    void buildDeskFilter_excludesInvalidWidthBandIds_fromResult() {
        // 許可幅レンジID は 1〜11。12 と 0 は除外
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions(
                List.of(new CategoryFilterOption(1, "長方形")),  // deskTopShape
                List.of()  // deskTaste
        );

        ProductCategoryFilter filter = productFilterOptionService.buildDeskFilter(
                List.of(), List.of(1, 5, 12, 0), List.of(), List.of(), List.of(), bundle
        );

        assertThat(filter.deskWidthBandIds()).containsExactlyInAnyOrder(1, 5);
    }

    @Test
    @DisplayName("buildDeskFilter でデスク幅許可レンジID境界値（1, 11）が通過する")
    void buildDeskFilter_allowsBoundaryWidthBandIds_whenIdIs1or11() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions(List.of(), List.of());

        ProductCategoryFilter filter = productFilterOptionService.buildDeskFilter(
                List.of(), List.of(1, 11), List.of(), List.of(), List.of(), bundle
        );

        assertThat(filter.deskWidthBandIds()).containsExactlyInAnyOrder(1, 11);
    }

    // -----------------------------------------------------------------------
    // 正常系：buildChairFilter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buildChairFilter で許可外素材IDが除外される")
    void buildChairFilter_excludesInvalidMaterialIds_fromResult() {
        // 許可素材は id=1 のみ。id=99 は除外
        ProductFilterOptionsBundle bundle = buildBundleWithChairOptions(
                List.of(),                                           // function
                List.of(new CategoryFilterOption(1, "メッシュ")),    // material
                List.of()                                            // taste
        );

        ProductCategoryFilter filter = productFilterOptionService.buildChairFilter(
                List.of(), List.of(1, 99), List.of(), bundle
        );

        assertThat(filter.chairMaterialIds()).containsExactly(1);
    }

    // -----------------------------------------------------------------------
    // 正常系：buildStorageFilter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buildStorageFilter で全ID nullの場合は空フィルターが返る")
    void buildStorageFilter_returnsEmptyFilter_whenAllIdsAreNull() {
        ProductFilterOptionsBundle bundle = buildBundleWithStorageOptions(List.of(), List.of());

        ProductCategoryFilter filter = productFilterOptionService.buildStorageFilter(
                null, null, bundle
        );

        assertThat(filter.storageUsageIds()).isEmpty();
        assertThat(filter.storageTasteIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // 正常系：loadOptionsBundle
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("loadOptionsBundle でリポジトリの全候補が詰め込まれて返る")
    void loadOptionsBundle_populatesAllOptionsFromRepository() {
        var colorOpt = new ColorFilterOption("1", "ホワイト", "#FFF");
        var deskShape = new CategoryFilterOption(1, "長方形");
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of(colorOpt));
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of(deskShape));
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());

        ProductFilterOptionsBundle bundle = productFilterOptionService.loadOptionsBundle();

        assertThat(bundle.colorOptions()).containsExactly(colorOpt);
        assertThat(bundle.deskTopShapeOptions()).containsExactly(deskShape);
    }

    // -----------------------------------------------------------------------
    // 境界値：デスク幅許可レンジIDの境界
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("デスク幅レンジID 1（下限）は通過し、0と-1は除外される")
    void buildDeskFilter_allowsWidthBandId1_rejectsZeroAndNegative() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions(List.of(), List.of());

        ProductCategoryFilter filter = productFilterOptionService.buildDeskFilter(
                List.of(), List.of(-1, 0, 1), List.of(), List.of(), List.of(), bundle
        );

        assertThat(filter.deskWidthBandIds()).containsExactly(1);
    }

    @Test
    @DisplayName("デスク幅レンジID 11（上限）は通過し、12は除外される")
    void buildDeskFilter_allowsWidthBandId11_rejects12() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions(List.of(), List.of());

        ProductCategoryFilter filter = productFilterOptionService.buildDeskFilter(
                List.of(), List.of(11, 12), List.of(), List.of(), List.of(), bundle
        );

        assertThat(filter.deskWidthBandIds()).containsExactly(11);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    private ProductFilterOptionsBundle buildBundleWithDeskOptions(
            List<CategoryFilterOption> deskTopShapeOptions,
            List<CategoryFilterOption> deskTasteOptions) {
        return new ProductFilterOptionsBundle(
                List.of(),
                deskTopShapeOptions,
                deskTasteOptions,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithChairOptions(
            List<CategoryFilterOption> chairFunctionOptions,
            List<CategoryFilterOption> chairMaterialOptions,
            List<CategoryFilterOption> chairTasteOptions) {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(),
                chairFunctionOptions, chairMaterialOptions, chairTasteOptions,
                List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithStorageOptions(
            List<CategoryFilterOption> storageUsageOptions,
            List<CategoryFilterOption> storageTasteOptions) {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                storageUsageOptions, storageTasteOptions, List.of()
        );
    }
}
