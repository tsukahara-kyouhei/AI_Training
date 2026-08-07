package jp.co.skig.officeorder.service.announcement;

import java.util.List;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {

    private final AnnouncementRepository announcementRepository = Mockito.mock(AnnouncementRepository.class);
    private final AnnouncementService service = new AnnouncementService(announcementRepository);

    @Test
    void findHeaderAnnouncements_shouldUseLimit20() {
        List<AnnouncementView> expected = List
                .of(new AnnouncementView(1L, "title", "body", "2025-01-01", "2025-01-01"));
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(expected);

        List<AnnouncementView> actual = service.findHeaderAnnouncements();

        assertEquals(expected, actual);
        verify(announcementRepository).findActiveAnnouncements(20);
    }

    @Test
    void findAnnouncementList_shouldRequestAllAnnouncements() {
        List<AnnouncementView> expected = List
                .of(new AnnouncementView(2L, "other", "body", "2025-01-02", "2025-01-02"));
        when(announcementRepository.findActiveAnnouncements(isNull())).thenReturn(expected);

        List<AnnouncementView> actual = service.findAnnouncementList();

        assertEquals(expected, actual);
        verify(announcementRepository).findActiveAnnouncements(isNull());
    }
}
