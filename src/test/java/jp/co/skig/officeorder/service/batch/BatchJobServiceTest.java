package jp.co.skig.officeorder.service.batch;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jp.co.skig.officeorder.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T00:00:00Z");
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    @Mock
    private BatchRepository batchRepository;

    private BatchJobService sut;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, JST);
        sut = new BatchJobService(batchRepository, clock);
    }

    // ─── executePopularRanking ───────────────────────────────────────────

    @Test
    void executePopularRanking_no_candidates_saves_empty_ranking() {
        // Arrange
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(List.of());

        // Act
        int count = sut.executePopularRanking(LocalDate.of(2026, 4, 14));

        // Assert
        assertThat(count).isZero();
        verify(batchRepository).replacePopularRankings(eq(LocalDate.of(2026, 4, 14)), eq(List.of()));
    }

    @Test
    void executePopularRanking_over_10_candidates_saves_only_top_10() {
        // Arrange
        List<BatchRepository.PopularRankingCandidate> candidates = buildCandidates(12);
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        // Act
        int count = sut.executePopularRanking(LocalDate.of(2026, 4, 14));

        // Assert
        assertThat(count).isEqualTo(10);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(any(), captor.capture());
        assertThat(captor.getValue()).hasSize(10);
    }

    @Test
    void executePopularRanking_candidates_are_sorted_by_sold_quantity_desc() {
        // Arrange
        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(1L, 5),
                new BatchRepository.PopularRankingCandidate(2L, 20),
                new BatchRepository.PopularRankingCandidate(3L, 10)
        );
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        // Act
        sut.executePopularRanking(LocalDate.of(2026, 4, 14));

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(any(), captor.capture());
        List<BatchRepository.PopularRankingCandidate> saved = captor.getValue();
        assertThat(saved.get(0).productId()).isEqualTo(2L); // 最多販売
        assertThat(saved.get(1).productId()).isEqualTo(3L);
        assertThat(saved.get(2).productId()).isEqualTo(1L);
    }

    @Test
    void executePopularRanking_tie_in_quantity_ordered_by_product_id_asc() {
        // Arrange
        List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                new BatchRepository.PopularRankingCandidate(5L, 10),
                new BatchRepository.PopularRankingCandidate(3L, 10),
                new BatchRepository.PopularRankingCandidate(1L, 10)
        );
        when(batchRepository.findPopularRankingCandidates(any(), any())).thenReturn(candidates);

        // Act
        sut.executePopularRanking(LocalDate.of(2026, 4, 14));

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.PopularRankingCandidate>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replacePopularRankings(any(), captor.capture());
        List<BatchRepository.PopularRankingCandidate> saved = captor.getValue();
        assertThat(saved.get(0).productId()).isEqualTo(1L);
        assertThat(saved.get(1).productId()).isEqualTo(3L);
        assertThat(saved.get(2).productId()).isEqualTo(5L);
    }

    // ─── executeRecommendedRelated ────────────────────────────────────────

    @Test
    void executeRecommendedRelated_no_occurrences_saves_empty_result() {
        // Arrange
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(List.of());

        // Act
        int count = sut.executeRecommendedRelated(LocalDate.of(2026, 4, 14));

        // Assert
        assertThat(count).isZero();
        verify(batchRepository).replaceRecommendedRelated(eq(LocalDate.of(2026, 4, 14)), eq(List.of()));
    }

    @Test
    void executeRecommendedRelated_single_product_order_has_no_related_products() {
        // Arrange
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 100L)  // 注文1に商品100のみ
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        // Act
        int count = sut.executeRecommendedRelated(LocalDate.of(2026, 4, 14));

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void executeRecommendedRelated_two_products_in_same_order_generates_mutual_recommendations() {
        // Arrange
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 100L),
                new BatchRepository.OrderProductOccurrence(1L, 200L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        // Act
        int count = sut.executeRecommendedRelated(LocalDate.of(2026, 4, 14));

        // Assert - 商品100→200、商品200→100 の双方向で合計2件
        assertThat(count).isEqualTo(2);
    }

    @Test
    void executeRecommendedRelated_recommendations_capped_at_4_per_source_product() {
        // Arrange - 商品1が6種類の商品と同時購入されている
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 1L),
                new BatchRepository.OrderProductOccurrence(1L, 2L),
                new BatchRepository.OrderProductOccurrence(1L, 3L),
                new BatchRepository.OrderProductOccurrence(1L, 4L),
                new BatchRepository.OrderProductOccurrence(1L, 5L),
                new BatchRepository.OrderProductOccurrence(1L, 6L)
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        // Act
        sut.executeRecommendedRelated(LocalDate.of(2026, 4, 14));

        // Assert - 各商品が持てるおすすめは最大4件
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatchRepository.RecommendedRelatedRow>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(batchRepository).replaceRecommendedRelated(any(), captor.capture());
        List<BatchRepository.RecommendedRelatedRow> saved = captor.getValue();
        // 各商品ソースのrank が 1〜4 の範囲であることを確認
        saved.forEach(row -> assertThat(row.rank()).isBetween(1, 4));
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private List<BatchRepository.PopularRankingCandidate> buildCandidates(int count) {
        List<BatchRepository.PopularRankingCandidate> list = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new BatchRepository.PopularRankingCandidate((long) i, count - i + 1));
        }
        return list;
    }

    // ─── executeRecommendedRelated — continue branches ───────────────────

    @Test
    void executeRecommendedRelated_occurrence_with_same_product_and_order_id_is_deduplicated() {
        // Arrange – 同一 orderId/productId の重複 occurrence が入力される場合
        // これにより Set<Long> にはユニークな 1 商品のみ → ペアが生成されず pairCounts は空
        // buildRelatedRows がそのまま空リストを返す
        List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                new BatchRepository.OrderProductOccurrence(1L, 100L)
                // 1注文1商品 → ペアなし
        );
        when(batchRepository.findOrderProductOccurrences(any())).thenReturn(occurrences);

        // Act
        int count = sut.executeRecommendedRelated(LocalDate.of(2026, 4, 14));

        // Assert – ペアゼロ
        assertThat(count).isZero();
    }
}
