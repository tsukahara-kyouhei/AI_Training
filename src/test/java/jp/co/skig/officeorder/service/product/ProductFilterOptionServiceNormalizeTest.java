package jp.co.skig.officeorder.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link ProductFilterOptionService#normalizeSearchTasteNames(List, List)} の単体テスト。
 */
class ProductFilterOptionServiceNormalizeTest {

    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        service = new ProductFilterOptionService(mock(jp.co.skig.officeorder.repository.ProductFilterOptionRepository.class));
    }

    @Test
    void normalizeSearchTasteNames_nullInput_returnsEmpty() {
        List<String> validNames = List.of("ナチュラル", "モダン");
        assertThat(service.normalizeSearchTasteNames(null, validNames)).isEmpty();
    }

    @Test
    void normalizeSearchTasteNames_emptyInput_returnsEmpty() {
        List<String> validNames = List.of("ナチュラル", "モダン");
        assertThat(service.normalizeSearchTasteNames(List.of(), validNames)).isEmpty();
    }

    @Test
    void normalizeSearchTasteNames_validNames_returnsFiltered() {
        List<String> validNames = List.of("ナチュラル", "モダン", "ヴィンテージ");
        List<String> rawNames = List.of("ナチュラル", "モダン");
        assertThat(service.normalizeSearchTasteNames(rawNames, validNames))
                .containsExactlyInAnyOrder("ナチュラル", "モダン");
    }

    @Test
    void normalizeSearchTasteNames_unknownNamesExcluded() {
        List<String> validNames = List.of("ナチュラル", "モダン");
        List<String> rawNames = List.of("ナチュラル", "存在しないテイスト");
        assertThat(service.normalizeSearchTasteNames(rawNames, validNames))
                .containsExactly("ナチュラル");
    }

    @Test
    void normalizeSearchTasteNames_duplicatesRemoved() {
        List<String> validNames = List.of("ナチュラル", "モダン");
        List<String> rawNames = List.of("ナチュラル", "ナチュラル", "モダン");
        assertThat(service.normalizeSearchTasteNames(rawNames, validNames))
                .containsExactlyInAnyOrder("ナチュラル", "モダン")
                .hasSize(2);
    }

    @Test
    void normalizeSearchTasteNames_allInvalid_returnsEmpty() {
        List<String> validNames = List.of("ナチュラル");
        List<String> rawNames = List.of("不正な値", "ハック試み");
        assertThat(service.normalizeSearchTasteNames(rawNames, validNames)).isEmpty();
    }
}
