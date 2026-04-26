package jp.co.skig.officeorder.service.announcement;

import java.util.List;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AnnouncementService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @InjectMocks
    AnnouncementService sut;

    @Mock
    AnnouncementRepository announcementRepository;

    // ── findHeaderAnnouncements ──────────────────────────────────────────

    @Test
    void ヘッダ用お知らせ取得でリポジトリに上限20件を渡すこと() {
        var views = List.of(
                new AnnouncementView(1L, "title1", "body1", "2026/01/01", "2026-01-01")
        );
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(views);

        var result = sut.findHeaderAnnouncements();

        verify(announcementRepository).findActiveAnnouncements(20);
        assertThat(result).isEqualTo(views);
    }

    @Test
    void ヘッダ用お知らせが0件の場合に空リストが返ること() {
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());

        var result = sut.findHeaderAnnouncements();

        assertThat(result).isEmpty();
    }

    // ── findAnnouncementList ─────────────────────────────────────────────

    @Test
    void 一覧取得でリポジトリにnullを渡すこと() {
        var views = List.of(
                new AnnouncementView(1L, "タイトル", "本文", "2026/01/01", "2026-01-01"),
                new AnnouncementView(2L, "タイトル2", "本文2", "2026/01/02", "2026-01-02")
        );
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(views);

        var result = sut.findAnnouncementList();

        verify(announcementRepository).findActiveAnnouncements(null);
        assertThat(result).hasSize(2);
    }

    @Test
    void お知らせ一覧が0件の場合に空リストが返ること() {
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of());

        var result = sut.findAnnouncementList();

        assertThat(result).isEmpty();
    }
}
