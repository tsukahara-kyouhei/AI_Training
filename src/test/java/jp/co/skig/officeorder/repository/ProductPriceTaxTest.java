package jp.co.skig.officeorder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductPriceTaxTest {

    @Test
    void toTaxExclusivePrice_販売価格が税込価格なら税抜価格を既存ルールで計算する() {
        BigDecimal taxIncludedPrice = new BigDecimal("11000");
        BigDecimal taxRate = new BigDecimal("10");

        BigDecimal result = ProductRepository.toTaxExclusivePrice(taxIncludedPrice, taxRate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void toTaxExclusivePrice_端数は既存のRoundingMode_DOWNで処理する() {
        BigDecimal taxIncludedPrice = new BigDecimal("11001");
        BigDecimal taxRate = new BigDecimal("10");

        BigDecimal result = ProductRepository.toTaxExclusivePrice(taxIncludedPrice, taxRate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("10000"));
    }
}
