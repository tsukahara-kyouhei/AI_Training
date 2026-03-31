package jp.co.skig.officeorder.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TextNormalizer} の単体テスト。
 */
class TextNormalizerTest {

    // -------------------------------------------------------------------
    // null / empty
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_nullInput_returnsNull() {
        assertThat(TextNormalizer.toFullWidth(null)).isNull();
    }

    @Test
    void toFullWidth_emptyString_returnsEmpty() {
        assertThat(TextNormalizer.toFullWidth("")).isEmpty();
    }

    // -------------------------------------------------------------------
    // 半角数字 → 全角数字
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_halfWidthDigits_convertedToFullWidth() {
        assertThat(TextNormalizer.toFullWidth("0123456789"))
                .isEqualTo("０１２３４５６７８９");
    }

    // -------------------------------------------------------------------
    // 半角英字 → 全角英字
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_halfWidthUppercase_convertedToFullWidth() {
        assertThat(TextNormalizer.toFullWidth("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                .isEqualTo("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ");
    }

    @Test
    void toFullWidth_halfWidthLowercase_convertedToFullWidth() {
        assertThat(TextNormalizer.toFullWidth("abcdefghijklmnopqrstuvwxyz"))
                .isEqualTo("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ");
    }

    // -------------------------------------------------------------------
    // 半角カタカナ → 全角カタカナ（基本）
    // -------------------------------------------------------------------

    @ParameterizedTest(name = "half={0} -> full={1}")
    @CsvSource({
            "ｱ,ア", "ｲ,イ", "ｳ,ウ", "ｴ,エ", "ｵ,オ",
            "ｶ,カ", "ｷ,キ", "ｸ,ク", "ｹ,ケ", "ｺ,コ",
            "ｻ,サ", "ｼ,シ", "ｽ,ス", "ｾ,セ", "ｿ,ソ",
            "ﾀ,タ", "ﾁ,チ", "ﾂ,ツ", "ﾃ,テ", "ﾄ,ト",
            "ﾅ,ナ", "ﾆ,ニ", "ﾇ,ヌ", "ﾈ,ネ", "ﾉ,ノ",
            "ﾏ,マ", "ﾐ,ミ", "ﾑ,ム", "ﾒ,メ", "ﾓ,モ",
            "ﾔ,ヤ", "ﾕ,ユ", "ﾖ,ヨ",
            "ﾗ,ラ", "ﾘ,リ", "ﾙ,ル", "ﾚ,レ", "ﾛ,ロ",
            "ﾜ,ワ", "ｦ,ヲ", "ﾝ,ン",
            "ｯ,ッ", "ｬ,ャ", "ｭ,ュ", "ｮ,ョ",
            "ｧ,ァ", "ｨ,ィ", "ｩ,ゥ", "ｪ,ェ", "ｫ,ォ",
            "ｰ,ー"
    })
    void toFullWidth_singleHalfKana_convertsToFullKana(String half, String full) {
        assertThat(TextNormalizer.toFullWidth(half)).isEqualTo(full);
    }

    // -------------------------------------------------------------------
    // 濁点合成
    // -------------------------------------------------------------------

    @ParameterizedTest(name = "half={0} -> voiced={1}")
    @CsvSource({
            "ｶﾞ,ガ", "ｷﾞ,ギ", "ｸﾞ,グ", "ｹﾞ,ゲ", "ｺﾞ,ゴ",
            "ｻﾞ,ザ", "ｼﾞ,ジ", "ｽﾞ,ズ", "ｾﾞ,ゼ", "ｿﾞ,ゾ",
            "ﾀﾞ,ダ", "ﾁﾞ,ヂ", "ﾂﾞ,ヅ", "ﾃﾞ,デ", "ﾄﾞ,ド",
            "ﾊﾞ,バ", "ﾋﾞ,ビ", "ﾌﾞ,ブ", "ﾍﾞ,ベ", "ﾎﾞ,ボ",
            "ｳﾞ,ヴ"
    })
    void toFullWidth_dakuten_composesVoicedKana(String half, String voiced) {
        assertThat(TextNormalizer.toFullWidth(half)).isEqualTo(voiced);
    }

    // -------------------------------------------------------------------
    // 半濁点合成
    // -------------------------------------------------------------------

    @ParameterizedTest(name = "half={0} -> semiVoiced={1}")
    @CsvSource({
            "ﾊﾟ,パ", "ﾋﾟ,ピ", "ﾌﾟ,プ", "ﾍﾟ,ペ", "ﾎﾟ,ポ"
    })
    void toFullWidth_handakuten_composesSemiVoicedKana(String half, String semiVoiced) {
        assertThat(TextNormalizer.toFullWidth(half)).isEqualTo(semiVoiced);
    }

    // -------------------------------------------------------------------
    // 混在文字列
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_mixedHalfKanaAndKanji_convertsOnlyHalfKana() {
        // "ｻﾞｳﾞｨ" (ザヴィ のカタカナ相当) + ひらがな + 漢字は変換しない
        assertThat(TextNormalizer.toFullWidth("ｻﾞ木ｳﾞｨ")).isEqualTo("ザ木ヴィ");
    }

    @Test
    void toFullWidth_mixedAlphanumericAndKana_convertsAll() {
        assertThat(TextNormalizer.toFullWidth("abc123ｱｲｳ"))
                .isEqualTo("ａｂｃ１２３アイウ");
    }

    @Test
    void toFullWidth_fullWidthInputUnchanged() {
        // すでに全角のものは変換しない
        String already = "ＡＢＣＤ１２３ア";
        assertThat(TextNormalizer.toFullWidth(already)).isEqualTo(already);
    }

    @Test
    void toFullWidth_hiraganaUnchanged() {
        String hiragana = "あいうえお";
        assertThat(TextNormalizer.toFullWidth(hiragana)).isEqualTo(hiragana);
    }

    @Test
    void toFullWidth_idempotent() {
        String input = "ｺｰﾋｰ desk100";
        String once = TextNormalizer.toFullWidth(input);
        String twice = TextNormalizer.toFullWidth(once);
        assertThat(twice).isEqualTo(once);
    }

    // -------------------------------------------------------------------
    // 単独の濁点・半濁点（合成対象ではない場合）
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_standaloneDakuten_convertsToFullWidthDakuten() {
        // ﾞ単独は ゛（U+309B）へ
        assertThat(TextNormalizer.toFullWidth("ﾞ")).isEqualTo("゛");
    }

    @Test
    void toFullWidth_standaloneHandakuten_convertsToFullWidthHandakuten() {
        // ﾟ単独は ゜（U+309C）へ
        assertThat(TextNormalizer.toFullWidth("ﾟ")).isEqualTo("゜");
    }

    // -------------------------------------------------------------------
    // 複合キーワード（実使用シナリオ）
    // -------------------------------------------------------------------

    @Test
    void toFullWidth_realWorldKeyword_deskInHalf_convertsCorrectly() {
        // "desk" (半角) → "ｄｅｓｋ" (全角)
        assertThat(TextNormalizer.toFullWidth("desk")).isEqualTo("ｄｅｓｋ");
    }

    @Test
    void toFullWidth_realWorldKeyword_naturalHalfKana_convertsCorrectly() {
        // "ﾅﾁｭﾗﾙ" → "ナチュラル"
        assertThat(TextNormalizer.toFullWidth("ﾅﾁｭﾗﾙ")).isEqualTo("ナチュラル");
    }
}
