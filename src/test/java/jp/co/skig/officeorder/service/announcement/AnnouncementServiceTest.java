package jp.co.skig.officeorder.service.announcement;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link AnnouncementService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    private AnnouncementService sut;

    @BeforeEach
    void setUp() {
        sut = new AnnouncementService(announcementRepository);
    }

    // ---- AS-01: findHeaderAnnouncements はリポジトリに上限20を渡して結果を返す ----
    @Test
    @DisplayName("ヘッダ向け取得はリポジトリに上限20を渡した結果を返す")
    void as01_findHeaderAnnouncements_returnsRepositoryResult() {
        AnnouncementView notice = new AnnouncementView(1L, "タイトル", "本文", "2026-01-01", "2026-01-01");
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of(notice));

        List<AnnouncementView> result = sut.findHeaderAnnouncements();

        assertThat(result).containsExactly(notice);
    }

    // ---- AS-02: findHeaderAnnouncements でリポジトリが空を返すと空リストを返す ----
    @Test
    @DisplayName("ヘッダ向け取得でお知らせが0件の場合は空リストを返す")
    void as02_findHeaderAnnouncements_emptyWhenRepositoryEmpty() {
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());

        List<AnnouncementView> result = sut.findHeaderAnnouncements();

        assertThat(result).isEmpty();
    }

    // ---- AS-03: findAnnouncementList はリポジトリに null を渡して全件取得する ----
    @Test
    @DisplayName("一覧取得はリポジトリに null を渡した結果を返す（全件）")
    void as03_findAnnouncementList_returnsAllFromRepository() {
        AnnouncementView n1 = new AnnouncementView(1L, "件名1", "本文1", "2026-01-01", "2026-01-01");
        AnnouncementView n2 = new AnnouncementView(2L, "件名2", "本文2", "2026-01-02", "2026-01-02");
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of(n1, n2));

        List<AnnouncementView> result = sut.findAnnouncementList();

        assertThat(result).containsExactly(n1, n2);
    }

    // ---- AS-04: findAnnouncementList でリポジトリが空を返すと空リストを返す ----
    @Test
    @DisplayName("一覧取得でお知らせが0件の場合は空リストを返す")
    void as04_findAnnouncementList_emptyWhenRepositoryEmpty() {
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of());

        List<AnnouncementView> result = sut.findAnnouncementList();

        assertThat(result).isEmpty();
    }
}
