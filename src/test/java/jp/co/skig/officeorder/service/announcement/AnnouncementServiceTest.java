package jp.co.skig.officeorder.service.announcement;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {

    private AnnouncementRepository announcementRepository;
    private AnnouncementService sut;

    @BeforeEach
    void setUp() {
        announcementRepository = mock(AnnouncementRepository.class);
        sut = new AnnouncementService(announcementRepository);
    }

    // --- findHeaderAnnouncements ---

    @Test
    @DisplayName("findHeaderAnnouncements はリポジトリに上限 20 を渡して呼び出す")
    void findHeaderAnnouncements_callsRepositoryWithLimit20() {
        List<AnnouncementView> expected = List.of(mock(AnnouncementView.class));
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(expected);

        List<AnnouncementView> result = sut.findHeaderAnnouncements();

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(announcementRepository).findActiveAnnouncements(captor.capture());
        assertThat(captor.getValue()).isEqualTo(20);
    }

    @Test
    @DisplayName("findHeaderAnnouncements は取得結果をそのまま返す")
    void findHeaderAnnouncements_returnsRepositoryResult() {
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());

        assertThat(sut.findHeaderAnnouncements()).isEmpty();
    }

    // --- findAnnouncementList ---

    @Test
    @DisplayName("findAnnouncementList はリポジトリに null 件数制限を渡す（全件取得）")
    void findAnnouncementList_callsRepositoryWithNullLimit() {
        List<AnnouncementView> expected = List.of(mock(AnnouncementView.class));
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(expected);

        List<AnnouncementView> result = sut.findAnnouncementList();

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(announcementRepository).findActiveAnnouncements(captor.capture());
        assertThat(captor.getValue()).isNull();
    }

    @Test
    @DisplayName("findAnnouncementList は取得結果をそのまま返す")
    void findAnnouncementList_returnsRepositoryResult() {
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of());

        assertThat(sut.findAnnouncementList()).isEmpty();
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    @DisplayName("findHeaderAnnouncements は 50ms 以内に完了する")
    void findHeaderAnnouncements_completesWithinTimeLimit() {
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());
        sut.findHeaderAnnouncements();
    }
}
