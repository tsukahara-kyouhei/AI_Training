package jp.co.skig.officeorder.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 検索キーワードを正規化するユーティリティクラス。
 *
 * <p>全角数字・英字・カタカナを半角に変換し、英字を小文字に統一する。
 * DBの {@code normalize_search_text()} 関数と同じ変換ルールを適用する。
 */
public final class SearchKeywordNormalizer {

    /**
     * 全角カタカナ → 半角カタカナのマッピング。
     * 濁音・半濁音付き文字は2文字のシーケンスに展開する。
     */
    private static final Map<Character, String> KATAKANA_MAP = new LinkedHashMap<>();

    static {
        // 濁音付き（2文字展開）
        KATAKANA_MAP.put('ガ', "ｶﾞ"); KATAKANA_MAP.put('ギ', "ｷﾞ"); KATAKANA_MAP.put('グ', "ｸﾞ");
        KATAKANA_MAP.put('ゲ', "ｹﾞ"); KATAKANA_MAP.put('ゴ', "ｺﾞ"); KATAKANA_MAP.put('ザ', "ｻﾞ");
        KATAKANA_MAP.put('ジ', "ｼﾞ"); KATAKANA_MAP.put('ズ', "ｽﾞ"); KATAKANA_MAP.put('ゼ', "ｾﾞ");
        KATAKANA_MAP.put('ゾ', "ｿﾞ"); KATAKANA_MAP.put('ダ', "ﾀﾞ"); KATAKANA_MAP.put('ヂ', "ﾁﾞ");
        KATAKANA_MAP.put('ヅ', "ﾂﾞ"); KATAKANA_MAP.put('デ', "ﾃﾞ"); KATAKANA_MAP.put('ド', "ﾄﾞ");
        KATAKANA_MAP.put('バ', "ﾊﾞ"); KATAKANA_MAP.put('ビ', "ﾋﾞ"); KATAKANA_MAP.put('ブ', "ﾌﾞ");
        KATAKANA_MAP.put('ベ', "ﾍﾞ"); KATAKANA_MAP.put('ボ', "ﾎﾞ"); KATAKANA_MAP.put('ヴ', "ｳﾞ");

        // 半濁音付き（2文字展開）
        KATAKANA_MAP.put('パ', "ﾊﾟ"); KATAKANA_MAP.put('ピ', "ﾋﾟ"); KATAKANA_MAP.put('プ', "ﾌﾟ");
        KATAKANA_MAP.put('ペ', "ﾍﾟ"); KATAKANA_MAP.put('ポ', "ﾎﾟ");

        // 清音（1文字変換）
        KATAKANA_MAP.put('ア', "ｱ"); KATAKANA_MAP.put('イ', "ｲ"); KATAKANA_MAP.put('ウ', "ｳ");
        KATAKANA_MAP.put('エ', "ｴ"); KATAKANA_MAP.put('オ', "ｵ"); KATAKANA_MAP.put('カ', "ｶ");
        KATAKANA_MAP.put('キ', "ｷ"); KATAKANA_MAP.put('ク', "ｸ"); KATAKANA_MAP.put('ケ', "ｹ");
        KATAKANA_MAP.put('コ', "ｺ"); KATAKANA_MAP.put('サ', "ｻ"); KATAKANA_MAP.put('シ', "ｼ");
        KATAKANA_MAP.put('ス', "ｽ"); KATAKANA_MAP.put('セ', "ｾ"); KATAKANA_MAP.put('ソ', "ｿ");
        KATAKANA_MAP.put('タ', "ﾀ"); KATAKANA_MAP.put('チ', "ﾁ"); KATAKANA_MAP.put('ツ', "ﾂ");
        KATAKANA_MAP.put('テ', "ﾃ"); KATAKANA_MAP.put('ト', "ﾄ"); KATAKANA_MAP.put('ナ', "ﾅ");
        KATAKANA_MAP.put('ニ', "ﾆ"); KATAKANA_MAP.put('ヌ', "ﾇ"); KATAKANA_MAP.put('ネ', "ﾈ");
        KATAKANA_MAP.put('ノ', "ﾉ"); KATAKANA_MAP.put('ハ', "ﾊ"); KATAKANA_MAP.put('ヒ', "ﾋ");
        KATAKANA_MAP.put('フ', "ﾌ"); KATAKANA_MAP.put('ヘ', "ﾍ"); KATAKANA_MAP.put('ホ', "ﾎ");
        KATAKANA_MAP.put('マ', "ﾏ"); KATAKANA_MAP.put('ミ', "ﾐ"); KATAKANA_MAP.put('ム', "ﾑ");
        KATAKANA_MAP.put('メ', "ﾒ"); KATAKANA_MAP.put('モ', "ﾓ"); KATAKANA_MAP.put('ヤ', "ﾔ");
        KATAKANA_MAP.put('ユ', "ﾕ"); KATAKANA_MAP.put('ヨ', "ﾖ"); KATAKANA_MAP.put('ラ', "ﾗ");
        KATAKANA_MAP.put('リ', "ﾘ"); KATAKANA_MAP.put('ル', "ﾙ"); KATAKANA_MAP.put('レ', "ﾚ");
        KATAKANA_MAP.put('ロ', "ﾛ"); KATAKANA_MAP.put('ワ', "ﾜ"); KATAKANA_MAP.put('ヲ', "ｦ");
        KATAKANA_MAP.put('ン', "ﾝ");

        // 小文字カタカナ（1文字変換）
        KATAKANA_MAP.put('ァ', "ｧ"); KATAKANA_MAP.put('ィ', "ｨ"); KATAKANA_MAP.put('ゥ', "ｩ");
        KATAKANA_MAP.put('ェ', "ｪ"); KATAKANA_MAP.put('ォ', "ｫ"); KATAKANA_MAP.put('ッ', "ｯ");
        KATAKANA_MAP.put('ャ', "ｬ"); KATAKANA_MAP.put('ュ', "ｭ"); KATAKANA_MAP.put('ョ', "ｮ");
        KATAKANA_MAP.put('ー', "ｰ"); KATAKANA_MAP.put('ヵ', "ｶ"); KATAKANA_MAP.put('ヶ', "ｹ");
    }

    private SearchKeywordNormalizer() {
    }

    /**
     * キーワードを正規化する。
     *
     * <p>以下の順で変換する:
     * <ol>
     *   <li>全角数字（０-９）→ 半角数字（0-9）</li>
     *   <li>全角英大文字（Ａ-Ｚ）→ 半角英小文字（a-z）</li>
     *   <li>全角英小文字（ａ-ｚ）→ 半角英小文字（a-z）</li>
     *   <li>全角カタカナ → 半角カタカナ（濁音・半濁音は2文字に展開）</li>
     *   <li>半角英大文字（A-Z）→ 半角英小文字（a-z）</li>
     * </ol>
     * ひらがなは変換対象外。
     *
     * @param keyword 入力キーワード
     * @return 正規化済みキーワード。{@code null} または空文字の場合は {@code null}
     */
    public static String normalize(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(keyword.length() * 2);
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);

            // 全角数字 → 半角数字
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
                continue;
            }

            // 全角英大文字 → 半角英小文字
            if (c >= 'Ａ' && c <= 'Ｚ') {
                sb.append((char) (c - 'Ａ' + 'a'));
                continue;
            }

            // 全角英小文字 → 半角英小文字
            if (c >= 'ａ' && c <= 'ｚ') {
                sb.append((char) (c - 'ａ' + 'a'));
                continue;
            }

            // 全角カタカナ → 半角カタカナ
            String mapped = KATAKANA_MAP.get(c);
            if (mapped != null) {
                sb.append(mapped);
                continue;
            }

            sb.append(c);
        }

        // 半角英大文字 → 半角英小文字
        String result = sb.toString().toLowerCase(Locale.ROOT);

        return result.isEmpty() ? null : result;
    }
}
