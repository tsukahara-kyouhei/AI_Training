package jp.co.skig.officeorder.repository;

import jp.co.skig.officeorder.mapper.ReviewMapper;
import jp.co.skig.officeorder.model.review.ProductReviewView;
import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepository {

    private final ReviewMapper reviewMapper;

    // Lombokを使わないためのコンストラクタ（初期化処理）
    public ReviewRepository(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    // ①商品のレビュー一覧を取得する
    public List<ProductReviewView> findProductReviews(String productId, int limit, int offset) {
        return reviewMapper.selectProductReviews(productId, limit, offset);
    }

    // ②商品のレビュー件数を取得する
    public Long countProductReviews(String productId) {
        return reviewMapper.countProductReviews(productId);
    }

    // ③商品のレビュー集計（平均評価と件数）を取得する
    public ReviewSummaryView findProductReviewSummary(String productId) {
        return reviewMapper.selectProductReviewSummary(productId);
    }

    // ④特定の会員が書いた商品レビューを取得する
    public Optional<ProductReviewView> findMemberProductReview(Long memberId, String productId) {
        return reviewMapper.selectMemberProductReview(memberId, productId);
    }

    // ⑤レビューを新規登録する
    public int insertReview(Long memberId, ReviewForm form) {
        return reviewMapper.insertReview(memberId, form);
    }

    // ⑥レビューを更新する
    public int updateReview(Long memberId, ReviewForm form) {
        return reviewMapper.updateReview(memberId, form);
    }

}