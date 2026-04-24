package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.mapper.ReviewMapper;
import jp.co.skig.officeorder.mapper.row.ReviewMapperRow;
import jp.co.skig.officeorder.mapper.row.ReviewSummaryMapperRow;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import jp.co.skig.officeorder.model.review.ReviewView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewRepositoryTest {

    private ReviewMapper reviewMapper;
    private ReviewRepository sut;

    @BeforeEach
    void setUp() {
        reviewMapper = mock(ReviewMapper.class);
        sut = new ReviewRepository(reviewMapper);
    }

    @Test
    @DisplayName("レビューサマリーがある場合は平均評価を小数点1桁で返す")
    void findSummary_returnsRounded() {
        when(reviewMapper.selectReviewSummary(1L))
                .thenReturn(new ReviewSummaryMapperRow(1L, 3, new BigDecimal("4.333")));

        ReviewSummaryView result = sut.findSummary(1L);

        assertThat(result.reviewCount()).isEqualTo(3);
        assertThat(result.averageRating()).isEqualTo(new BigDecimal("4.3"));
    }

    @Test
    @DisplayName("レビューサマリーがない場合はデフォルトを返す")
    void findSummary_returnsEmpty() {
        when(reviewMapper.selectReviewSummary(1L)).thenReturn(null);

        ReviewSummaryView result = sut.findSummary(1L);

        assertThat(result.reviewCount()).isZero();
        assertThat(result.averageRating()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("findByProduct は MapperRow を View に変換する")
    void findByProduct_convertsRows() {
        var row = new ReviewMapperRow(1L, 10L, 1L, 5, "Good", "Body",
                true, false, LocalDateTime.now(), LocalDateTime.now());
        when(reviewMapper.selectReviewsByProduct(1L, 5, 0)).thenReturn(List.of(row));

        List<ReviewView> result = sut.findByProduct(1L, 5, 0);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().reviewId()).isEqualTo(1L);
        assertThat(result.getFirst().rating()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByMemberAndProduct が存在する場合 Optional.of を返す")
    void findByMemberAndProduct_found() {
        var row = new ReviewMapperRow(1L, 10L, 1L, 4, null, "Body",
                true, false, LocalDateTime.now(), LocalDateTime.now());
        when(reviewMapper.selectByMemberAndProduct(10L, 1L)).thenReturn(row);

        Optional<ReviewView> result = sut.findByMemberAndProduct(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().rating()).isEqualTo(4);
    }

    @Test
    @DisplayName("findByMemberAndProduct が存在しない場合 empty を返す")
    void findByMemberAndProduct_notFound() {
        when(reviewMapper.selectByMemberAndProduct(10L, 1L)).thenReturn(null);

        assertThat(sut.findByMemberAndProduct(10L, 1L)).isEmpty();
    }

    @Test
    @DisplayName("findSummaries は productIds が空なら空 Map を返す")
    void findSummaries_emptyList() {
        Map<Long, ReviewSummaryView> result = sut.findSummaries(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findSummaries は結果を productId でマッピングする")
    void findSummaries_mapsById() {
        var rows = List.of(
                new ReviewSummaryMapperRow(1L, 5, new BigDecimal("4.0")),
                new ReviewSummaryMapperRow(2L, 3, new BigDecimal("3.5"))
        );
        when(reviewMapper.selectReviewSummaries(List.of(1L, 2L))).thenReturn(rows);

        Map<Long, ReviewSummaryView> result = sut.findSummaries(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).reviewCount()).isEqualTo(5);
        assertThat(result.get(2L).reviewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("existsPurchase は Mapper 結果を返す")
    void existsPurchase_delegatesToMapper() {
        when(reviewMapper.existsPurchase(10L, 1L)).thenReturn(true);
        assertThat(sut.existsPurchase(10L, 1L)).isTrue();
    }
}
