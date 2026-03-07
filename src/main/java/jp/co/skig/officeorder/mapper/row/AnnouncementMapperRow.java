package jp.co.skig.officeorder.mapper.row;

import java.time.OffsetDateTime;

/**
 * お知らせ一覧・ヘッダ表示に必要な公開中お知らせの1行。
 */
public record AnnouncementMapperRow(
        Long announcementId,
        String title,
        String body,
        OffsetDateTime publishedStartAt
) {
}
