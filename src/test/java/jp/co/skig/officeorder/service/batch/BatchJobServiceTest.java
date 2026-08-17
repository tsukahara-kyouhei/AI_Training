package jp.co.skig.officeorder.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.skig.officeorder.repository.BatchRepository;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private Clock appClock;

    @InjectMocks
    private BatchJobService service;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.of(
                2026, 8, 17,
                10, 0, 0, 0,
                ZoneOffset.ofHours(9));

        when(appClock.instant())
                .thenReturn(now.toInstant());

        when(appClock.getZone())
                .thenReturn(ZoneOffset.ofHours(9));
    }

    @Test
    void executePopularRanking_正常系_販売数量降順で上位10件を保存する() {

        LocalDate rankingDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(1L, 10),
                new BatchRepository.PopularRankingCandidate(2L, 50),
                new BatchRepository.PopularRankingCandidate(3L, 30));

        when(batchRepository.findPopularRankingCandidates(
                now.minusDays(30),
                now))
                .thenReturn(candidates);

        int result = service.executePopularRanking(rankingDate);

        assertThat(result)
                .isEqualTo(3);

        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .replacePopularRankings(
                        eq(rankingDate),
                        captor.capture());

        List<BatchRepository.PopularRankingCandidate> saved = captor.getValue();

        assertThat(saved)
                .extracting(BatchRepository.PopularRankingCandidate::productId)
                .containsExactly(2L, 3L, 1L);

        assertThat(saved)
                .extracting(BatchRepository.PopularRankingCandidate::soldQuantity1m)
                .containsExactly(50, 30, 10);
    }

    @Test
    void executePopularRanking_正常系_販売数量が同じ場合は商品ID昇順にする() {

        LocalDate rankingDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(3L, 20),
                new BatchRepository.PopularRankingCandidate(1L, 20),
                new BatchRepository.PopularRankingCandidate(2L, 20));

        when(batchRepository.findPopularRankingCandidates(
                now.minusDays(30),
                now))
                .thenReturn(candidates);

        service.executePopularRanking(rankingDate);

        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .replacePopularRankings(
                        eq(rankingDate),
                        captor.capture());

        assertThat(captor.getValue())
                .extracting(BatchRepository.PopularRankingCandidate::productId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void executePopularRanking_正常系_11件以上ある場合は上位10件だけ保存する() {

        LocalDate rankingDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.PopularRankingCandidate> candidates = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> new BatchRepository.PopularRankingCandidate(
                        i,
                        i * 10))
                .toList();

        when(batchRepository.findPopularRankingCandidates(
                now.minusDays(30),
                now))
                .thenReturn(candidates);

        int result = service.executePopularRanking(rankingDate);

        assertThat(result)
                .isEqualTo(10);

        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .replacePopularRankings(
                        eq(rankingDate),
                        captor.capture());

        assertThat(captor.getValue())
                .hasSize(10);

        assertThat(captor.getValue())
                .extracting(BatchRepository.PopularRankingCandidate::productId)
                .containsExactly(
                        15L, 14L, 13L, 12L, 11L,
                        10L, 9L, 8L, 7L, 6L);
    }

    @Test
    void executePopularRanking_正常系_候補が空なら0件を保存する() {

        LocalDate rankingDate = LocalDate.of(2026, 8, 17);

        when(batchRepository.findPopularRankingCandidates(
                now.minusDays(30),
                now))
                .thenReturn(List.of());

        int result = service.executePopularRanking(rankingDate);

        assertThat(result)
                .isZero();

        verify(batchRepository)
                .replacePopularRankings(
                        rankingDate,
                        List.of());
    }

    @Test
    void executeRecommendedRelated_正常系_共起商品をスコア順で保存する() {

        LocalDate recommendationDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L),
                new BatchRepository.OrderProductOccurrence(2L, 1L),
                new BatchRepository.OrderProductOccurrence(2L, 2L),
                new BatchRepository.OrderProductOccurrence(2L, 3L));

        when(batchRepository.findOrderProductOccurrences(
                now.minusDays(30)))
                .thenReturn(occurrences);

        int result = service.executeRecommendedRelated(recommendationDate);

        assertThat(result)
                .isEqualTo(6);

        ArgumentCaptor<List<BatchRepository.RecommendedRelatedRow>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .replaceRecommendedRelated(
                        eq(recommendationDate),
                        captor.capture());

        List<BatchRepository.RecommendedRelatedRow> rows = captor.getValue();

        assertThat(rows)
                .isNotEmpty();

        assertThat(rows)
                .allSatisfy(row -> {
                    assertThat(row.rank())
                            .isBetween(1, 4);
                    assertThat(row.score())
                            .isGreaterThan(BigDecimal.ZERO);
                });
    }

    @Test
    void executeRecommendedRelated_正常系_同一注文内の同一商品は重複カウントしない() {

        LocalDate recommendationDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L));

        when(batchRepository.findOrderProductOccurrences(
                now.minusDays(30)))
                .thenReturn(occurrences);

        service.executeRecommendedRelated(recommendationDate);

        ArgumentCaptor<List<BatchRepository.RecommendedRelatedRow>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .replaceRecommendedRelated(
                        eq(recommendationDate),
                        captor.capture());

        List<BatchRepository.RecommendedRelatedRow> rows = captor.getValue();

        assertThat(rows)
                .hasSize(2);

        assertThat(rows)
                .allSatisfy(row -> assertThat(row.score())
                        .isEqualByComparingTo(BigDecimal.ONE));
    }

    @Test
    void executeRecommendedRelated_正常系_1商品あたり上位4件まで保存する() {
        // Arrange
        LocalDate recommendationDate = LocalDate.of(2026, 8, 17);

        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L),
                new BatchRepository.OrderProductOccurrence(1L, 3L),
                new BatchRepository.OrderProductOccurrence(1L, 4L),
                new BatchRepository.OrderProductOccurrence(1L, 5L),

                new BatchRepository.OrderProductOccurrence(2L, 1L),
                new BatchRepository.OrderProductOccurrence(2L, 2L),
                new BatchRepository.OrderProductOccurrence(2L, 3L),
                new BatchRepository.OrderProductOccurrence(2L, 4L),
                new BatchRepository.OrderProductOccurrence(2L, 5L),

                new BatchRepository.OrderProductOccurrence(3L, 1L),
                new BatchRepository.OrderProductOccurrence(3L, 2L),
                new BatchRepository.OrderProductOccurrence(3L, 3L),
                new BatchRepository.OrderProductOccurrence(3L, 4L),
                new BatchRepository.OrderProductOccurrence(3L, 5L),

                new BatchRepository.OrderProductOccurrence(4L, 1L),
                new BatchRepository.OrderProductOccurrence(4L, 2L),
                new BatchRepository.OrderProductOccurrence(4L, 3L),
                new BatchRepository.OrderProductOccurrence(4L, 4L),
                new BatchRepository.OrderProductOccurrence(4L, 5L),

                new BatchRepository.OrderProductOccurrence(5L, 1L),
                new BatchRepository.OrderProductOccurrence(5L, 2L),
                new BatchRepository.OrderProductOccurrence(5L, 3L),
                new BatchRepository.OrderProductOccurrence(5L, 4L),
                new BatchRepository.OrderProductOccurrence(5L, 5L),

                new BatchRepository.OrderProductOccurrence(6L, 1L),
                new BatchRepository.OrderProductOccurrence(6L, 2L),
                new BatchRepository.OrderProductOccurrence(6L, 3L),
                new BatchRepository.OrderProductOccurrence(6L, 4L),
                new BatchRepository.OrderProductOccurrence(6L, 5L));

        when(batchRepository.findOrderProductOccurrences(any(OffsetDateTime.class)))
                .thenReturn(occurrences);

        // Act
        int result = service.executeRecommendedRelated(recommendationDate);

        // Assert
        assertThat(result).isEqualTo(20);

        ArgumentCaptor<List<BatchRepository.RecommendedRelatedRow>> captor = ArgumentCaptor.forClass(List.class);

        verify(batchRepository).replaceRecommendedRelated(
                eq(recommendationDate),
                captor.capture());

        List<BatchRepository.RecommendedRelatedRow> savedRows = captor.getValue();

        assertThat(savedRows).hasSize(20);

        // 各商品について、関連商品が最大4件まで保存されていることを確認
        Map<Long, List<BatchRepository.RecommendedRelatedRow>> rowsBySource = savedRows.stream()
                .collect(Collectors.groupingBy(
                        BatchRepository.RecommendedRelatedRow::sourceProductId));

        assertThat(rowsBySource).hasSize(5);

        for (List<BatchRepository.RecommendedRelatedRow> rows : rowsBySource.values()) {
            assertThat(rows).hasSize(4);

            assertThat(rows)
                    .extracting(BatchRepository.RecommendedRelatedRow::rank)
                    .containsExactly(1, 2, 3, 4);
        }
    }

    @Test
    void executeRecommendedRelated_正常系_空の出現情報なら0件を保存する() {

        LocalDate recommendationDate = LocalDate.of(2026, 8, 17);

        when(batchRepository.findOrderProductOccurrences(
                now.minusDays(30)))
                .thenReturn(List.of());

        int result = service.executeRecommendedRelated(recommendationDate);

        assertThat(result)
                .isZero();

        verify(batchRepository)
                .replaceRecommendedRelated(
                        recommendationDate,
                        List.of());
    }

    @Test
    void executeRecommendedRelated_異常系_nullの出現情報でも0件を保存する() {

        LocalDate recommendationDate = LocalDate.of(2026, 8, 17);

        when(batchRepository.findOrderProductOccurrences(
                now.minusDays(30)))
                .thenReturn(null);

        int result = service.executeRecommendedRelated(recommendationDate);

        assertThat(result)
                .isZero();

        verify(batchRepository)
                .replaceRecommendedRelated(
                        recommendationDate,
                        List.of());
    }
}
