package jp.co.skig.officeorder.service.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.skig.officeorder.model.announcement.AnnouncementView;
import jp.co.skig.officeorder.repository.AnnouncementRepository;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    @Nested
    @DisplayName("findHeaderAnnouncementsのテスト")
    class FindHeaderAnnouncementsTest {

        @Test
        @DisplayName("お知らせが存在する場合、件数制限20件で正しく取得できること")
        void shouldReturnHeaderAnnouncementsWhenExist() {
            // モックのセットアップ（Recordのコンストラクタに合わせて引数を渡す）
            AnnouncementView mockView = new AnnouncementView(1L, "タイトル", "本文", "2026-08-17", "URL");
            List<AnnouncementView> expectedList = List.of(mockView);
            when(announcementRepository.findActiveAnnouncements(20)).thenReturn(expectedList);

            // 実行
            List<AnnouncementView> actualList = announcementService.findHeaderAnnouncements();

            // 検証
            assertThat(actualList).isNotNull();
            assertThat(actualList).hasSize(1);
            assertThat(actualList).isEqualTo(expectedList);
            verify(announcementRepository).findActiveAnnouncements(20);
        }

        @Test
        @DisplayName("お知らせが存在しない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoHeaderAnnouncements() {
            // モックのセットアップ
            when(announcementRepository.findActiveAnnouncements(20)).thenReturn(Collections.emptyList());

            // 実行
            List<AnnouncementView> actualList = announcementService.findHeaderAnnouncements();

            // 検証
            assertThat(actualList).isEmpty();
            verify(announcementRepository).findActiveAnnouncements(20);
        }
    }

    @Nested
    @DisplayName("findAnnouncementListのテスト")
    class FindAnnouncementListTest {

        @Test
        @DisplayName("お知らせ一覧画面用のお知らせが全件（引数nullで）取得できること")
        void shouldReturnAllAnnouncements() {
            // モックのセットアップ（Recordのコンストラクタに合わせて引数を渡す）
            AnnouncementView mockView = new AnnouncementView(1L, "タイトル", "本文", "2026-08-17", "URL");
            List<AnnouncementView> expectedList = List.of(mockView);
            when(announcementRepository.findActiveAnnouncements(null)).thenReturn(expectedList);

            // 実行
            List<AnnouncementView> actualList = announcementService.findAnnouncementList();

            // 検証
            assertThat(actualList).isNotNull();
            assertThat(actualList).hasSize(1);
            assertThat(actualList).isEqualTo(expectedList);
            verify(announcementRepository).findActiveAnnouncements(null);
        }
    }
}