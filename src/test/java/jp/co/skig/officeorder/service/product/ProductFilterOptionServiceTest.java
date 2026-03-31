package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductFilterOptionService} の単体テスト。
 */
class ProductFilterOptionServiceTest {

    private ProductFilterOptionRepository repository;
    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductFilterOptionRepository.class);
        service = new ProductFilterOptionService(repository);
    }

    // -----------------------------------------------------------------------
    // normalizeSearchTasteNames
    // -----------------------------------------------------------------------

    @Test
    void normalizeSearchTasteNames_nullInput_returnsEmpty() {
        assertThat(service.normalizeSearchTasteNames(null, List.of("ナチュラル"))).isEmpty();
    }

    @Test
    void normalizeSearchTasteNames_emptyInput_returnsEmpty() {
        assertThat(service.normalizeSearchTasteNames(List.of(), List.of("ナチュラル"))).isEmpty();
    }

    @Test
    void normalizeSearchTasteNames_allValidNames_returnsAll() {
        List<String> valid = List.of("ナチュラル", "ベーシック", "モダン");
        List<String> raw = List.of("ナチュラル", "ベーシック");
        assertThat(service.normalizeSearchTasteNames(raw, valid))
                .containsExactlyInAnyOrder("ナチュラル", "ベーシック");
    }

    @Test
    void normalizeSearchTasteNames_invalidNamesFiltered() {
        List<String> valid = List.of("ナチュラル");
        List<String> raw = List.of("ナチュラル", "無効値");
        assertThat(service.normalizeSearchTasteNames(raw, valid))
                .containsExactly("ナチュラル");
    }

    @Test
    void normalizeSearchTasteNames_duplicatesDeduped() {
        List<String> valid = List.of("ナチュラル");
        List<String> raw = List.of("ナチュラル", "ナチュラル");
        assertThat(service.normalizeSearchTasteNames(raw, valid))
                .containsExactly("ナチュラル")
                .hasSize(1);
    }

    @Test
    void normalizeSearchTasteNames_allInvalid_returnsEmpty() {
        List<String> valid = List.of("ナチュラル");
        List<String> raw = List.of("無効1", "無効2");
        assertThat(service.normalizeSearchTasteNames(raw, valid)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // loadUnifiedSearchTasteOptions
    // -----------------------------------------------------------------------

    @Test
    void loadUnifiedSearchTasteOptions_delegatesToRepository() {
        List<String> expected = List.of("ナチュラル", "モダン", "ヴィンテージ");
        when(repository.findUnifiedSearchTasteOptions()).thenReturn(expected);

        List<String> result = service.loadUnifiedSearchTasteOptions();

        assertThat(result).isEqualTo(expected);
        verify(repository).findUnifiedSearchTasteOptions();
    }
}
