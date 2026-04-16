package jp.co.skig.officeorder.service.batch;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jp.co.skig.officeorder.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchJobServiceTest {

    private BatchRepository batchRepository;
    private Clock clock;
    private BatchJobService sut;

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 17);

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        clock = Clock.fixed(
                Instant.parse("2026-04-17T00:00:00Z"),
                ZoneId.of("Asia/Tokyo")
        );
        sut = new BatchJobService(batchRepository, clock);
    }

    // --- executePopularRanking ---

    @Test
    @DisplayName("集計候補が 0 件のとき 0 を返す")
    void executePopularRanking_emptyResult_returnsZero() {
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(List.of());

        int count = sut.executePopularRanking(TODAY);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("候補が上限未満のとき全件保存して件数を返す")
    void executePopularRanking_candidatesLessThanLimit_returnsAll() {
        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(1L, 100),
                new BatchRepository.PopularRankingCandidate(2L, 80)
        );
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        int count = sut.executePopularRanking(TODAY);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("候補が上位 10 件超のとき販売数量降順で上位 10 件のみ保存する")
    @SuppressWarnings("unchecked")
    void executePopularRanking_moreThanLimit_returnsTop10() {
        List<BatchRepository.PopularRankingCandidate> candidates = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            candidates.add(new BatchRepository.PopularRankingCandidate(i, i * 10));
        }
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        int count = sut.executePopularRanking(TODAY);

        assertThat(count).isEqualTo(10);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(eq(TODAY), captor.capture());
        List<BatchRepository.PopularRankingCandidate> saved =
                (List<BatchRepository.PopularRankingCandidate>) captor.getValue();
        // 上位 10 件は soldQuantity が最も多い候補のはず（15,14,13...6）
        assertThat(saved.get(0).soldQuantity1m()).isEqualTo(150);
        assertThat(saved.get(9).soldQuantity1m()).isEqualTo(60);
    }

    @Test
    @DisplayName("販売数量が同じ場合は product_id 昇順で並べる")
    @SuppressWarnings("unchecked")
    void executePopularRanking_sameSoldQty_sortsByProductIdAsc() {
        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(10L, 50),
                new BatchRepository.PopularRankingCandidate(3L, 50),
                new BatchRepository.PopularRankingCandidate(7L, 50)
        );
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        sut.executePopularRanking(TODAY);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(eq(TODAY), captor.capture());
        List<BatchRepository.PopularRankingCandidate> saved =
                (List<BatchRepository.PopularRankingCandidate>) captor.getValue();
        assertThat(saved.get(0).productId()).isEqualTo(3L);
        assertThat(saved.get(1).productId()).isEqualTo(7L);
        assertThat(saved.get(2).productId()).isEqualTo(10L);
    }

    // --- executeRecommendedRelated ---

    @Test
    @DisplayName("出現情報が 0 件のとき 0 を返す")
    void executeRecommendedRelated_emptyOccurrences_returnsZero() {
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(List.of());

        int count = sut.executeRecommendedRelated(TODAY);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("1 注文に 1 商品のみの場合はペアが存在しないので 0 を返す")
    void executeRecommendedRelated_singleProductInOrder_returnsZero() {
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 100L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        int count = sut.executeRecommendedRelated(TODAY);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("同一注文に 2 商品がある場合は双方向のペアを生成する")
    @SuppressWarnings("unchecked")
    void executeRecommendedRelated_twoProductsInSameOrder_createsBidirectionalPair() {
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 100L),
                new BatchRepository.OrderProductOccurrence(1L, 200L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        int count = sut.executeRecommendedRelated(TODAY);

        // 100→200 と 200→100 の 2 件が生成される
        assertThat(count).isEqualTo(2);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replaceRecommendedRelated(eq(TODAY), captor.capture());
        List<BatchRepository.RecommendedRelatedRow> rows =
                (List<BatchRepository.RecommendedRelatedRow>) captor.getValue();
        assertThat(rows).hasSize(2);
    }

    @Test
    @DisplayName("1 商品に 5 件以上のペアがあるとき上位 4 件に切り詰める")
    @SuppressWarnings("unchecked")
    void executeRecommendedRelated_moreThanFourPairs_truncatesToFour() {
        // product 1 と 2〜7 が同一注文内に全て存在 → product 1 のペアは 6 件になるが上位 4 件まで
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L),
                new BatchRepository.OrderProductOccurrence(1L, 3L),
                new BatchRepository.OrderProductOccurrence(1L, 4L),
                new BatchRepository.OrderProductOccurrence(1L, 5L),
                new BatchRepository.OrderProductOccurrence(1L, 6L),
                new BatchRepository.OrderProductOccurrence(1L, 7L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        sut.executeRecommendedRelated(TODAY);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replaceRecommendedRelated(eq(TODAY), captor.capture());
        List<BatchRepository.RecommendedRelatedRow> rows =
                (List<BatchRepository.RecommendedRelatedRow>) captor.getValue();
        // 各商品 ID の出現件数 (rank 1〜4 のみ) を確認
        long maxRank = rows.stream().mapToInt(BatchRepository.RecommendedRelatedRow::rank).max().orElse(0);
        assertThat(maxRank).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("コサイン類似度の score が BigDecimal で保存される")
    @SuppressWarnings("unchecked")
    void executeRecommendedRelated_similarityScoreIsBigDecimal() {
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 10L),
                new BatchRepository.OrderProductOccurrence(1L, 20L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        sut.executeRecommendedRelated(TODAY);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replaceRecommendedRelated(eq(TODAY), captor.capture());
        List<BatchRepository.RecommendedRelatedRow> rows =
                (List<BatchRepository.RecommendedRelatedRow>) captor.getValue();
        rows.forEach(row -> {
            assertThat(row.score()).isNotNull();
            assertThat(row.score()).isGreaterThan(BigDecimal.ZERO);
        });
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    @DisplayName("executePopularRanking は 200ms 以内に完了する")
    void executePopularRanking_completesWithinTimeLimit() {
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(List.of());
        sut.executePopularRanking(TODAY);
    }
}
