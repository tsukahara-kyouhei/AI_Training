package jp.co.skig.officeorder.service.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchKeywordNormalizerTest {

    private final SearchKeywordNormalizer normalizer = new SearchKeywordNormalizer();

    @Test
    void trimsAndLowercasesKeyword() {
        assertEquals("desk", normalizer.normalize("  DESK  "));
    }

    @Test
    void normalizesFullWidthCharacters() {
        assertEquals("abc123", normalizer.normalize("ＡＢＣ１２３"));
    }

    @Test
    void returnsNullForBlankKeyword() {
        assertNull(normalizer.normalize("   "));
    }
}
