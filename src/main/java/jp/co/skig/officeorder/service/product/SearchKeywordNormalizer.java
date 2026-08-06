package jp.co.skig.officeorder.service.product;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 商品検索向けのキーワード正規化を担当するユーティリティ。
 */
public class SearchKeywordNormalizer {

    /**
     * 入力キーワードを検索比較用に正規化する。
     *
     * @param rawKeyword 入力キーワード
     * @return 正規化済みキーワード
     */
    public String normalize(String rawKeyword) {
        if (rawKeyword == null) {
            return null;
        }
        String trimmed = rawKeyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFKC);
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * SQL の LIKE 句用に、検索文字列の前後空白を除き、比較用に正規化した文字列を返す。
     *
     * @param rawKeyword 入力キーワード
     * @return LIKE 句に渡すための正規化済み文字列
     */
    public String normalizeForLike(String rawKeyword) {
        String normalized = normalize(rawKeyword);
        if (normalized == null) {
            return null;
        }
        return "%" + normalized + "%";
    }
}
