package jp.co.skig.officeorder.common;

/**
 * 検索キーワードの全角・半角正規化ユーティリティ。
 *
 * <ul>
 *   <li>全角英数字 → 半角英数字</li>
 *   <li>半角カタカナ → 全角カタカナ（濁点・半濁点の合成を含む）</li>
 * </ul>
 *
 * <p>ひらがな・漢字・全角記号は変換対象外とし、入力のまま通す。
 */
public final class KeywordNormalizer {

    /** 半角カタカナ → 全角カタカナ 変換テーブル（U+FF66〜U+FF9D）。 */
    private static final char[] HANKAKU_TO_ZENKAKU = {
        'ヲ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', // FF66-FF6F
        'ー', 'ア', 'イ', 'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', // FF70-FF79
        'コ', 'サ', 'シ', 'ス', 'セ', 'ソ', 'タ', 'チ', 'ツ', 'テ', // FF7A-FF83
        'ト', 'ナ', 'ニ', 'ヌ', 'ネ', 'ノ', 'ハ', 'ヒ', 'フ', 'ヘ', // FF84-FF8D
        'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ', 'ユ', 'ヨ', 'ラ', // FF8E-FF97
        'リ', 'ル', 'レ', 'ロ', 'ワ', 'ン', 'ヴ', // FF98-FF9E (FF9E = 濁点単独)
    };

    /** 濁点を付与できる全角カタカナの対応表（濁点なし → 濁点あり）。 */
    private static final String DAKUTEN_BASE   = "カキクケコサシスセソタチツテトハヒフヘホウ";
    private static final String DAKUTEN_VOICED = "ガギグゲゴザジズゼゾダヂヅデドバビブベボヴ";

    /** 半濁点を付与できる全角カタカナの対応表（半濁点なし → 半濁点あり）。 */
    private static final String HANDAKUTEN_BASE   = "ハヒフヘホ";
    private static final String HANDAKUTEN_VOICED = "パピプペポ";

    private KeywordNormalizer() {}

    /**
     * 検索キーワードを正規化する。
     *
     * @param input 入力文字列（null 許容）
     * @return 正規化済み文字列。null または空入力の場合は空文字列を返す
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(input.length());
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);

            // 半角カタカナ処理（U+FF66〜FF9F）
            if (c >= '\uFF66' && c <= '\uFF9F') {
                char next = (i + 1 < length) ? input.charAt(i + 1) : 0;

                if (next == '\uFF9E') {
                    // 濁点合成
                    char zenkaku = toZenkakuKatakana(c);
                    int idx = DAKUTEN_BASE.indexOf(zenkaku);
                    if (idx >= 0) {
                        sb.append(DAKUTEN_VOICED.charAt(idx));
                    } else {
                        sb.append(zenkaku);
                        sb.append('゛');
                    }
                    i++; // 濁点をスキップ
                } else if (next == '\uFF9F') {
                    // 半濁点合成
                    char zenkaku = toZenkakuKatakana(c);
                    int idx = HANDAKUTEN_BASE.indexOf(zenkaku);
                    if (idx >= 0) {
                        sb.append(HANDAKUTEN_VOICED.charAt(idx));
                    } else {
                        sb.append(zenkaku);
                        sb.append('゜');
                    }
                    i++; // 半濁点をスキップ
                } else {
                    sb.append(toZenkakuKatakana(c));
                }
            } else if ((c >= '\uFF21' && c <= '\uFF3A') // 全角英大文字
                    || (c >= '\uFF41' && c <= '\uFF5A') // 全角英小文字
                    || (c >= '\uFF10' && c <= '\uFF19')) { // 全角数字
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 半角カタカナ1文字を全角カタカナに変換する。
     * 濁点・半濁点単独文字（FF9E/FF9F）はそのまま全角に変換する。
     */
    private static char toZenkakuKatakana(char c) {
        int idx = c - '\uFF66';
        if (idx >= 0 && idx < HANKAKU_TO_ZENKAKU.length) {
            return HANKAKU_TO_ZENKAKU[idx];
        }
        // FF9F (半濁点単独)
        if (c == '\uFF9F') {
            return '゜';
        }
        return c;
    }
}
