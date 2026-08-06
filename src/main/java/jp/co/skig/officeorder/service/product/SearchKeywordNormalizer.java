package jp.co.skig.officeorder.service.product;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 検索キーワードの正規化とトークン化を行うユーティリティ。
 */
public final class SearchKeywordNormalizer {

    private SearchKeywordNormalizer() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace('　', ' ');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static List<String> tokenize(String keyword) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return List.of();
        }
        String[] tokens = normalized.split(" ");
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static String toLikePattern(String token) {
        return "%" + escapeLikeToken(token) + "%";
    }

    public static String toPrefixPattern(String token) {
        return escapeLikeToken(token) + "%";
    }

    private static String escapeLikeToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        return token.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
