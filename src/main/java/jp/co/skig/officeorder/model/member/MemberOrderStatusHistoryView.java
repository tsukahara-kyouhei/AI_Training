package jp.co.skig.officeorder.model.member;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 注文ステータス履歴1件分の表示モデル。
 */
public record MemberOrderStatusHistoryView(
        OffsetDateTime changedAt,
        String status) {
    private static final DateTimeFormatter CHANGED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * ステータス変更日時を画面表示用の形式へ整形する。
     */
    public String changedAtDisplay() {
        if (changedAt == null) {
            return "-";
        }
        return CHANGED_AT_FORMATTER.format(changedAt);
    }

    /**
     * ステータスコードを日本語ラベルへ変換する。
     */
    public String statusLabel() {
        if ("awaiting_payment".equals(status)) {
            return "入金待ち";
        }
        if ("processing".equals(status)) {
            return "処理中";
        }
        if ("completed".equals(status)) {
            return "完了";
        }
        if ("cancelled".equals(status)) {
            return "キャンセル";
        }
        return "受付";
    }
}
