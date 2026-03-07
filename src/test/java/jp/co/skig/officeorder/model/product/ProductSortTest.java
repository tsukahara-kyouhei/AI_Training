package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSortTest {

    /**
     * 並び順コードは大文字小文字を区別せずに解決できることを確認する。
     */
    @Test
    void fromValue_resolvesCaseInsensitiveValue() {
        assertThat(ProductSort.fromValue("PRICE_ASC", ProductSort.RECOMMENDED))
                .isEqualTo(ProductSort.PRICE_ASC);
    }

    /**
     * 空値や未知の並び順コードでは、呼び出し側が指定した既定値へフォールバックすることを確認する。
     */
    @Test
    void fromValue_returnsDefaultForBlankOrUnknownValue() {
        assertThat(ProductSort.fromValue(null, ProductSort.NEWEST)).isEqualTo(ProductSort.NEWEST);
        assertThat(ProductSort.fromValue("unknown", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.RECOMMENDED);
    }
}
