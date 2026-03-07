package jp.co.skig.officeorder.model.cart;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartSummaryViewTest {

    /**
     * カート金額サマリーの各表示値が、日本円の桁区切り表記で統一されることを確認する。
     */
    @Test
    void textMethods_formatSummaryAmountsAsYen() {
        CartSummaryView summary = new CartSummaryView(
                new BigDecimal("120000"),
                new BigDecimal("6000"),
                new BigDecimal("800"),
                new BigDecimal("12680"),
                new BigDecimal("139480")
        );

        assertThat(summary.productSubtotalText()).isEqualTo("120,000");
        assertThat(summary.assemblyFeeTotalText()).isEqualTo("6,000");
        assertThat(summary.shippingFeeText()).isEqualTo("800");
        assertThat(summary.taxAmountText()).isEqualTo("12,680");
        assertThat(summary.totalAmountText()).isEqualTo("139,480");
    }
}
