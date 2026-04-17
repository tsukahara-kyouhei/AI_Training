package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProductFilterOptionService#normalizeTasteDisplayNames} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        service = new ProductFilterOptionService(productFilterOptionRepository);
    }

    // ---- NT-01: 有効値はすべて通過 ----
    @Test
    void nt01_allValid() {
        List<String> result = service.normalizeTasteDisplayNames(
                Arrays.asList("ナチュラル", "モダン"),
                Arrays.asList("ナチュラル", "モダン", "北欧")
        );
        assertThat(result).containsExactlyInAnyOrder("ナチュラル", "モダン");
    }

    // ---- NT-02: 不正値を除去 ----
    @Test
    void nt02_invalidValueRemoved() {
        List<String> result = service.normalizeTasteDisplayNames(
                Arrays.asList("ナチュラル", "存在しない"),
                Arrays.asList("ナチュラル", "モダン")
        );
        assertThat(result).containsExactly("ナチュラル");
    }

    // ---- NT-03: 重複を除去 ----
    @Test
    void nt03_duplicateRemoved() {
        List<String> result = service.normalizeTasteDisplayNames(
                Arrays.asList("ナチュラル", "ナチュラル"),
                Arrays.asList("ナチュラル")
        );
        assertThat(result).containsExactly("ナチュラル");
    }

    // ---- NT-04: null 入力 → 空リスト ----
    @Test
    void nt04_nullInput_emptyList() {
        List<String> result = service.normalizeTasteDisplayNames(null, Arrays.asList("ナチュラル"));
        assertThat(result).isEmpty();
    }

    // ---- NT-05: 空リスト入力 → 空リスト ----
    @Test
    void nt05_emptyInput_emptyList() {
        List<String> result = service.normalizeTasteDisplayNames(List.of(), Arrays.asList("ナチュラル"));
        assertThat(result).isEmpty();
    }

    // ---- NT-06: 許可リストが空 → 空リスト ----
    @Test
    void nt06_emptyAllowed_emptyList() {
        List<String> result = service.normalizeTasteDisplayNames(
                Arrays.asList("ナチュラル"),
                List.of()
        );
        assertThat(result).isEmpty();
    }

    // ---- NT-07: null 要素を除去 ----
    @Test
    void nt07_nullElementRemoved() {
        List<String> raw = Arrays.asList(null, "ナチュラル");
        List<String> result = service.normalizeTasteDisplayNames(raw, Arrays.asList("ナチュラル"));
        assertThat(result).containsExactly("ナチュラル");
    }
}
