package jp.co.skig.officeorder.service.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    AnnouncementRepository announcementRepository;

    @InjectMocks
    AnnouncementService announcementService;

    // --- findHeaderAnnouncements ---

    @Test
    void findHeaderAnnouncements_delegatesWithLimitTwenty() {
        List<AnnouncementView> expected = List.of(
                new AnnouncementView(1L, "お知らせ1", "本文1", "2026年4月15日", "2026-04-15"));
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(expected);

        List<AnnouncementView> result = announcementService.findHeaderAnnouncements();

        assertThat(result).isEqualTo(expected);
        verify(announcementRepository).findActiveAnnouncements(20);
    }

    @Test
    void findHeaderAnnouncements_emptyList_returnsEmpty() {
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());

        List<AnnouncementView> result = announcementService.findHeaderAnnouncements();

        assertThat(result).isEmpty();
    }

    // --- findAnnouncementList ---

    @Test
    void findAnnouncementList_delegatesWithNullLimit() {
        List<AnnouncementView> expected = List.of(
                new AnnouncementView(1L, "お知らせ1", "本文1", "2026年4月15日", "2026-04-15"),
                new AnnouncementView(2L, "お知らせ2", "本文2", "2026年4月14日", "2026-04-14"));
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(expected);

        List<AnnouncementView> result = announcementService.findAnnouncementList();

        assertThat(result).isEqualTo(expected);
        verify(announcementRepository).findActiveAnnouncements(null);
    }

    @Test
    void findAnnouncementList_emptyList_returnsEmpty() {
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of());

        List<AnnouncementView> result = announcementService.findAnnouncementList();

        assertThat(result).isEmpty();
    }
}
