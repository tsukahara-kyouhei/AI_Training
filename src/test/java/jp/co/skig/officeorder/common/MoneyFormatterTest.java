package jp.co.skig.officeorder.common;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MoneyFormatter} の単体テスト。
 */
class MoneyFormatterTest {

    @Nested
    class FormatYenTest {

        @Test
        void nullはゼロ円として扱われる() {
            assertThat(MoneyFormatter.formatYen(null)).isEqualTo("0");
        }

        @Test
        void ゼロは0を返す() {
            assertThat(MoneyFormatter.formatYen(BigDecimal.ZERO)).isEqualTo("0");
        }

        @Test
        void 桁数が1桁のときカンマなし() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("1"))).isEqualTo("1");
        }

        @Test
        void 桁数が3桁のときカンマなし() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("999"))).isEqualTo("999");
        }

        @Test
        void 桁数が4桁のとき1カンマ() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("1000"))).isEqualTo("1,000");
        }

        @Test
        void 桁数が6桁のとき1カンマ() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("100000"))).isEqualTo("100,000");
        }

        @Test
        void 桁数が7桁のとき2カンマ() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("1000000"))).isEqualTo("1,000,000");
        }

        @Test
        void 小数点以下は切り捨て() {
            assertThat(MoneyFormatter.formatYen(new BigDecimal("1999.99"))).isEqualTo("1,999");
        }

        @Test
        void 小数点以下は切り上げしない() {
            // 1999.9 → 1999（繰り上げなし）
            assertThat(MoneyFormatter.formatYen(new BigDecimal("1999.9"))).isEqualTo("1,999");
        }
    }
}
