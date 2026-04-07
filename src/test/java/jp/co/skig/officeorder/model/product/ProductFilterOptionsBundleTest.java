package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ProductFilterOptionsBundle} のコンパクトコンストラクタ検証。
 */
class ProductFilterOptionsBundleTest {

    // 6-C-1
    @Test
    void nullSearchTasteOptionsConvertedToEmptyList() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                null, null, null, null, null, null, null, null, null
        );
        assertThat(bundle.searchTasteOptions()).isEmpty();
    }

    // 6-C-2
    @Test
    void searchTasteOptionsIsImmutable() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                null, null, null, null, null, null, null, null,
                new ArrayList<>(List.of("ベーシック", "モダン"))
        );
        assertThatThrownBy(() -> bundle.searchTasteOptions().add("追加"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
