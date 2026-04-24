package jp.co.skig.officeorder.service.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SearchKeywordNormalizer} の単体テスト。
 */
class SearchKeywordNormalizerTest {

    private final SearchKeywordNormalizer normalizer = new SearchKeywordNormalizer();

    // ---- normalize ----

    @Test
    void normalize_null入力はnullを返す() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void normalize_空文字はnullを返す() {
        assertThat(normalizer.normalize("")).isNull();
    }

    @Test
    void normalize_空白のみはnullを返す() {
        assertThat(normalizer.normalize("   ")).isNull();
    }

    @Test
    void normalize_ASCII小文字はそのまま返す() {
        assertThat(normalizer.normalize("desk")).isEqualTo("desk");
    }

    @Test
    void normalize_ASCII大文字は小文字に変換する() {
        assertThat(normalizer.normalize("DESK")).isEqualTo("desk");
    }

    @Test
    void normalize_全角大文字は半角小文字に変換する() {
        // Ａ = U+FF21
        assertThat(normalizer.normalize("\uFF21")).isEqualTo("a");
    }

    @Test
    void normalize_全角英字列は半角小文字に変換する() {
        // Ｄｅｓｋ
        assertThat(normalizer.normalize("\uFF24\uFF45\uFF53\uFF4B")).isEqualTo("desk");
    }

    @Test
    void normalize_全角数字は半角数字に変換する() {
        // ０１２３
        assertThat(normalizer.normalize("\uFF10\uFF11\uFF12\uFF13")).isEqualTo("0123");
    }

    @Test
    void normalize_全角半角混在は正規化する() {
        // Ｄesk (全角D + 半角esk)
        assertThat(normalizer.normalize("\uFF24esk")).isEqualTo("desk");
    }

    @Test
    void normalize_前後の空白は除去する() {
        assertThat(normalizer.normalize("  desk  ")).isEqualTo("desk");
    }

    @Test
    void normalize_カタカナはそのまま返す() {
        assertThat(normalizer.normalize("デスク")).isEqualTo("デスク");
    }

    // ---- toLikePattern ----

    @Test
    void toLikePattern_nullはnullを返す() {
        assertThat(normalizer.toLikePattern(null)).isNull();
    }

    @Test
    void toLikePattern_キーワードを前後パーセントで囲む() {
        assertThat(normalizer.toLikePattern("desk")).isEqualTo("%desk%");
    }

    // ---- toPrefixPattern ----

    @Test
    void toPrefixPattern_nullはnullを返す() {
        assertThat(normalizer.toPrefixPattern(null)).isNull();
    }

    @Test
    void toPrefixPattern_キーワードの末尾にパーセントを付与する() {
        assertThat(normalizer.toPrefixPattern("ABC-001")).isEqualTo("ABC-001%");
    }
}
