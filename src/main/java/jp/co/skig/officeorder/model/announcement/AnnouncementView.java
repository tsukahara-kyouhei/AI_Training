package jp.co.skig.officeorder.model.announcement;

/**
 * ヘッダとお知らせ一覧で共通利用するお知らせ表示モデル。
 */
public record AnnouncementView(
                long announcementId,
                String title,
                String body,
                String publishedDateDisplay,
                String publishedDateIso) {
}
