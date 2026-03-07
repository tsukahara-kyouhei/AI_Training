package jp.co.skig.officeorder.service.batch;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import jp.co.skig.officeorder.repository.BatchRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchJobServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-07T00:00:00Z"), ZoneOffset.UTC);

    private final BatchRepository batchRepository = mock(BatchRepository.class);
    private final BatchJobService service = new BatchJobService(batchRepository, FIXED_CLOCK);

    /**
     * 売れ筋ランキング再計算で、販売数量降順・商品ID昇順のルールどおりに保存対象が並ぶことを確認する。
     */
    @Test
    void executePopularRanking_sortsBySoldQuantityDescendingAndProductIdAscending() {
        LocalDate rankingDate = LocalDate.of(2026, 3, 7);
        OffsetDateTime expectedNow = OffsetDateTime.parse("2026-03-07T00:00:00Z");
        OffsetDateTime expectedSinceAt = expectedNow.minusDays(30);
        when(batchRepository.findPopularRankingCandidates(expectedSinceAt, expectedNow)).thenReturn(List.of(
                new BatchRepository.PopularRankingCandidate(30L, 8),
                new BatchRepository.PopularRankingCandidate(10L, 10),
                new BatchRepository.PopularRankingCandidate(20L, 10),
                new BatchRepository.PopularRankingCandidate(40L, 2)
        ));

        int savedCount = service.executePopularRanking(rankingDate);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(org.mockito.ArgumentMatchers.eq(rankingDate), captor.capture());
        assertThat(savedCount).isEqualTo(4);
        assertThat(captor.getValue())
                .extracting(BatchRepository.PopularRankingCandidate::productId)
                .containsExactly(10L, 20L, 30L, 40L);
    }

    /**
     * おすすめ関連商品再計算で、注文内共起からコサイン類似度ベースの順位が生成されることを確認する。
     */
    @Test
    void executeRecommendedRelated_buildsCosineSimilarityRankingRows() {
        LocalDate recommendationDate = LocalDate.of(2026, 3, 7);
        OffsetDateTime expectedSinceAt = OffsetDateTime.parse("2026-02-05T00:00:00Z");
        when(batchRepository.findOrderProductOccurrences(expectedSinceAt)).thenReturn(List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L),
                new BatchRepository.OrderProductOccurrence(1L, 3L),
                new BatchRepository.OrderProductOccurrence(2L, 1L),
                new BatchRepository.OrderProductOccurrence(2L, 2L),
                new BatchRepository.OrderProductOccurrence(3L, 1L),
                new BatchRepository.OrderProductOccurrence(3L, 4L)
        ));

        int savedCount = service.executeRecommendedRelated(recommendationDate);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.RecommendedRelatedRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replaceRecommendedRelated(org.mockito.ArgumentMatchers.eq(recommendationDate), captor.capture());

        List<BatchRepository.RecommendedRelatedRow> actual = captor.getValue().stream()
                .sorted(Comparator
                        .comparingLong(BatchRepository.RecommendedRelatedRow::sourceProductId)
                        .thenComparingInt(BatchRepository.RecommendedRelatedRow::rank))
                .toList();

        assertThat(savedCount).isEqualTo(8);
        assertThat(actual).hasSize(8);
        assertRecommendation(actual.get(0), 1L, 2L, 1, "0.816497");
        assertRecommendation(actual.get(1), 1L, 3L, 2, "0.577350");
        assertRecommendation(actual.get(2), 1L, 4L, 3, "0.577350");
        assertRecommendation(actual.get(3), 2L, 1L, 1, "0.816497");
        assertRecommendation(actual.get(4), 2L, 3L, 2, "0.707107");
        assertRecommendation(actual.get(5), 3L, 2L, 1, "0.707107");
        assertRecommendation(actual.get(6), 3L, 1L, 2, "0.577350");
        assertRecommendation(actual.get(7), 4L, 1L, 1, "0.577350");
    }

    private void assertRecommendation(BatchRepository.RecommendedRelatedRow actual,
                                      long sourceProductId,
                                      long recommendedProductId,
                                      int rank,
                                      String score) {
        assertThat(actual.sourceProductId()).isEqualTo(sourceProductId);
        assertThat(actual.recommendedProductId()).isEqualTo(recommendedProductId);
        assertThat(actual.rank()).isEqualTo(rank);
        assertThat(actual.score()).isEqualByComparingTo(new BigDecimal(score));
    }
}
