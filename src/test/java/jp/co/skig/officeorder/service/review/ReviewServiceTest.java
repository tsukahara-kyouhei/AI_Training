package jp.co.skig.officeorder.service.review;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewListResponse;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import jp.co.skig.officeorder.model.review.ReviewView;
import jp.co.skig.officeorder.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewService sut;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        sut = new ReviewService(reviewRepository);
    }

    // --- findReviewSummary ---

    @Test
    @DisplayName("レビューサマリーをリポジトリから取得して返す")
    void findReviewSummary_delegatesToRepository() {
        var summary = new ReviewSummaryView(1L, 5, new BigDecimal("4.2"));
        when(reviewRepository.findSummary(1L)).thenReturn(summary);

        var result = sut.findReviewSummary(1L);

        assertThat(result).isEqualTo(summary);
    }

    // --- findReviews ---

    @Test
    @DisplayName("limit+1 件取得し hasNext=true を返す")
    void findReviews_hasNextTrue() {
        var reviews = List.of(
                review(1), review(2), review(3), review(4), review(5), review(6)
        );
        when(reviewRepository.findByProduct(1L, 6, 0)).thenReturn(reviews);

        ReviewListResponse result = sut.findReviews(1L, 1, 5);

        assertThat(result.items()).hasSize(5);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("limit 以下の件数なら hasNext=false を返す")
    void findReviews_hasNextFalse() {
        var reviews = List.of(review(1), review(2));
        when(reviewRepository.findByProduct(1L, 6, 0)).thenReturn(reviews);

        ReviewListResponse result = sut.findReviews(1L, 1, 5);

        assertThat(result.items()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
    }

    // --- hasPurchased ---

    @Test
    @DisplayName("購入済みならtrueを返す")
    void hasPurchased_returnsTrue() {
        when(reviewRepository.existsPurchase(10L, 1L)).thenReturn(true);
        assertThat(sut.hasPurchased(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("未購入ならfalseを返す")
    void hasPurchased_returnsFalse() {
        when(reviewRepository.existsPurchase(10L, 1L)).thenReturn(false);
        assertThat(sut.hasPurchased(10L, 1L)).isFalse();
    }

    // --- postReview ---

    @Test
    @DisplayName("既存レビューがなければ INSERT する")
    void postReview_insertsWhenNoExisting() {
        when(reviewRepository.findByMemberAndProduct(10L, 1L)).thenReturn(Optional.empty());
        ReviewForm form = createForm(5, "Good", "Great product", true);

        sut.postReview(10L, 1L, form);

        verify(reviewRepository).insert(10L, 1L, 5, "Good", "Great product", true);
    }

    @Test
    @DisplayName("既存レビューがあれば UPDATE する")
    void postReview_updatesWhenExisting() {
        var existing = review(100L, 10L, 1L, false);
        when(reviewRepository.findByMemberAndProduct(10L, 1L)).thenReturn(Optional.of(existing));
        ReviewForm form = createForm(4, "Updated", "Updated body", true);

        sut.postReview(10L, 1L, form);

        verify(reviewRepository).update(100L, 4, "Updated", "Updated body", true);
    }

    @Test
    @DisplayName("既存レビューがブロックされていれば更新しない")
    void postReview_skipsWhenBlocked() {
        var existing = review(100L, 10L, 1L, true);
        when(reviewRepository.findByMemberAndProduct(10L, 1L)).thenReturn(Optional.of(existing));
        ReviewForm form = createForm(4, "Updated", "Updated body", true);

        sut.postReview(10L, 1L, form);

        verify(reviewRepository, never()).update(anyLong(), anyInt(), anyString(), anyString(), anyBoolean());
    }

    // --- updateReview ---

    @Test
    @DisplayName("本人かつ未ブロックなら更新する")
    void updateReview_updatesForOwner() {
        var existing = review(100L, 10L, 1L, false);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));
        ReviewForm form = createForm(3, null, "new body", true);

        sut.updateReview(10L, 100L, form);

        verify(reviewRepository).update(eq(100L), eq(3), isNull(), eq("new body"), eq(true));
    }

    @Test
    @DisplayName("本人でなければ更新しない")
    void updateReview_skipsForNonOwner() {
        var existing = review(100L, 999L, 1L, false);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));
        ReviewForm form = createForm(3, null, "new body", true);

        sut.updateReview(10L, 100L, form);

        verify(reviewRepository, never()).update(anyLong(), anyInt(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("ブロック済みなら更新しない")
    void updateReview_skipsWhenBlocked() {
        var existing = review(100L, 10L, 1L, true);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));
        ReviewForm form = createForm(3, null, "new body", true);

        sut.updateReview(10L, 100L, form);

        verify(reviewRepository, never()).update(anyLong(), anyInt(), anyString(), anyString(), anyBoolean());
    }

    // --- deleteReview ---

    @Test
    @DisplayName("本人かつ未ブロックなら削除する")
    void deleteReview_deletesForOwner() {
        var existing = review(100L, 10L, 1L, false);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));

        sut.deleteReview(10L, 100L);

        verify(reviewRepository).delete(100L);
    }

    @Test
    @DisplayName("本人でなければ削除しない")
    void deleteReview_skipsForNonOwner() {
        var existing = review(100L, 999L, 1L, false);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));

        sut.deleteReview(10L, 100L);

        verify(reviewRepository, never()).delete(anyLong());
    }

    @Test
    @DisplayName("ブロック済みなら削除しない")
    void deleteReview_skipsWhenBlocked() {
        var existing = review(100L, 10L, 1L, true);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(existing));

        sut.deleteReview(10L, 100L);

        verify(reviewRepository, never()).delete(anyLong());
    }

    // --- findReviewSummaries ---

    @Test
    @DisplayName("一括集計を委譲する")
    void findReviewSummaries_delegatesToRepository() {
        var summaries = Map.of(1L, new ReviewSummaryView(1L, 3, new BigDecimal("4.0")));
        when(reviewRepository.findSummaries(List.of(1L))).thenReturn(summaries);

        var result = sut.findReviewSummaries(List.of(1L));

        assertThat(result).isEqualTo(summaries);
    }

    // --- helpers ---

    private ReviewView review(long id) {
        return review(id, 10L, 1L, false);
    }

    private ReviewView review(long id, long memberId, long productId, boolean blocked) {
        return new ReviewView(id, memberId, productId, 5, "title", "body",
                true, blocked, LocalDateTime.now(), LocalDateTime.now());
    }

    private ReviewForm createForm(int rating, String title, String body, boolean published) {
        ReviewForm form = new ReviewForm();
        form.setRating(rating);
        form.setTitle(title);
        form.setBody(body);
        form.setPublished(published);
        return form;
    }
}
