package jp.co.skig.officeorder.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NormalizationUtils#normalizeForSearch(String)} の単体テスト。
 */
class NormalizationUtilsTest {

    // ---- NU-01: 全角数字 → 半角数字 ----
    @Test
    void nu01_fullWidthDigits() {
        assertThat(NormalizationUtils.normalizeForSearch("１２３")).isEqualTo("123");
    }

    @Test
    void nu02_allFullWidthDigits() {
        assertThat(NormalizationUtils.normalizeForSearch("０１２３４５６７８９")).isEqualTo("0123456789");
    }

    // ---- NU-03/04: 全角英大文字・小文字 → 半角 ----
    @Test
    void nu03_fullWidthUppercase() {
        assertThat(NormalizationUtils.normalizeForSearch("ＡＢＣ")).isEqualTo("ABC");
    }

    @Test
    void nu04_fullWidthLowercase() {
        assertThat(NormalizationUtils.normalizeForSearch("ａｂｃ")).isEqualTo("abc");
    }

    // ---- NU-05: 全角英大小混在 ----
    @Test
    void nu05_fullWidthMixedCase() {
        assertThat(NormalizationUtils.normalizeForSearch("ＤＳＫ")).isEqualTo("DSK");
    }

    // ---- NU-06: 全角英字 + 数字の複合（商品コード形式） ----
    @Test
    void nu06_fullWidthAlphanumeric() {
        assertThat(NormalizationUtils.normalizeForSearch("ＤＳＫ１００１")).isEqualTo("DSK1001");
    }

    // ---- NU-07: 単純な半角カタカナ → 全角カタカナ ----
    @Test
    void nu07_halfKatakana_simple() {
        assertThat(NormalizationUtils.normalizeForSearch("ﾅﾁｭﾗﾙ")).isEqualTo("ナチュラル");
    }

    // ---- NU-08: 半角カタカナ（濁点合成）→ 全角カタカナ ----
    @Test
    void nu08_halfKatakana_dakuten() {
        assertThat(NormalizationUtils.normalizeForSearch("ﾃﾞｽｸ")).isEqualTo("デスク");
    }

    // ---- NU-09: 小書き文字を含む半角カタカナ ----
    @Test
    void nu09_halfKatakana_smallChar() {
        assertThat(NormalizationUtils.normalizeForSearch("ﾁｪｱ")).isEqualTo("チェア");
    }

    // ---- NU-10: 半角カタカナ ヲ ----
    @Test
    void nu10_halfKatakana_wo() {
        assertThat(NormalizationUtils.normalizeForSearch("ｦ")).isEqualTo("ヲ");
    }

    // ---- NU-11: 半濁点合成 ----
    @Test
    void nu11_halfKatakana_handakuten() {
        assertThat(NormalizationUtils.normalizeForSearch("ﾊﾟｲﾌﾟ")).isEqualTo("パイプ");
    }

    // ---- NU-12: 半角長音符 ----
    @Test
    void nu12_halfKatakana_prolongedSound() {
        assertThat(NormalizationUtils.normalizeForSearch("ｽﾄｰﾚｰｼﾞ")).isEqualTo("ストーレージ");
    }

    // ---- NU-13: 小書きツ（ｯ）----
    @Test
    void nu13_halfKatakana_smallTsu() {
        assertThat(NormalizationUtils.normalizeForSearch("ｿｯｸｽ")).isEqualTo("ソックス");
    }

    // ---- NU-14: 全角ハイフン → 半角 ----
    @Test
    void nu14_fullWidthHyphen() {
        assertThat(NormalizationUtils.normalizeForSearch("DSK－001")).isEqualTo("DSK-001");
    }

    // ---- NU-15: 全角括弧 → 半角 ----
    @Test
    void nu15_fullWidthParentheses() {
        assertThat(NormalizationUtils.normalizeForSearch("（3段）")).isEqualTo("(3段)");
    }

    // ---- NU-16: 半角英字はそのまま ----
    @Test
    void nu16_halfWidthAlpha_unchanged() {
        assertThat(NormalizationUtils.normalizeForSearch("DSK")).isEqualTo("DSK");
    }

    // ---- NU-17: 半角数字はそのまま ----
    @Test
    void nu17_halfWidthDigits_unchanged() {
        assertThat(NormalizationUtils.normalizeForSearch("1001")).isEqualTo("1001");
    }

    // ---- NU-18: 複合変換（商品コード形式） ----
    @Test
    void nu18_complexConversion_productCode() {
        assertThat(NormalizationUtils.normalizeForSearch("ＤＳＫ－１００１")).isEqualTo("DSK-1001");
    }

    // ---- NU-19: 半角カタカナ + 全角英字の複合 ----
    @Test
    void nu19_complexConversion_katakanaAndAlpha() {
        assertThat(NormalizationUtils.normalizeForSearch("ﾅﾁｭﾗﾙＡ")).isEqualTo("ナチュラルA");
    }

    // ---- NU-20: null はそのまま ----
    @Test
    void nu20_null_returnsNull() {
        assertThat(NormalizationUtils.normalizeForSearch(null)).isNull();
    }

    // ---- NU-21: 空文字はそのまま ----
    @Test
    void nu21_emptyString_returnsEmpty() {
        assertThat(NormalizationUtils.normalizeForSearch("")).isEqualTo("");
    }

    // ---- NU-22: 全角カタカナはそのまま ----
    @Test
    void nu22_fullWidthKatakana_unchanged() {
        assertThat(NormalizationUtils.normalizeForSearch("デスク")).isEqualTo("デスク");
    }

    // ---- NU-23: ひらがなはそのまま ----
    @Test
    void nu23_hiragana_unchanged() {
        assertThat(NormalizationUtils.normalizeForSearch("あいう")).isEqualTo("あいう");
    }

    // ---- NU-24: 漢字はそのまま ----
    @Test
    void nu24_kanji_unchanged() {
        assertThat(NormalizationUtils.normalizeForSearch("収納家具")).isEqualTo("収納家具");
    }

    // ---- NU-25: 混合文字列の総合変換 ----
    @Test
    void nu25_mixed_comprehensive() {
        // "ﾅﾁｭﾗﾙ デスク" (半角カタカナ + 全角スペース + 全角カタカナ)
        assertThat(NormalizationUtils.normalizeForSearch("ﾅﾁｭﾗﾙ　デスク")).isEqualTo("ナチュラル　デスク");
    }
}
