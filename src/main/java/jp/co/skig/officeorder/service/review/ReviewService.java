package jp.co.skig.officeorder.service.review;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewListResponse;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import jp.co.skig.officeorder.model.review.ReviewView;
import jp.co.skig.officeorder.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * レビューの CRUD ビジネスロジック、購入履歴判定、権限チェックを担当するサービス。
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int DEFAULT_PAGE_SIZE = 5;

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    /**
     * 商品の公開レビュー集計を取得する。
     */
    public ReviewSummaryView findReviewSummary(long productId) {
        return reviewRepository.findSummary(productId);
    }

    /**
     * 商品の公開レビュー一覧を取得する（もっと見る対応）。
     *
     * <p>limit + 1 件取得し、hasNext を判定する。
     */
    public ReviewListResponse findReviews(long productId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        int offset = (normalizedPage - 1) * normalizedSize;
        List<ReviewView> fetched = reviewRepository.findByProduct(productId, normalizedSize + 1, offset);
        boolean hasNext = fetched.size() > normalizedSize;
        List<ReviewView> items = hasNext ? fetched.subList(0, normalizedSize) : fetched;
        return new ReviewListResponse(items, hasNext);
    }

    /**
     * 会員の投稿済みレビューを取得する。
     */
    public Optional<ReviewView> findMyReview(long memberId, long productId) {
        return reviewRepository.findByMemberAndProduct(memberId, productId);
    }

    /**
     * 会員が商品を購入済みか判定する。
     */
    public boolean hasPurchased(long memberId, long productId) {
        return reviewRepository.existsPurchase(memberId, productId);
    }

    /**
     * レビューを投稿する。既存レビューがある場合は更新する。
     */
    @Transactional
    public void postReview(long memberId, long productId, ReviewForm form) {
        Optional<ReviewView> existing = reviewRepository.findByMemberAndProduct(memberId, productId);
        String title = trimToNull(form.getTitle());
        if (existing.isPresent()) {
            ReviewView review = existing.get();
            if (review.blocked()) {
                log.warn("event={} memberId={} reviewId={} reason=blocked",
                        LogEvent.REVIEW_ACTION_REJECTED.value(), memberId, review.reviewId());
                return;
            }
            reviewRepository.update(review.reviewId(), form.getRating(), title, form.getBody(), form.isPublished());
            log.info("event={} memberId={} productId={} reviewId={}",
                    LogEvent.REVIEW_UPDATED.value(), memberId, productId, review.reviewId());
        } else {
            reviewRepository.insert(memberId, productId, form.getRating(), title, form.getBody(), form.isPublished());
            log.info("event={} memberId={} productId={}",
                    LogEvent.REVIEW_POSTED.value(), memberId, productId);
        }
    }

    /**
     * レビューを更新する。本人確認＋ブロック確認を行う。
     */
    @Transactional
    public void updateReview(long memberId, long reviewId, ReviewForm form) {
        ReviewView review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.memberId() != memberId) {
            log.warn("event={} memberId={} reviewId={} reason=not_owner_or_not_found",
                    LogEvent.REVIEW_ACTION_REJECTED.value(), memberId, reviewId);
            return;
        }
        if (review.blocked()) {
            log.warn("event={} memberId={} reviewId={} reason=blocked",
                    LogEvent.REVIEW_ACTION_REJECTED.value(), memberId, reviewId);
            return;
        }
        String title = trimToNull(form.getTitle());
        reviewRepository.update(reviewId, form.getRating(), title, form.getBody(), form.isPublished());
        log.info("event={} memberId={} productId={} reviewId={}",
                LogEvent.REVIEW_UPDATED.value(), memberId, review.productId(), reviewId);
    }

    /**
     * レビューを削除する。本人確認＋ブロック確認を行う。
     */
    @Transactional
    public void deleteReview(long memberId, long reviewId) {
        ReviewView review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.memberId() != memberId) {
            log.warn("event={} memberId={} reviewId={} reason=not_owner_or_not_found",
                    LogEvent.REVIEW_ACTION_REJECTED.value(), memberId, reviewId);
            return;
        }
        if (review.blocked()) {
            log.warn("event={} memberId={} reviewId={} reason=blocked",
                    LogEvent.REVIEW_ACTION_REJECTED.value(), memberId, reviewId);
            return;
        }
        reviewRepository.delete(reviewId);
        log.info("event={} memberId={} productId={} reviewId={}",
                LogEvent.REVIEW_DELETED.value(), memberId, review.productId(), reviewId);
    }

    /**
     * 商品一覧用の一括集計取得。
     */
    public Map<Long, ReviewSummaryView> findReviewSummaries(List<Long> productIds) {
        return reviewRepository.findSummaries(productIds);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
