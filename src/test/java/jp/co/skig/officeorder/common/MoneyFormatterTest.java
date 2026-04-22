package jp.co.skig.officeorder.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MoneyFormatter#formatYen(BigDecimal)} の単体テスト。
 */
class MoneyFormatterTest {

    // ---- MF-01: 0 → "0" ----
    @Test
    void mf01_zero_returnsZero() {
        assertThat(MoneyFormatter.formatYen(BigDecimal.ZERO)).isEqualTo("0");
    }

    // ---- MF-02: null → "0" ----
    @Test
    void mf02_null_returnsZero() {
        assertThat(MoneyFormatter.formatYen(null)).isEqualTo("0");
    }

    // ---- MF-03: 999 (3桁) → カンマなし ----
    @Test
    void mf03_lessThan1000_noComma() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("999"))).isEqualTo("999");
    }

    // ---- MF-04: 1000 → "1,000" ----
    @Test
    void mf04_exactly1000_hasComma() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("1000"))).isEqualTo("1,000");
    }

    // ---- MF-05: 1234567 → "1,234,567" ----
    @Test
    void mf05_largeNumber_multipleCommas() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("1234567"))).isEqualTo("1,234,567");
    }

    // ---- MF-06: 小数点以下は切り捨て (1500.9 → "1,500") ----
    @Test
    void mf06_withDecimal_truncatesDown() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("1500.9"))).isEqualTo("1,500");
    }

    // ---- MF-07: 負数 (-1000) → "-1,000" ----
    @Test
    void mf07_negative1000_formatsCorrectly() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("-1000"))).isEqualTo("-1,000");
    }

    // ---- MF-08: 負数 (-1234567) → "-1,234,567" ----
    @Test
    void mf08_largeNegative_multipleCommas() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("-1234567"))).isEqualTo("-1,234,567");
    }

    // ---- MF-09: 1 → "1" ----
    @Test
    void mf09_one_returnsOne() {
        assertThat(MoneyFormatter.formatYen(BigDecimal.ONE)).isEqualTo("1");
    }

    // ---- MF-10: 10000 → "10,000" ----
    @Test
    void mf10_exactly10000_formatsCorrectly() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("10000"))).isEqualTo("10,000");
    }
}
