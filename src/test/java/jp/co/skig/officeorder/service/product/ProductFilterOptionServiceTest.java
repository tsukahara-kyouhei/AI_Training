package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link ProductFilterOptionService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    @InjectMocks
    private ProductFilterOptionService service;

    private ProductFilterOptionsBundle bundleWithTastes(List<String> tasteOptions) {
        return new ProductFilterOptionsBundle(
                null, null, null, null, null, null, null, null, tasteOptions
        );
    }

    // 6-D-1
    @Test
    void loadOptionsBundleIncludesSearchTasteOptions() {
        List<String> expectedTastes = List.of("ベーシック", "ナチュラル", "モダン");
        when(productFilterOptionRepository.findActiveColorOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTopShapeOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveDeskTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairFunctionOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairMaterialOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveChairTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageUsageOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveStorageTasteOptions()).thenReturn(List.of());
        when(productFilterOptionRepository.findActiveSearchTasteOptions()).thenReturn(expectedTastes);

        ProductFilterOptionsBundle bundle = service.loadOptionsBundle();

        assertThat(bundle.searchTasteOptions()).isEqualTo(expectedTastes);
    }

    // 6-D-2
    @Test
    void buildSearchCategoryFilterRetainsOnlyValidNames() {
        ProductFilterOptionsBundle bundle = bundleWithTastes(List.of("ベーシック", "ナチュラル", "モダン"));
        List<String> rawTasteNames = List.of("ベーシック", "不正値", "ナチュラル");

        ProductCategoryFilter filter = service.buildSearchCategoryFilter(rawTasteNames, bundle);

        assertThat(filter.searchTasteNames()).containsExactlyInAnyOrder("ベーシック", "ナチュラル");
    }

    // 6-D-3
    @Test
    void buildSearchCategoryFilterWithNullReturnsEmpty() {
        ProductFilterOptionsBundle bundle = bundleWithTastes(List.of("ベーシック"));

        ProductCategoryFilter filterFromNull = service.buildSearchCategoryFilter(null, bundle);
        ProductCategoryFilter filterFromEmpty = service.buildSearchCategoryFilter(List.of(), bundle);

        assertThat(filterFromNull.searchTasteNames()).isEmpty();
        assertThat(filterFromEmpty.searchTasteNames()).isEmpty();
    }

    // 6-D-4
    @Test
    void buildSearchCategoryFilterRemovesDuplicates() {
        ProductFilterOptionsBundle bundle = bundleWithTastes(List.of("ベーシック", "ナチュラル"));
        List<String> rawTasteNames = List.of("ベーシック", "ベーシック");

        ProductCategoryFilter filter = service.buildSearchCategoryFilter(rawTasteNames, bundle);

        assertThat(filter.searchTasteNames()).containsExactly("ベーシック");
        assertThat(filter.searchTasteNames()).hasSize(1);
    }
}
