package jp.co.skig.officeorder.service.announcement;

import java.util.List;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    private AnnouncementService sut;

    @BeforeEach
    void setUp() {
        sut = new AnnouncementService(announcementRepository);
    }

    // ─── findHeaderAnnouncements ────────────────────────────────────────

    @Test
    void findHeaderAnnouncements_called_passes_limit_20_to_repository() {
        // Arrange
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of());

        // Act
        sut.findHeaderAnnouncements();

        // Assert
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(announcementRepository).findActiveAnnouncements(captor.capture());
        assertThat(captor.getValue()).isEqualTo(20);
    }

    @Test
    void findHeaderAnnouncements_repository_returns_items_returns_same_list() {
        // Arrange
        AnnouncementView item = new AnnouncementView(1L, "タイトル", "本文", null, null);
        when(announcementRepository.findActiveAnnouncements(20)).thenReturn(List.of(item));

        // Act
        List<AnnouncementView> result = sut.findHeaderAnnouncements();

        // Assert
        assertThat(result).containsExactly(item);
    }

    // ─── findAnnouncementList ────────────────────────────────────────────

    @Test
    void findAnnouncementList_called_passes_null_limit_to_repository() {
        // Arrange
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of());

        // Act
        sut.findAnnouncementList();

        // Assert
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(announcementRepository).findActiveAnnouncements(captor.capture());
        assertThat(captor.getValue()).isNull();
    }

    @Test
    void findAnnouncementList_repository_returns_items_returns_same_list() {
        // Arrange
        AnnouncementView item1 = new AnnouncementView(1L, "タイトル1", "本文1", null, null);
        AnnouncementView item2 = new AnnouncementView(2L, "タイトル2", "本文2", null, null);
        when(announcementRepository.findActiveAnnouncements(null)).thenReturn(List.of(item1, item2));

        // Act
        List<AnnouncementView> result = sut.findAnnouncementList();

        // Assert
        assertThat(result).containsExactly(item1, item2);
    }
}
