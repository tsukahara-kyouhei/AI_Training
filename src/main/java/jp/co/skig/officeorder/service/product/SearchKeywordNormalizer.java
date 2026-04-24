package jp.co.skig.officeorder.service.product;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 検索キーワードの正規化（全角英数字→半角変換、英字小文字化）をカプセル化するコンポーネント。
 */
@Component
public class SearchKeywordNormalizer {

    /** 全角英大文字・全角英小文字・全角数字（変換元、62文字）。 */
    private static final String FULLWIDTH_CHARS =
            "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ" +
            "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ" +
            "０１２３４５６７８９";

    /** 半角英大文字・半角英小文字・半角数字（変換先、62文字）。 */
    private static final String HALFWIDTH_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789";

    /**
     * キーワードを正規化して返す。
     *
     * <p>null または空白のみの入力は null を返す。
     * strip によるトリム → 全角英数字を半角へ変換 → 英字を小文字へ統一 の順に処理する。
     *
     * @param keyword 入力キーワード
     * @return 正規化済みキーワード。null または空白のみの場合は null
     */
    public String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.strip();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            int idx = FULLWIDTH_CHARS.indexOf(c);
            sb.append(idx >= 0 ? HALFWIDTH_CHARS.charAt(idx) : c);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * 正規化済みキーワードを部分一致用 LIKE パターンに変換する。
     *
     * @param normalizedKeyword 正規化済みキーワード
     * @return 部分一致パターン（{@code %keyword%}）。null の場合は null
     */
    public String toLikePattern(String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return null;
        }
        return "%" + normalizedKeyword + "%";
    }

    /**
     * 正規化済みキーワードを前方一致用 LIKE パターンに変換する。
     *
     * @param normalizedKeyword 正規化済みキーワード
     * @return 前方一致パターン（{@code keyword%}）。null の場合は null
     */
    public String toPrefixPattern(String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return null;
        }
        return normalizedKeyword + "%";
    }
}
