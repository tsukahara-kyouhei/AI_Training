package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriceBandTest {

    /**
     * 定義済みの価格帯IDから、対応するレンジ情報を復元できることを確認する。
     */
    @Test
    void fromId_returnsMatchingBand() {
        PriceBand band = PriceBand.fromId(2);

        assertThat(band).isEqualTo(PriceBand.BAND_2);
        assertThat(band.min()).isEqualTo(20000L);
        assertThat(band.max()).isEqualTo(40000L);
    }

    /**
     * 未定義の価格帯IDでは null を返し、誤ったレンジへ変換しないことを確認する。
     */
    @Test
    void fromId_returnsNullForUnknownId() {
        assertThat(PriceBand.fromId(999)).isNull();
    }
}
