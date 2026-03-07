package jp.co.skig.officeorder.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金額表示用の共通フォーマッタ。
 */
public final class MoneyFormatter {

    private MoneyFormatter() {
    }

    /**
     * 円表示用に3桁区切り文字列へ整形する。
     *
     * @param value 金額
     * @return 整形済み金額文字列
     */
    public static String formatYen(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        String plain = safe.setScale(0, RoundingMode.DOWN).toPlainString();
        StringBuilder builder = new StringBuilder();
        int length = plain.length();
        for (int i = 0; i < length; i++) {
            if (i > 0 && (length - i) % 3 == 0) {
                builder.append(',');
            }
            builder.append(plain.charAt(i));
        }
        return builder.toString();
    }
}
