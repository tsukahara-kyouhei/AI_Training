package jp.co.skig.officeorder.repository;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.mapper.row.KeywordSearchParam;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link ProductRepository} の {@code buildSearchParams()} におけるキーワード分割ロジックのテスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductRepositoryKeywordBuildTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AppTimeProvider appTimeProvider;

    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ProductRepository(productMapper, appTimeProvider);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(OffsetDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private List<KeywordSearchParam> extractKeywords(String keyword) throws Exception {
        ProductSearchCondition condition = new ProductSearchCondition(
                null, keyword, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15,
                null, List.of()
        );
        Method method = ProductRepository.class.getDeclaredMethod("buildSearchParams", ProductSearchCondition.class);
        method.setAccessible(true);
        Map<String, Object> params = (Map<String, Object>) method.invoke(repository, condition);
        return (List<KeywordSearchParam>) params.get("keywords");
    }

    // ---- BK-01: 1キーワード ----
    @Test
    void bk01_singleKeyword() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("ナチュラル");
        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).keywordLike()).isEqualTo("%ナチュラル%");
        assertThat(keywords.get(0).codeLike()).isEqualTo("ナチュラル%");
    }

    // ---- BK-02: 半角スペース区切り2キーワード ----
    @Test
    void bk02_twoKeywords_halfSpaceSeparator() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("ナチュラル デスク");
        assertThat(keywords).hasSize(2);
        assertThat(keywords.get(0).keywordLike()).isEqualTo("%ナチュラル%");
        assertThat(keywords.get(1).keywordLike()).isEqualTo("%デスク%");
    }

    // ---- BK-03: 全角スペース区切り ----
    @Test
    void bk03_twoKeywords_fullSpaceSeparator() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("ナチュラル　デスク");
        assertThat(keywords).hasSize(2);
        assertThat(keywords.get(0).keywordLike()).isEqualTo("%ナチュラル%");
        assertThat(keywords.get(1).keywordLike()).isEqualTo("%デスク%");
    }

    // ---- BK-04: 前方一致形式の確認 ----
    @Test
    void bk04_codeLikeFormat() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("DSK");
        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).codeLike()).isEqualTo("DSK%");
    }

    // ---- BK-05: null → 空リスト ----
    @Test
    void bk05_nullKeyword_emptyList() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords(null);
        assertThat(keywords).isEmpty();
    }

    // ---- BK-06: 空文字 → 空リスト ----
    @Test
    void bk06_emptyKeyword_emptyList() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("");
        assertThat(keywords).isEmpty();
    }

    // ---- BK-07: 空白のみ → 空リスト ----
    @Test
    void bk07_blankKeyword_emptyList() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("   ");
        assertThat(keywords).isEmpty();
    }

    // ---- BK-08: 連続スペースは1区切りとして扱う ----
    @Test
    void bk08_consecutiveSpaces_twoKeywords() throws Exception {
        List<KeywordSearchParam> keywords = extractKeywords("デスク  チェア");
        assertThat(keywords).hasSize(2);
        assertThat(keywords.get(0).keywordLike()).isEqualTo("%デスク%");
        assertThat(keywords.get(1).keywordLike()).isEqualTo("%チェア%");
    }
}
