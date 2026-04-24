package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.mapper.ReviewMapper;
import jp.co.skig.officeorder.mapper.row.ReviewMapperRow;
import jp.co.skig.officeorder.mapper.row.ReviewSummaryMapperRow;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import jp.co.skig.officeorder.model.review.ReviewView;
import org.springframework.stereotype.Repository;

/**
 * レビュー関連データの取得・更新と画面表示用整形を担当するリポジトリ。
 */
@Repository
public class ReviewRepository {

    private final ReviewMapper reviewMapper;

    public ReviewRepository(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    public ReviewSummaryView findSummary(long productId) {
        ReviewSummaryMapperRow row = reviewMapper.selectReviewSummary(productId);
        if (row == null) {
            return ReviewSummaryView.empty(productId);
        }
        return toSummaryView(row);
    }

    public List<ReviewView> findByProduct(long productId, int limit, int offset) {
        return reviewMapper.selectReviewsByProduct(productId, limit, offset).stream()
                .map(this::toView)
                .toList();
    }

    public Optional<ReviewView> findByMemberAndProduct(long memberId, long productId) {
        ReviewMapperRow row = reviewMapper.selectByMemberAndProduct(memberId, productId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(toView(row));
    }

    public Optional<ReviewView> findById(long reviewId) {
        ReviewMapperRow row = reviewMapper.selectById(reviewId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(toView(row));
    }

    public boolean existsPurchase(long memberId, long productId) {
        return Boolean.TRUE.equals(reviewMapper.existsPurchase(memberId, productId));
    }

    public void insert(long memberId, long productId, int rating, String title, String body, boolean isPublished) {
        reviewMapper.insertReview(memberId, productId, rating, title, body, isPublished);
    }

    public int update(long reviewId, int rating, String title, String body, boolean isPublished) {
        return reviewMapper.updateReview(reviewId, rating, title, body, isPublished);
    }

    public void delete(long reviewId) {
        reviewMapper.deleteReview(reviewId);
    }

    public Map<Long, ReviewSummaryView> findSummaries(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ReviewSummaryMapperRow> rows = reviewMapper.selectReviewSummaries(productIds);
        Map<Long, ReviewSummaryView> result = new HashMap<>();
        for (ReviewSummaryMapperRow row : rows) {
            result.put(row.productId(), toSummaryView(row));
        }
        return result;
    }

    public int countPublishedByProduct(long productId) {
        return reviewMapper.countPublishedByProduct(productId);
    }

    private ReviewView toView(ReviewMapperRow row) {
        return new ReviewView(
                row.reviewId(),
                row.memberId(),
                row.productId(),
                row.rating(),
                row.title(),
                row.body(),
                row.isPublished(),
                row.isBlocked(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private ReviewSummaryView toSummaryView(ReviewSummaryMapperRow row) {
        BigDecimal avg = row.averageRating() == null
                ? BigDecimal.ZERO
                : row.averageRating().setScale(1, RoundingMode.HALF_UP);
        return new ReviewSummaryView(row.productId(), row.reviewCount(), avg);
    }
}
