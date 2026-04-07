package jp.co.skig.officeorder.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NormalizationUtilsTest {

    @Test
    void normalize_fullWidthUpperAlpha() {
        assertEquals("ABCD", NormalizationUtils.normalize("ＡＢＣＤ"));
    }

    @Test
    void normalize_fullWidthLowerAlpha() {
        assertEquals("abcd", NormalizationUtils.normalize("ａｂｃｄ"));
    }

    @Test
    void normalize_fullWidthDigits() {
        assertEquals("0123", NormalizationUtils.normalize("０１２３"));
    }

    @Test
    void normalize_halfKatakanaDakuon() {
        assertEquals("デスク", NormalizationUtils.normalize("ﾃﾞｽｸ"));
    }

    @Test
    void normalize_halfKatakanaDakuonSingle() {
        assertEquals("ガ", NormalizationUtils.normalize("ｶﾞ"));
    }

    @Test
    void normalize_halfKatakanaHandakuon() {
        assertEquals("パ", NormalizationUtils.normalize("ﾊﾟ"));
    }

    @Test
    void normalize_halfKatakanaLongVowel() {
        assertEquals("コンピューター", NormalizationUtils.normalize("ｺﾝﾋﾟｭｰﾀｰ"));
    }

    @Test
    void normalize_mixedInput() {
        assertEquals("A1デスクabc", NormalizationUtils.normalize("Ａ１ﾃﾞｽｸabc"));
    }

    @Test
    void normalize_halfWidthAlphaNumericUnchanged() {
        assertEquals("ABC123", NormalizationUtils.normalize("ABC123"));
    }

    @Test
    void normalize_fullWidthKatakanaUnchanged() {
        assertEquals("デスク", NormalizationUtils.normalize("デスク"));
    }

    @Test
    void normalize_nullInput() {
        assertNull(NormalizationUtils.normalize(null));
    }

    @Test
    void normalize_emptyString() {
        assertEquals("", NormalizationUtils.normalize(""));
    }

    @Test
    void normalize_hiraganaAndKanjiUnchanged() {
        assertEquals("机 つくえ", NormalizationUtils.normalize("机 つくえ"));
    }
}
