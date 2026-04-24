package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private ProductFilterOptionsBundle bundle;

    @BeforeEach
    void setUp() {
        bundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("black", "ブラック", "#000000")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(1, "ベーシック"),
                        new CategoryFilterOption(2, "カジュアル"),
                        new CategoryFilterOption(3, "モダン")
                )
        );
    }

    // ---- normalizeTasteIds ----

    @Test
    void normalizeTasteIds_nullはemptyを返す() {
        assertThat(service.normalizeTasteIds(null, bundle)).isEmpty();
    }

    @Test
    void normalizeTasteIds_空リストはemptyを返す() {
        assertThat(service.normalizeTasteIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void normalizeTasteIds_有効なIDはLongリストに変換される() {
        List<Long> result = service.normalizeTasteIds(List.of(1, 2), bundle);
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void normalizeTasteIds_無効なIDは除外される() {
        List<Long> result = service.normalizeTasteIds(List.of(99), bundle);
        assertThat(result).isEmpty();
    }

    // ---- resolveSelectedTasteIds ----

    @Test
    void resolveSelectedTasteIds_nullはemptyを返す() {
        assertThat(service.resolveSelectedTasteIds(null, bundle)).isEmpty();
    }

    @Test
    void resolveSelectedTasteIds_空リストはemptyを返す() {
        assertThat(service.resolveSelectedTasteIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void resolveSelectedTasteIds_選択済みIDはオプションの順序で返す() {
        // 逆順で渡しても、オプション定義順 (1→2) で返される
        List<Long> result = service.resolveSelectedTasteIds(List.of(2L, 1L), bundle);
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void resolveSelectedTasteIds_オプションに存在しないIDは除外される() {
        List<Long> result = service.resolveSelectedTasteIds(List.of(1L, 99L), bundle);
        assertThat(result).containsExactly(1L);
    }
}
