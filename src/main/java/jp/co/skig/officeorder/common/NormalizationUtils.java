package jp.co.skig.officeorder.common;

/**
 * 検索キーワードの全角半角正規化ユーティリティ。
 *
 * <ul>
 *   <li>全角英字（Ａ-Ｚ, ａ-ｚ）→ 半角（A-Z, a-z）</li>
 *   <li>全角数字（０-９）→ 半角（0-9）</li>
 *   <li>半角カタカナ → 全角カタカナ（濁音・半濁音の結合文字を含む）</li>
 * </ul>
 */
public final class NormalizationUtils {

    /** 半角カタカナ清音文字（濁点・半濁点の基底としても使用）。 */
    private static final String HALF_KATAKANA =
            "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ";

    /** 全角カタカナ清音文字（HALF_KATAKANAと1対1対応）。 */
    private static final String FULL_KATAKANA =
            "ヲァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン゛゜";

    /** 濁点が付く半角カタカナ基底文字。 */
    private static final String DAKUTEN_BASES = "ｳｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾊﾋﾌﾍﾎ";

    /** 濁点付き全角カタカナ（DAKUTEN_BASESと1対1対応）。 */
    private static final String DAKUTEN_COMBINED = "ヴガギグゲゴザジズゼゾダヂヅデドバビブベボ";

    /** 半濁点が付く半角カタカナ基底文字。 */
    private static final String HANDAKUTEN_BASES = "ﾊﾋﾌﾍﾎ";

    /** 半濁点付き全角カタカナ（HANDAKUTEN_BASESと1対1対応）。 */
    private static final String HANDAKUTEN_COMBINED = "パピプペポ";

    private NormalizationUtils() {
    }

    /**
     * 全角英数字を半角に、半角カタカナを全角に正規化する。
     *
     * @param input 入力文字列
     * @return 正規化済み文字列。nullの場合はnullを返す
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            // 全角英大文字 Ａ(FF21)〜Ｚ(FF3A) → A(0041)〜Z(005A)
            if (ch >= '\uFF21' && ch <= '\uFF3A') {
                sb.append((char) (ch - 0xFF21 + 'A'));
                continue;
            }
            // 全角英小文字 ａ(FF41)〜ｚ(FF5A) → a(0061)〜z(007A)
            if (ch >= '\uFF41' && ch <= '\uFF5A') {
                sb.append((char) (ch - 0xFF41 + 'a'));
                continue;
            }
            // 全角数字 ０(FF10)〜９(FF19) → 0(0030)〜9(0039)
            if (ch >= '\uFF10' && ch <= '\uFF19') {
                sb.append((char) (ch - 0xFF10 + '0'));
                continue;
            }

            // 半角カタカナの濁音・半濁音結合処理
            if (i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == '\uFF9E') { // 濁点 ﾞ
                    int dakuIdx = DAKUTEN_BASES.indexOf(ch);
                    if (dakuIdx >= 0) {
                        sb.append(DAKUTEN_COMBINED.charAt(dakuIdx));
                        i++; // skip combining mark
                        continue;
                    }
                } else if (next == '\uFF9F') { // 半濁点 ﾟ
                    int hanIdx = HANDAKUTEN_BASES.indexOf(ch);
                    if (hanIdx >= 0) {
                        sb.append(HANDAKUTEN_COMBINED.charAt(hanIdx));
                        i++; // skip combining mark
                        continue;
                    }
                }
            }

            // 半角カタカナ清音 → 全角
            int halfIdx = HALF_KATAKANA.indexOf(ch);
            if (halfIdx >= 0) {
                sb.append(FULL_KATAKANA.charAt(halfIdx));
                continue;
            }

            // その他の文字はそのまま
            sb.append(ch);
        }
        return sb.toString();
    }
}
