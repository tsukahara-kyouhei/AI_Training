package jp.co.skig.officeorder.common;

/**
 * 文字列の全角・半角正規化ユーティリティ。
 *
 * <p>商品検索キーワードの正規化に使用する。
 * インスタンス化不可のユーティリティクラス。
 */
public final class NormalizationUtils {

    private NormalizationUtils() {
    }

    /**
     * 検索キーワードを正規化する。
     *
     * <p>変換順序:
     * <ol>
     *   <li>半角カタカナ（濁点・半濁点合成含む）→ 全角カタカナ</li>
     *   <li>全角数字 → 半角数字</li>
     *   <li>全角英大文字 → 半角英大文字</li>
     *   <li>全角英小文字 → 半角英小文字</li>
     *   <li>全角ハイフン → 半角ハイフン</li>
     *   <li>全角括弧 → 半角括弧</li>
     * </ol>
     *
     * @param text 変換対象文字列
     * @return 正規化済み文字列。null または空文字はそのまま返す。
     */
    public static String normalizeForSearch(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = halfKatakanaToFullKatakana(text);
        result = fullWidthToHalfWidth(result);
        return result;
    }

    /**
     * 半角カタカナを全角カタカナに変換する。
     * 濁点（U+FF9E）・半濁点（U+FF9F）の 2 文字合成を処理する。
     */
    private static String halfKatakanaToFullKatakana(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            // 次の文字が濁点または半濁点なら合成変換
            if (i + 1 < len) {
                char next = text.charAt(i + 1);
                if (next == '\uFF9E') {
                    Character combined = combineWithDakuten(c);
                    if (combined != null) {
                        sb.append(combined);
                        i++;
                        continue;
                    }
                } else if (next == '\uFF9F') {
                    Character combined = combineWithHandakuten(c);
                    if (combined != null) {
                        sb.append(combined);
                        i++;
                        continue;
                    }
                }
            }
            // 単独の半角カタカナ変換
            char full = singleHalfKatakanaToFull(c);
            sb.append(full);
        }
        return sb.toString();
    }

    /**
     * 半角カタカナ + 濁点 の合成変換。
     * 変換できない場合は null を返す。
     */
    private static Character combineWithDakuten(char c) {
        return switch (c) {
            case '\uFF76' -> 'ガ'; // カ + ﾞ
            case '\uFF77' -> 'ギ'; // キ + ﾞ
            case '\uFF78' -> 'グ'; // ク + ﾞ
            case '\uFF79' -> 'ゲ'; // ケ + ﾞ
            case '\uFF7A' -> 'ゴ'; // コ + ﾞ
            case '\uFF7B' -> 'ザ'; // サ + ﾞ
            case '\uFF7C' -> 'ジ'; // シ + ﾞ
            case '\uFF7D' -> 'ズ'; // ス + ﾞ
            case '\uFF7E' -> 'ゼ'; // セ + ﾞ
            case '\uFF7F' -> 'ゾ'; // ソ + ﾞ
            case '\uFF80' -> 'ダ'; // タ + ﾞ
            case '\uFF81' -> 'ヂ'; // チ + ﾞ
            case '\uFF82' -> 'ヅ'; // ツ + ﾞ
            case '\uFF83' -> 'デ'; // テ + ﾞ
            case '\uFF84' -> 'ド'; // ト + ﾞ
            case '\uFF8A' -> 'バ'; // ハ + ﾞ
            case '\uFF8B' -> 'ビ'; // ヒ + ﾞ
            case '\uFF8C' -> 'ブ'; // フ + ﾞ
            case '\uFF8D' -> 'ベ'; // ヘ + ﾞ
            case '\uFF8E' -> 'ボ'; // ホ + ﾞ
            case '\uFF73' -> 'ヴ'; // ウ + ﾞ
            default -> null;
        };
    }

    /**
     * 半角カタカナ + 半濁点 の合成変換。
     * 変換できない場合は null を返す。
     */
    private static Character combineWithHandakuten(char c) {
        return switch (c) {
            case '\uFF8A' -> 'パ'; // ハ + ﾟ
            case '\uFF8B' -> 'ピ'; // ヒ + ﾟ
            case '\uFF8C' -> 'プ'; // フ + ﾟ
            case '\uFF8D' -> 'ペ'; // ヘ + ﾟ
            case '\uFF8E' -> 'ポ'; // ホ + ﾟ
            default -> null;
        };
    }

    /**
     * 単独の半角カタカナ 1 文字を全角カタカナに変換する。
     * 半角カタカナ以外の文字はそのまま返す。
     */
    private static char singleHalfKatakanaToFull(char c) {
        return switch (c) {
            case '\uFF66' -> 'ヲ';
            case '\uFF67' -> 'ァ';
            case '\uFF68' -> 'ィ';
            case '\uFF69' -> 'ゥ';
            case '\uFF6A' -> 'ェ';
            case '\uFF6B' -> 'ォ';
            case '\uFF6C' -> 'ャ';
            case '\uFF6D' -> 'ュ';
            case '\uFF6E' -> 'ョ';
            case '\uFF6F' -> 'ッ';
            case '\uFF70' -> 'ー';
            case '\uFF71' -> 'ア';
            case '\uFF72' -> 'イ';
            case '\uFF73' -> 'ウ';
            case '\uFF74' -> 'エ';
            case '\uFF75' -> 'オ';
            case '\uFF76' -> 'カ';
            case '\uFF77' -> 'キ';
            case '\uFF78' -> 'ク';
            case '\uFF79' -> 'ケ';
            case '\uFF7A' -> 'コ';
            case '\uFF7B' -> 'サ';
            case '\uFF7C' -> 'シ';
            case '\uFF7D' -> 'ス';
            case '\uFF7E' -> 'セ';
            case '\uFF7F' -> 'ソ';
            case '\uFF80' -> 'タ';
            case '\uFF81' -> 'チ';
            case '\uFF82' -> 'ツ';
            case '\uFF83' -> 'テ';
            case '\uFF84' -> 'ト';
            case '\uFF85' -> 'ナ';
            case '\uFF86' -> 'ニ';
            case '\uFF87' -> 'ヌ';
            case '\uFF88' -> 'ネ';
            case '\uFF89' -> 'ノ';
            case '\uFF8A' -> 'ハ';
            case '\uFF8B' -> 'ヒ';
            case '\uFF8C' -> 'フ';
            case '\uFF8D' -> 'ヘ';
            case '\uFF8E' -> 'ホ';
            case '\uFF8F' -> 'マ';
            case '\uFF90' -> 'ミ';
            case '\uFF91' -> 'ム';
            case '\uFF92' -> 'メ';
            case '\uFF93' -> 'モ';
            case '\uFF94' -> 'ヤ';
            case '\uFF95' -> 'ユ';
            case '\uFF96' -> 'ヨ';
            case '\uFF97' -> 'ラ';
            case '\uFF98' -> 'リ';
            case '\uFF99' -> 'ル';
            case '\uFF9A' -> 'レ';
            case '\uFF9B' -> 'ロ';
            case '\uFF9C' -> 'ワ';
            case '\uFF9D' -> 'ン';
            default -> c;
        };
    }

    /**
     * 全角数字・英字・記号を半角に変換する。
     */
    private static String fullWidthToHalfWidth(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\uFF10' && c <= '\uFF19') {
                // 全角数字 → 半角数字
                sb.append((char) (c - '\uFF10' + '0'));
            } else if (c >= '\uFF21' && c <= '\uFF3A') {
                // 全角英大文字 → 半角英大文字
                sb.append((char) (c - '\uFF21' + 'A'));
            } else if (c >= '\uFF41' && c <= '\uFF5A') {
                // 全角英小文字 → 半角英小文字
                sb.append((char) (c - '\uFF41' + 'a'));
            } else if (c == '\uFF0D') {
                // 全角ハイフン → 半角ハイフン
                sb.append('-');
            } else if (c == '\uFF08') {
                // 全角左括弧 → 半角左括弧
                sb.append('(');
            } else if (c == '\uFF09') {
                // 全角右括弧 → 半角右括弧
                sb.append(')');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
