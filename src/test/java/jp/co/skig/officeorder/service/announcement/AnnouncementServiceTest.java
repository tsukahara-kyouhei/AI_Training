package jp.co.skig.officeorder.service.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
        private AnnouncementService service;

        @Test
        void findHeaderAnnouncements_正常系_ヘッダ表示件数でお知らせを取得する() {

                List<AnnouncementView> expected = List.of(
                                new AnnouncementView(
                                                1L,
                                                "メンテナンスのお知らせ",
                                                "システムメンテナンスを実施します。",
                                                "2026/08/17",
                                                "2026-08-17"));

                when(announcementRepository.findActiveAnnouncements(20))
                                .thenReturn(expected);

                List<AnnouncementView> result = service.findHeaderAnnouncements();

                assertThat(result)
                                .isSameAs(expected);

                verify(announcementRepository)
                                .findActiveAnnouncements(20);
        }

        @Test
        void findAnnouncementList_正常系_公開中のお知らせ一覧を取得する() {

                List<AnnouncementView> expected = List.of(
                                new AnnouncementView(
                                                1L,
                                                "メンテナンスのお知らせ",
                                                "システムメンテナンスを実施します。",
                                                "2026/08/17",
                                                "2026-08-17"),
                                new AnnouncementView(
                                                2L,
                                                "夏季休業のお知らせ",
                                                "夏季休業期間についてのお知らせです。",
                                                "2026/08/18",
                                                "2026-08-18"));

                when(announcementRepository.findActiveAnnouncements(null))
                                .thenReturn(expected);

                List<AnnouncementView> result = service.findAnnouncementList();

                assertThat(result)
                                .isSameAs(expected);

                verify(announcementRepository)
                                .findActiveAnnouncements(null);
        }
}