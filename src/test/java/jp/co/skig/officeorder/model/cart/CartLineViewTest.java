package jp.co.skig.officeorder.model.cart;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartLineViewTest {

    /**
     * 組立・設置対象商品では、商品小計と組立小計を合算した税抜小計が正しく計算されることを確認する。
     */
    @Test
    void lineSubtotalMethods_calculateProductAndAssemblyAmounts() {
        CartLineView line = new CartLineView(
                10L,
                1L,
                "Nordis ワークデスク",
                "P0001-C01",
                "ホワイト",
                new BigDecimal("50000"),
                8,
                true,
                new BigDecimal("3000"),
                true,
                2,
                "/products/1"
        );

        assertThat(line.unitPriceText()).isEqualTo("50,000");
        assertThat(line.assemblyFeePerUnitText()).isEqualTo("3,000");
        assertThat(line.lineProductSubtotal()).isEqualByComparingTo("100000");
        assertThat(line.lineAssemblySubtotal()).isEqualByComparingTo("6000");
        assertThat(line.lineSubtotalBeforeTax()).isEqualByComparingTo("106000");
        assertThat(line.lineAssemblySubtotalText()).isEqualTo("6,000");
        assertThat(line.lineSubtotalBeforeTaxText()).isEqualTo("106,000");
    }

    /**
     * 組立・設置対象外または未希望の場合は、組立費小計を 0 円として扱うことを確認する。
     */
    @Test
    void lineAssemblySubtotal_returnsZeroWhenAssemblyIsNotApplied() {
        CartLineView line = new CartLineView(
                11L,
                2L,
                "Lattice チェア",
                "P0002-C01",
                "ブラック",
                new BigDecimal("18000"),
                5,
                false,
                new BigDecimal("3000"),
                false,
                3,
                "/products/2"
        );

        assertThat(line.lineAssemblySubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line.lineSubtotalBeforeTax()).isEqualByComparingTo("54000");
    }
}
