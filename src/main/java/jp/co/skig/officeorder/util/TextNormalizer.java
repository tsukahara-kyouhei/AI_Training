package jp.co.skig.officeorder.util;

/**
 * 全角・半角テキスト正規化ユーティリティ。
 *
 * <p>半角数字・英字・カタカナを全角へ変換する。
 * ひらがな・漢字・すでに全角の文字は変換しない。
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /**
     * 入力文字列の半角数字・英字・カタカナを全角へ変換する。
     *
     * <p>半角カタカナの濁点（ﾞ）・半濁点（ﾟ）は直前の文字と合成して変換する。
     * {@code null} または空文字の場合は入力値をそのまま返す。
     *
     * @param input 変換対象文字列
     * @return 全角変換済み文字列（null の場合は null）
     */
    public static String toFullWidth(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c >= '0' && c <= '9') {
                // 半角数字 → 全角数字（コードポイント差分 +0xFEE0）
                sb.append((char) (c + 0xFEE0));
            } else if (c >= 'A' && c <= 'Z') {
                // 半角英大文字 → 全角英大文字
                sb.append((char) (c + 0xFEE0));
            } else if (c >= 'a' && c <= 'z') {
                // 半角英小文字 → 全角英小文字
                sb.append((char) (c + 0xFEE0));
            } else if (c >= '\uFF65' && c <= '\uFF9F') {
                // 半角カタカナ（U+FF65–U+FF9F）
                char next = (i + 1 < len) ? input.charAt(i + 1) : 0;
                if (next == '\uFF9E') {
                    char voiced = toVoiced(c);
                    if (voiced != 0) {
                        sb.append(voiced);
                        i++; // 濁点を消費
                    } else {
                        sb.append(toFullWidthKatakana(c));
                    }
                } else if (next == '\uFF9F') {
                    char semiVoiced = toSemiVoiced(c);
                    if (semiVoiced != 0) {
                        sb.append(semiVoiced);
                        i++; // 半濁点を消費
                    } else {
                        sb.append(toFullWidthKatakana(c));
                    }
                } else {
                    sb.append(toFullWidthKatakana(c));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 半角カタカナに対応する全角カタカナ（濁音）を返す。
     * 濁音化できない文字の場合は 0 を返す。
     */
    private static char toVoiced(char c) {
        switch (c) {
            case '\uFF76': return '\u30AC'; // ｶ → ガ
            case '\uFF77': return '\u30AE'; // ｷ → ギ
            case '\uFF78': return '\u30B0'; // ｸ → グ
            case '\uFF79': return '\u30B2'; // ｹ → ゲ
            case '\uFF7A': return '\u30B4'; // ｺ → ゴ
            case '\uFF7B': return '\u30B6'; // ｻ → ザ
            case '\uFF7C': return '\u30B8'; // ｼ → ジ
            case '\uFF7D': return '\u30BA'; // ｽ → ズ
            case '\uFF7E': return '\u30BC'; // ｾ → ゼ
            case '\uFF7F': return '\u30BE'; // ｿ → ゾ
            case '\uFF80': return '\u30C0'; // ﾀ → ダ
            case '\uFF81': return '\u30C2'; // ﾁ → ヂ
            case '\uFF82': return '\u30C5'; // ﾂ → ヅ
            case '\uFF83': return '\u30C7'; // ﾃ → デ
            case '\uFF84': return '\u30C9'; // ﾄ → ド
            case '\uFF8A': return '\u30D0'; // ﾊ → バ
            case '\uFF8B': return '\u30D3'; // ﾋ → ビ
            case '\uFF8C': return '\u30D6'; // ﾌ → ブ
            case '\uFF8D': return '\u30D9'; // ﾍ → ベ
            case '\uFF8E': return '\u30DC'; // ﾎ → ボ
            case '\uFF73': return '\u30F4'; // ｳ → ヴ
            default: return 0;
        }
    }

    /**
     * 半角カタカナに対応する全角カタカナ（半濁音）を返す。
     * 半濁音化できない文字の場合は 0 を返す。
     */
    private static char toSemiVoiced(char c) {
        switch (c) {
            case '\uFF8A': return '\u30D1'; // ﾊ → パ
            case '\uFF8B': return '\u30D4'; // ﾋ → ピ
            case '\uFF8C': return '\u30D7'; // ﾌ → プ
            case '\uFF8D': return '\u30DA'; // ﾍ → ペ
            case '\uFF8E': return '\u30DD'; // ﾎ → ポ
            default: return 0;
        }
    }

    /**
     * 半角カタカナ1文字を全角カタカナへ変換する（濁点・半濁点合成なし）。
     */
    private static char toFullWidthKatakana(char c) {
        switch (c) {
            case '\uFF65': return '\u30FB'; // ･ → ・
            case '\uFF66': return '\u30F2'; // ｦ → ヲ
            case '\uFF67': return '\u30A1'; // ｧ → ァ
            case '\uFF68': return '\u30A3'; // ｨ → ィ
            case '\uFF69': return '\u30A5'; // ｩ → ゥ
            case '\uFF6A': return '\u30A7'; // ｪ → ェ
            case '\uFF6B': return '\u30A9'; // ｫ → ォ
            case '\uFF6C': return '\u30E3'; // ｬ → ャ
            case '\uFF6D': return '\u30E5'; // ｭ → ュ
            case '\uFF6E': return '\u30E7'; // ｮ → ョ
            case '\uFF6F': return '\u30C3'; // ｯ → ッ
            case '\uFF70': return '\u30FC'; // ｰ → ー
            case '\uFF71': return '\u30A2'; // ｱ → ア
            case '\uFF72': return '\u30A4'; // ｲ → イ
            case '\uFF73': return '\u30A6'; // ｳ → ウ
            case '\uFF74': return '\u30A8'; // ｴ → エ
            case '\uFF75': return '\u30AA'; // ｵ → オ
            case '\uFF76': return '\u30AB'; // ｶ → カ
            case '\uFF77': return '\u30AD'; // ｷ → キ
            case '\uFF78': return '\u30AF'; // ｸ → ク
            case '\uFF79': return '\u30B1'; // ｹ → ケ
            case '\uFF7A': return '\u30B3'; // ｺ → コ
            case '\uFF7B': return '\u30B5'; // ｻ → サ
            case '\uFF7C': return '\u30B7'; // ｼ → シ
            case '\uFF7D': return '\u30B9'; // ｽ → ス
            case '\uFF7E': return '\u30BB'; // ｾ → セ
            case '\uFF7F': return '\u30BD'; // ｿ → ソ
            case '\uFF80': return '\u30BF'; // ﾀ → タ
            case '\uFF81': return '\u30C1'; // ﾁ → チ
            case '\uFF82': return '\u30C4'; // ﾂ → ツ
            case '\uFF83': return '\u30C6'; // ﾃ → テ
            case '\uFF84': return '\u30C8'; // ﾄ → ト
            case '\uFF85': return '\u30CA'; // ﾅ → ナ
            case '\uFF86': return '\u30CB'; // ﾆ → ニ
            case '\uFF87': return '\u30CC'; // ﾇ → ヌ
            case '\uFF88': return '\u30CD'; // ﾈ → ネ
            case '\uFF89': return '\u30CE'; // ﾉ → ノ
            case '\uFF8A': return '\u30CF'; // ﾊ → ハ
            case '\uFF8B': return '\u30D2'; // ﾋ → ヒ
            case '\uFF8C': return '\u30D5'; // ﾌ → フ
            case '\uFF8D': return '\u30D8'; // ﾍ → ヘ
            case '\uFF8E': return '\u30DB'; // ﾎ → ホ
            case '\uFF8F': return '\u30DE'; // ﾏ → マ
            case '\uFF90': return '\u30DF'; // ﾐ → ミ
            case '\uFF91': return '\u30E0'; // ﾑ → ム
            case '\uFF92': return '\u30E1'; // ﾒ → メ
            case '\uFF93': return '\u30E2'; // ﾓ → モ
            case '\uFF94': return '\u30E4'; // ﾔ → ヤ
            case '\uFF95': return '\u30E6'; // ﾕ → ユ
            case '\uFF96': return '\u30E8'; // ﾖ → ヨ
            case '\uFF97': return '\u30E9'; // ﾗ → ラ
            case '\uFF98': return '\u30EA'; // ﾘ → リ
            case '\uFF99': return '\u30EB'; // ﾙ → ル
            case '\uFF9A': return '\u30EC'; // ﾚ → レ
            case '\uFF9B': return '\u30ED'; // ﾛ → ロ
            case '\uFF9C': return '\u30EF'; // ﾜ → ワ
            case '\uFF9D': return '\u30F3'; // ﾝ → ン
            case '\uFF9E': return '\u309B'; // ﾞ → ゛（単独濁点）
            case '\uFF9F': return '\u309C'; // ﾟ → ゜（単独半濁点）
            default: return c;
        }
    }
}
