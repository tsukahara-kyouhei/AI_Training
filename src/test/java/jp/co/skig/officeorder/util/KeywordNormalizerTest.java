package jp.co.skig.officeorder.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KeywordNormalizer} の正規化ロジック検証。
 */
class KeywordNormalizerTest {

    // 6-A-1
    @Test
    void nullInputReturnsNull() {
        assertThat(KeywordNormalizer.normalize(null)).isNull();
    }

    // 6-A-2
    @Test
    void emptyInputReturnsEmpty() {
        assertThat(KeywordNormalizer.normalize("")).isEmpty();
    }

    // 6-A-3
    @Test
    void fullWidthDigitsConvertedToHalfWidth() {
        assertThat(KeywordNormalizer.normalize("１２３")).isEqualTo("123");
    }

    // 6-A-4
    @Test
    void fullWidthUppercaseConvertedToHalfWidthLower() {
        assertThat(KeywordNormalizer.normalize("ＡＢＣ")).isEqualTo("abc");
    }

    // 6-A-5
    @Test
    void fullWidthLowercaseConvertedToHalfWidthLower() {
        assertThat(KeywordNormalizer.normalize("ａｂｃ")).isEqualTo("abc");
    }

    // 6-A-6
    @Test
    void halfWidthKatakanaConvertedToFullWidth() {
        assertThat(KeywordNormalizer.normalize("ｱｲｳ")).isEqualTo("アイウ");
    }

    // 6-A-7
    @Test
    void halfWidthKatakanaWithVoicingConvertedToFullWidth() {
        assertThat(KeywordNormalizer.normalize("ｶﾞｷﾞ")).isEqualTo("ガギ");
    }

    // 6-A-8
    @Test
    void halfWidthKatakanaWithSemiVoicingConvertedToFullWidth() {
        assertThat(KeywordNormalizer.normalize("ﾊﾟﾋﾟ")).isEqualTo("パピ");
    }

    // 6-A-9
    @Test
    void halfWidthUppercaseConvertedToLower() {
        assertThat(KeywordNormalizer.normalize("DESK")).isEqualTo("desk");
    }

    // 6-A-10
    @Test
    void mixedInputConverted() {
        assertThat(KeywordNormalizer.normalize("ＡＢＣ１２３ｱｲｳ")).isEqualTo("abc123アイウ");
    }

    // 6-A-11
    @Test
    void hiraganaAndKanjiAreNotConverted() {
        assertThat(KeywordNormalizer.normalize("デスクあいう机")).isEqualTo("デスクあいう机");
    }

    // 6-A-12
    @Test
    void alreadyHalfWidthLowercaseIsUnchanged() {
        assertThat(KeywordNormalizer.normalize("desk123")).isEqualTo("desk123");
    }
}
