package jp.co.skig.officeorder.repository;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductRepository の normalizeKeyword / toKeywordLike / toKeywordPrefix に関するテスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductRepositoryKeywordTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AppTimeProvider appTimeProvider;

    private ProductRepository sut;

    @BeforeEach
    void setUp() {
        sut = new ProductRepository(productMapper, appTimeProvider);
    }

    // ─── normalizeKeyword ────────────────────────────────────────────────

    @Test
    void normalizeKeyword_nullを渡したとき_nullを返すこと() {
        assertThat(sut.normalizeKeyword(null)).isNull();
    }

    @Test
    void normalizeKeyword_空文字を渡したとき_nullを返すこと() {
        assertThat(sut.normalizeKeyword("")).isNull();
    }

    @Test
    void normalizeKeyword_空白のみを渡したとき_nullを返すこと() {
        assertThat(sut.normalizeKeyword("   ")).isNull();
    }

    @Test
    void normalizeKeyword_半角カタカナを全角カタカナに変換すること() {
        // ｱｰﾑﾁｪｱ → アームチェア
        assertThat(sut.normalizeKeyword("ｱｰﾑﾁｪｱ")).isEqualTo("アームチェア");
    }

    @Test
    void normalizeKeyword_全角英数を半角英数に変換すること() {
        // Ａ１ → A1
        assertThat(sut.normalizeKeyword("Ａ１")).isEqualTo("A1");
    }

    @Test
    void normalizeKeyword_前後スペースをトリムすること() {
        assertThat(sut.normalizeKeyword("  デスク  ")).isEqualTo("デスク");
    }

    @Test
    void normalizeKeyword_通常の全角文字はそのまま返すこと() {
        assertThat(sut.normalizeKeyword("ナチュラル")).isEqualTo("ナチュラル");
    }

    @Test
    void normalizeKeyword_混在文字列を正規化すること() {
        // 半角カナ + 全角英字が混在
        assertThat(sut.normalizeKeyword("ｵﾌｨｽＡ")).isEqualTo("オフィスA");
    }
}
