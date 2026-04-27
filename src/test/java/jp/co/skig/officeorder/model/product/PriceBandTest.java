package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PriceBand} の単体テスト。
 */
class PriceBandTest {

    @Nested
    class FromIdTest {

        @Test
        void 存在しないIDはnullを返す() {
            assertThat(PriceBand.fromId(0)).isNull();
        }

        @Test
        void 範囲外IDはnullを返す() {
            assertThat(PriceBand.fromId(99)).isNull();
        }

        @Test
        void id1はBAND_1を返す() {
            assertThat(PriceBand.fromId(1)).isEqualTo(PriceBand.BAND_1);
        }

        @Test
        void id6はBAND_6を返す() {
            assertThat(PriceBand.fromId(6)).isEqualTo(PriceBand.BAND_6);
        }

        @Test
        void すべての有効なIDが正しく解決される() {
            assertThat(PriceBand.fromId(1)).isEqualTo(PriceBand.BAND_1);
            assertThat(PriceBand.fromId(2)).isEqualTo(PriceBand.BAND_2);
            assertThat(PriceBand.fromId(3)).isEqualTo(PriceBand.BAND_3);
            assertThat(PriceBand.fromId(4)).isEqualTo(PriceBand.BAND_4);
            assertThat(PriceBand.fromId(5)).isEqualTo(PriceBand.BAND_5);
            assertThat(PriceBand.fromId(6)).isEqualTo(PriceBand.BAND_6);
        }
    }
}
