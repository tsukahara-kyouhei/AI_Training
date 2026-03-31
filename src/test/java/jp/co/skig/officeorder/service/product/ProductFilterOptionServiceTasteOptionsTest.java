package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductFilterOptionService} のテイスト選択肢統合に関する単体テスト。
 * loadOptionsBundle / buildDeskFilter / buildChairFilter / buildStorageFilter を検証する。
 */
class ProductFilterOptionServiceTasteOptionsTest {

    private ProductFilterOptionRepository repository;
    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductFilterOptionRepository.class);
        service = new ProductFilterOptionService(repository);

        when(repository.findActiveColorOptions()).thenReturn(List.of());
        when(repository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(repository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(repository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(repository.findActiveStorageUsageOptions()).thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // loadOptionsBundle の findActiveTasteOptions 委譲確認
    // -----------------------------------------------------------------------

    @Test
    void loadOptionsBundle_delegatesToFindActiveTasteOptions() {
        List<CategoryFilterOption> expected = List.of(
                new CategoryFilterOption(1, "ベーシック"),
                new CategoryFilterOption(2, "モダン")
        );
        when(repository.findActiveTasteOptions()).thenReturn(expected);

        ProductFilterOptionsBundle bundle = service.loadOptionsBundle();

        assertThat(bundle.tasteOptions()).isEqualTo(expected);
        verify(repository).findActiveTasteOptions();
    }

    @Test
    void loadOptionsBundle_noDeskTasteOptions_noChairTasteOptions_noStorageTasteOptions() {
        when(repository.findActiveTasteOptions()).thenReturn(List.of());

        ProductFilterOptionsBundle bundle = service.loadOptionsBundle();

        // 統合後のフィールドにアクセスできること
        assertThat(bundle.tasteOptions()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // buildDeskFilter - tasteIds
    // -----------------------------------------------------------------------

    @Test
    void buildDeskFilter_validTasteId_includedInTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(1, "ベーシック"));
        ProductFilterOptionsBundle bundle = bundleWith(tasteOptions);

        ProductCategoryFilter filter = service.buildDeskFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(1), bundle
        );

        assertThat(filter.tasteIds()).containsExactly(1);
    }

    @Test
    void buildDeskFilter_invalidTasteId_excludedFromTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(1, "ベーシック"));
        ProductFilterOptionsBundle bundle = bundleWith(tasteOptions);

        ProductCategoryFilter filter = service.buildDeskFilter(
                List.of(), List.of(), List.of(), List.of(), List.of(999), bundle
        );

        assertThat(filter.tasteIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // buildChairFilter - tasteIds
    // -----------------------------------------------------------------------

    @Test
    void buildChairFilter_validTasteId_includedInTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(2, "モダン"));
        List<CategoryFilterOption> chairFuncOptions = List.of(new CategoryFilterOption(1, "肘付き"));
        List<CategoryFilterOption> chairMatOptions = List.of(new CategoryFilterOption(1, "メッシュ"));
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), chairFuncOptions, chairMatOptions, List.of(), tasteOptions
        );

        ProductCategoryFilter filter = service.buildChairFilter(
                List.of(), List.of(), List.of(2), bundle
        );

        assertThat(filter.tasteIds()).containsExactly(2);
    }

    @Test
    void buildChairFilter_invalidTasteId_excludedFromTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(2, "モダン"));
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), tasteOptions
        );

        ProductCategoryFilter filter = service.buildChairFilter(
                List.of(), List.of(), List.of(999), bundle
        );

        assertThat(filter.tasteIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // buildStorageFilter - tasteIds
    // -----------------------------------------------------------------------

    @Test
    void buildStorageFilter_validTasteId_includedInTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(3, "シンプル"));
        List<CategoryFilterOption> usageOptions = List.of(new CategoryFilterOption(1, "書類収納"));
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), usageOptions, tasteOptions
        );

        ProductCategoryFilter filter = service.buildStorageFilter(List.of(), List.of(3), bundle);

        assertThat(filter.tasteIds()).containsExactly(3);
    }

    @Test
    void buildStorageFilter_invalidTasteId_excludedFromTasteIds() {
        List<CategoryFilterOption> tasteOptions = List.of(new CategoryFilterOption(3, "シンプル"));
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), tasteOptions
        );

        ProductCategoryFilter filter = service.buildStorageFilter(List.of(), List.of(999), bundle);

        assertThat(filter.tasteIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private ProductFilterOptionsBundle bundleWith(List<CategoryFilterOption> tasteOptions) {
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(), List.of(), List.of(), tasteOptions
        );
    }
}
