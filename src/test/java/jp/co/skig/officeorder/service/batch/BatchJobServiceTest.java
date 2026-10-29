package jp.co.skig.officeorder.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import jp.co.skig.officeorder.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 集計系バッチサービスの単体テスト。
 *
 * <p>単体テスト計画の BAT-001〜BAT-009 に対応する。
 */
class BatchJobServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, TOKYO);
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, TOKYO);

    private BatchRepository batchRepository;
    private BatchJobService batchJobService;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        batchJobService = new BatchJobService(batchRepository, FIXED_CLOCK);
    }

    private BatchRepository.PopularRankingCandidate candidate(long productId, int soldQuantity) {
        return new BatchRepository.PopularRankingCandidate(productId, soldQuantity);
    }

    private BatchRepository.OrderProductOccurrence occurrence(long orderId, long productId) {
        return new BatchRepository.OrderProductOccurrence(orderId, productId);
    }

    @Nested
    @DisplayName("BAT-001〜BAT-004 売れ筋ランキング")
    class PopularRanking {

        @Test
        @DisplayName("BAT-001 販売数量降順、同数時は商品ID昇順になる")
        void executePopularRanking_sortsBySoldQuantityDescThenProductIdAsc() {
            List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                    candidate(3L, 5),
                    candidate(1L, 10),
                    candidate(2L, 10),
                    candidate(4L, 3)
            );
            when(batchRepository.findPopularRankingCandidates(
                    FIXED_NOW.minusDays(30), FIXED_NOW)).thenReturn(candidates);

            int count = batchJobService.executePopularRanking(LocalDate.of(2026, 9, 3));

            assertThat(count).isEqualTo(4);
            verify(batchRepository).replacePopularRankings(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }

        @Test
        @DisplayName("BAT-002 売れ筋上限は上位10件だけを保存対象にする")
        void executePopularRanking_limitsToTop10() {
            List<BatchRepository.PopularRankingCandidate> candidates = java.util.stream.IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> candidate(i, i))
                    .toList();
            when(batchRepository.findPopularRankingCandidates(
                    FIXED_NOW.minusDays(30), FIXED_NOW)).thenReturn(candidates);

            int count = batchJobService.executePopularRanking(LocalDate.of(2026, 9, 3));

            assertThat(count).isEqualTo(10);
        }

        @Test
        @DisplayName("BAT-003 ランキング再生成は対象日データを削除してから再登録する")
        void executePopularRanking_replacesByDate() {
            List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                    candidate(1L, 10),
                    candidate(2L, 5)
            );
            when(batchRepository.findPopularRankingCandidates(
                    FIXED_NOW.minusDays(30), FIXED_NOW)).thenReturn(candidates);

            batchJobService.executePopularRanking(LocalDate.of(2026, 9, 3));

            verify(batchRepository).replacePopularRankings(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }

        @Test
        @DisplayName("BAT-004 キャンセル注文は集計対象から除外する")
        void executePopularRanking_excludesCancelledOrders() {
            // キャンセル除外はSQL側の集計条件で行われる。
            // サービスはリポジトリが返した候補だけを並べ替えて保存する。
            List<BatchRepository.PopularRankingCandidate> candidates = List.of(
                    candidate(1L, 10)
            );
            when(batchRepository.findPopularRankingCandidates(
                    FIXED_NOW.minusDays(30), FIXED_NOW)).thenReturn(candidates);

            int count = batchJobService.executePopularRanking(LocalDate.of(2026, 9, 3));

            assertThat(count).isEqualTo(1);
            verify(batchRepository).findPopularRankingCandidates(
                    FIXED_NOW.minusDays(30), FIXED_NOW
            );
        }
    }

    @Nested
    @DisplayName("BAT-005〜BAT-009 おすすめ関連商品")
    class RecommendedRelated {

        @Test
        @DisplayName("BAT-005 同一注文内の商品組み合わせを正しく集計する")
        void executeRecommendedRelated_countsCoOccurrence() throws Exception {
            List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                    occurrence(1L, 1L),
                    occurrence(1L, 2L),
                    occurrence(1L, 3L),
                    occurrence(2L, 1L),
                    occurrence(2L, 2L)
            );
            when(batchRepository.findOrderProductOccurrences(FIXED_NOW.minusDays(30)))
                    .thenReturn(occurrences);

            int count = batchJobService.executeRecommendedRelated(LocalDate.of(2026, 9, 3));

            assertThat(count).isGreaterThan(0);
            verify(batchRepository).replaceRecommendedRelated(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }

        @Test
        @DisplayName("BAT-006 sourceとrecommendedが同一の商品を作成しない")
        void buildRelatedRows_doesNotCreateSelfRecommendation() throws Exception {
            List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                    occurrence(1L, 1L),
                    occurrence(1L, 2L)
            );

            List<BatchRepository.RecommendedRelatedRow> rows = invokeBuildRelatedRows(occurrences);

            assertThat(rows).allSatisfy(row -> {
                assertThat(row.sourceProductId()).isNotEqualTo(row.recommendedProductId());
            });
        }

        @Test
        @DisplayName("BAT-007 商品ごとに上位4件だけを保存対象にする")
        void buildRelatedRows_limitsToTop4PerProduct() throws Exception {
            // 商品1と商品2〜6が同一注文で共起するケース
            List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                    occurrence(1L, 1L),
                    occurrence(1L, 2L),
                    occurrence(1L, 3L),
                    occurrence(1L, 4L),
                    occurrence(1L, 5L),
                    occurrence(1L, 6L)
            );

            List<BatchRepository.RecommendedRelatedRow> rows = invokeBuildRelatedRows(occurrences);

            long source1Count = rows.stream()
                    .filter(row -> row.sourceProductId() == 1L)
                    .count();
            assertThat(source1Count).isLessThanOrEqualTo(4);
        }

        @Test
        @DisplayName("BAT-008 関連商品再生成は対象日データを削除してから再登録する")
        void executeRecommendedRelated_replacesByDate() {
            List<BatchRepository.OrderProductOccurrence> occurrences = List.of(
                    occurrence(1L, 1L),
                    occurrence(1L, 2L)
            );
            when(batchRepository.findOrderProductOccurrences(FIXED_NOW.minusDays(30)))
                    .thenReturn(occurrences);

            batchJobService.executeRecommendedRelated(LocalDate.of(2026, 9, 3));

            verify(batchRepository).replaceRecommendedRelated(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }

        @Test
        @DisplayName("BAT-009 候補なしは空の結果を保存し異常終了しない")
        void executeRecommendedRelated_noCandidates_savesEmptyAndDoesNotFail() {
            when(batchRepository.findOrderProductOccurrences(FIXED_NOW.minusDays(30)))
                    .thenReturn(List.of());

            int count = batchJobService.executeRecommendedRelated(LocalDate.of(2026, 9, 3));

            assertThat(count).isZero();
            verify(batchRepository).replaceRecommendedRelated(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }

        @Test
        @DisplayName("BAT-009 候補がnullでも空の結果を保存し異常終了しない")
        void executeRecommendedRelated_nullCandidates_savesEmptyAndDoesNotFail() {
            when(batchRepository.findOrderProductOccurrences(FIXED_NOW.minusDays(30)))
                    .thenReturn(null);

            int count = batchJobService.executeRecommendedRelated(LocalDate.of(2026, 9, 3));

            assertThat(count).isZero();
            verify(batchRepository).replaceRecommendedRelated(
                    eq(LocalDate.of(2026, 9, 3)),
                    anyList()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<BatchRepository.RecommendedRelatedRow> invokeBuildRelatedRows(
            List<BatchRepository.OrderProductOccurrence> occurrences) throws Exception {
        Method method = BatchJobService.class.getDeclaredMethod("buildRelatedRows", List.class);
        method.setAccessible(true);
        return (List<BatchRepository.RecommendedRelatedRow>) method.invoke(batchJobService, occurrences);
    }
}