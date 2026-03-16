package jp.co.skig.officeorder.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchKeywordNormalizerTest {

    @Test
    void nullを渡すとnullを返す() {
        assertThat(SearchKeywordNormalizer.normalize(null)).isNull();
    }

    @Test
    void 空文字を渡すとnullを返す() {
        assertThat(SearchKeywordNormalizer.normalize("")).isNull();
    }

    @Test
    void 全角数字を半角に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("０１２３４５６７８９")).isEqualTo("0123456789");
    }

    @Test
    void 全角英大文字を半角英小文字に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ＡＢＣ")).isEqualTo("abc");
    }

    @Test
    void 全角英小文字を半角英小文字に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ａｂｃ")).isEqualTo("abc");
    }

    @Test
    void 半角英大文字を半角英小文字に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ABC")).isEqualTo("abc");
    }

    @Test
    void 全角カタカナ清音を半角カタカナに変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ナチュラル")).isEqualTo("ﾅﾁｭﾗﾙ");
    }

    @Test
    void 全角カタカナ濁音を半角カタカナ2文字に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ガギグゲゴ")).isEqualTo("ｶﾞｷﾞｸﾞｹﾞｺﾞ");
    }

    @Test
    void 全角カタカナ半濁音を半角カタカナ2文字に変換する() {
        assertThat(SearchKeywordNormalizer.normalize("パピプペポ")).isEqualTo("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ");
    }

    @Test
    void 商品コード形式の全角入力を変換する() {
        assertThat(SearchKeywordNormalizer.normalize("Ｐ０００１-Ｃ０１")).isEqualTo("p0001-c01");
    }

    @Test
    void ひらがなは変換しない() {
        assertThat(SearchKeywordNormalizer.normalize("なちゅらる")).isEqualTo("なちゅらる");
    }

    @Test
    void 混合入力を正しく変換する() {
        assertThat(SearchKeywordNormalizer.normalize("ＡＢＣ１２３ナチュラル")).isEqualTo("abc123ﾅﾁｭﾗﾙ");
    }

    @Test
    void 半角カタカナはそのまま維持される() {
        assertThat(SearchKeywordNormalizer.normalize("ﾅﾁｭﾗﾙ")).isEqualTo("ﾅﾁｭﾗﾙ");
    }
}
