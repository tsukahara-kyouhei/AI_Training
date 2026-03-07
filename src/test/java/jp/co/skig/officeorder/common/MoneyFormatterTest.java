package jp.co.skig.officeorder.common;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatterTest {

    /**
     * 金額が未設定でも、画面表示では 0 円として整形されることを確認する。
     */
    @Test
    void formatYen_returnsZeroWhenValueIsNull() {
        assertThat(MoneyFormatter.formatYen(null)).isEqualTo("0");
    }

    /**
     * 小数部を切り捨てたうえで、3桁区切りの円表示に整形されることを確認する。
     */
    @Test
    void formatYen_formatsWithGroupingAndTruncatesFraction() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("1234567.89"))).isEqualTo("1,234,567");
    }

    /**
     * マイナス金額でも符号を維持したまま桁区切りされることを確認する。
     */
    @Test
    void formatYen_formatsNegativeValues() {
        assertThat(MoneyFormatter.formatYen(new BigDecimal("-1000.99"))).isEqualTo("-1,000");
    }
}
