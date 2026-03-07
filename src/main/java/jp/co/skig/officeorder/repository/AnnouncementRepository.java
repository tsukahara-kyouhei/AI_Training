package jp.co.skig.officeorder.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.row.AnnouncementMapperRow;
import jp.co.skig.officeorder.mapper.AnnouncementMapper;
import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import org.springframework.stereotype.Repository;

/**
 * お知らせデータの取得と画面表示用整形を担当するリポジトリ。
 *
 * <p>公開期間内のお知らせ取得に加えて、画面でそのまま使う掲載日表示形式への変換もここで行う。
 */
@Repository
public class AnnouncementRepository {

    /** 画面表示用の日付フォーマット。 */
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /** HTML datetime 属性用の日付フォーマット。 */
    private static final DateTimeFormatter DATE_ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** お知らせSQLアクセスを担当する MyBatis Mapper。 */
    private final AnnouncementMapper announcementMapper;
    /** 公開期間判定の基準時刻を返す共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * お知らせリポジトリを生成する。
     *
     * @param announcementMapper お知らせMapper
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public AnnouncementRepository(AnnouncementMapper announcementMapper,
                                  AppTimeProvider appTimeProvider) {
        this.announcementMapper = announcementMapper;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * 公開中のお知らせを取得し、画面表示用へ整形する。
     *
     * @param limit 取得件数上限。無制限時は {@code null}
     * @return お知らせ一覧
     */
    public List<AnnouncementView> findActiveAnnouncements(Integer limit) {
        List<AnnouncementMapperRow> rows = announcementMapper.selectActiveAnnouncements(
                limit,
                appTimeProvider.nowOffsetDateTime()
        );
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(this::toAnnouncementView)
                .toList();
    }

    /**
     * Mapper行を画面表示用お知らせへ変換する。
     *
     * @param row お知らせ行
     * @return 画面表示用お知らせ
     */
    private AnnouncementView toAnnouncementView(AnnouncementMapperRow row) {
        LocalDate publishedDate = toLocalDate(row.publishedStartAt());
        String dateDisplay = publishedDate == null ? "-" : publishedDate.format(DATE_DISPLAY_FORMATTER);
        String dateIso = publishedDate == null ? "" : publishedDate.format(DATE_ISO_FORMATTER);
        return new AnnouncementView(
                row.announcementId(),
                row.title() == null ? "" : row.title(),
                row.body() == null ? "" : row.body(),
                dateDisplay,
                dateIso
        );
    }

    /**
     * Mapper返却値を掲載日表示用の {@link LocalDate} へ変換する。
     *
     * <p>DBドライバ差異により型がぶれても扱えるよう、複数型を許容している。
     *
     * @param value 変換対象
     * @return 掲載日
     */
    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(appTimeProvider.zoneId()).toLocalDate();
        }
        return null;
    }
}





