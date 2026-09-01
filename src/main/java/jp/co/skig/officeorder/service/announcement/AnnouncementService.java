package jp.co.skig.officeorder.service.announcement;

import java.util.List;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

/**
 * 公開中のお知らせを画面表示用に取得するサービス。
 *
 * <p>
 * ヘッダの1行お知らせ表示と、お知らせ一覧画面のどちらも
 * {@link AnnouncementRepository} の同じ公開条件を使うため、
 * 取得件数だけをここで切り替えている。
 */
@Service
public class AnnouncementService {

    /** ヘッダに表示するお知らせの最大件数。 */
    private static final int HEADER_NOTICE_LIMIT = 20;

    /** お知らせ取得を担当するリポジトリ。 */
    private final AnnouncementRepository announcementRepository;

    /**
     * お知らせ取得サービスを生成する。
     *
     * @param announcementRepository 公開中お知らせを取得するリポジトリ
     */
    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /**
     * ヘッダ表示用のお知らせ一覧を取得する。
     *
     * @return ヘッダ表示件数に絞った公開中お知らせ
     */
    public List<AnnouncementView> findHeaderAnnouncements() {
        return announcementRepository.findActiveAnnouncements(HEADER_NOTICE_LIMIT);
    }

    /**
     * お知らせ一覧画面用の公開中お知らせを取得する。
     *
     * @return 公開中のお知らせ一覧
     */
    public List<AnnouncementView> findAnnouncementList() {
        return announcementRepository.findActiveAnnouncements(null);
    }
}
