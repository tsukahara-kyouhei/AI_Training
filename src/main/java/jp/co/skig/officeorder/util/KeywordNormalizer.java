package jp.co.skig.officeorder.util;

import java.util.Locale;

/**
 * キーワード検索用の文字列正規化ユーティリティ。
 *
 * <p>全角→半角・半角カタカナ→全角カタカナ・大文字→小文字の変換を行い、
 * DB データとの照合に適した形へ整える。
 */
public final class KeywordNormalizer {

    /** 半角カタカナ変換元文字列（U+FF66〜U+FF9F）。 */
    private static final String HALF_KANA =
            "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ";

    /** 全角カタカナ変換先文字列（HALF_KANA と同順）。 */
    private static final String FULL_KANA =
            "ヲァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン゛゜";

    private KeywordNormalizer() {
    }

    /**
     * 入力文字列に正規化を適用し返す。
     *
     * <p>変換順序：
     * <ol>
     *   <li>null / 空文字はそのまま返す</li>
     *   <li>全角 ASCII 数字 (U+FF10〜U+FF19) → 半角数字</li>
     *   <li>全角 ASCII 大文字 (U+FF21〜U+FF3A) → 半角小文字</li>
     *   <li>全角 ASCII 小文字 (U+FF41〜U+FF5A) → 半角小文字</li>
     *   <li>半角カタカナ濁音/半濁音（2文字形式）→ 全角カタカナ1文字</li>
     *   <li>残り半角カタカナ (U+FF66〜U+FF9F) → 全角カタカナ</li>
     *   <li>半角英字大文字 (A-Z) → 小文字</li>
     * </ol>
     *
     * @param input 入力文字列
     * @return 正規化後の文字列。null / 空文字はそのまま返す。
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder(input.length());
        int len = input.length();
        int i = 0;

        while (i < len) {
            char c = input.charAt(i);

            // 全角 ASCII 数字 (U+FF10〜U+FF19) → 半角数字
            if (c >= '\uFF10' && c <= '\uFF19') {
                sb.append((char) (c - '\uFF10' + '0'));
                i++;
                continue;
            }

            // 全角 ASCII 大文字 (U+FF21〜U+FF3A) → 半角小文字
            if (c >= '\uFF21' && c <= '\uFF3A') {
                sb.append((char) (c - '\uFF21' + 'a'));
                i++;
                continue;
            }

            // 全角 ASCII 小文字 (U+FF41〜U+FF5A) → 半角小文字
            if (c >= '\uFF41' && c <= '\uFF5A') {
                sb.append((char) (c - '\uFF41' + 'a'));
                i++;
                continue;
            }

            // 半角カタカナ濁音/半濁音（2文字形式）→ 全角カタカナ1文字
            if (i + 1 < len) {
                char next = input.charAt(i + 1);
                if (next == '\uFF9E') {
                    char voiced = toVoiced(c);
                    if (voiced != 0) {
                        sb.append(voiced);
                        i += 2;
                        continue;
                    }
                } else if (next == '\uFF9F') {
                    char semiVoiced = toSemiVoiced(c);
                    if (semiVoiced != 0) {
                        sb.append(semiVoiced);
                        i += 2;
                        continue;
                    }
                }
            }

            // 残り半角カタカナ (U+FF66〜U+FF9F) → 全角カタカナ
            int hkIdx = HALF_KANA.indexOf(c);
            if (hkIdx >= 0) {
                sb.append(FULL_KANA.charAt(hkIdx));
                i++;
                continue;
            }

            sb.append(c);
            i++;
        }

        // 半角英字大文字 (A-Z) → 小文字
        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    private static char toVoiced(char c) {
        return switch (c) {
            case 'ｶ' -> 'ガ';
            case 'ｷ' -> 'ギ';
            case 'ｸ' -> 'グ';
            case 'ｹ' -> 'ゲ';
            case 'ｺ' -> 'ゴ';
            case 'ｻ' -> 'ザ';
            case 'ｼ' -> 'ジ';
            case 'ｽ' -> 'ズ';
            case 'ｾ' -> 'ゼ';
            case 'ｿ' -> 'ゾ';
            case 'ﾀ' -> 'ダ';
            case 'ﾁ' -> 'ヂ';
            case 'ﾂ' -> 'ヅ';
            case 'ﾃ' -> 'デ';
            case 'ﾄ' -> 'ド';
            case 'ﾊ' -> 'バ';
            case 'ﾋ' -> 'ビ';
            case 'ﾌ' -> 'ブ';
            case 'ﾍ' -> 'ベ';
            case 'ﾎ' -> 'ボ';
            case 'ｳ' -> 'ヴ';
            default -> (char) 0;
        };
    }

    private static char toSemiVoiced(char c) {
        return switch (c) {
            case 'ﾊ' -> 'パ';
            case 'ﾋ' -> 'ピ';
            case 'ﾌ' -> 'プ';
            case 'ﾍ' -> 'ペ';
            case 'ﾎ' -> 'ポ';
            default -> (char) 0;
        };
    }
}
