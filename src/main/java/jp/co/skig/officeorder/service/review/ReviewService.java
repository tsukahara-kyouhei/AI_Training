package jp.co.skig.officeorder.service.review;

import jp.co.skig.officeorder.model.review.ProductReviewPage;
import jp.co.skig.officeorder.model.review.ProductReviewView;
import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    // コンストラクタで2つのRepositoryを読み込む
    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    // ①商品のレビュー集計を取得する
    public ReviewSummaryView getProductReviewSummary(String productId) {
        return reviewRepository.findProductReviewSummary(productId);
    }

    // ②商品のレビュー一覧とページネーション情報を取得する
    public ProductReviewPage getProductReviews(String productId, int limit, int offset) {
        List<ProductReviewView> reviews = reviewRepository.findProductReviews(productId, limit, offset);
        Long totalCount = reviewRepository.countProductReviews(productId);

        // 【修正ポイント1】空のインスタンスを作ってから Setter で値を入れる
        ProductReviewPage page = new ProductReviewPage();
        page.setItems(reviews);
        page.setTotalCount(totalCount);
        return page;
    }

    // ③特定の会員が書いた商品レビューを取得する
    public Optional<ProductReviewView> getMemberReview(Long memberId, String productId) {
        return reviewRepository.findMemberProductReview(memberId, productId);
    }

    // ④会員がその商品を購入したことがあるか判定する
    public boolean hasPurchasedProduct(Long memberId, String productId) {
        // 【修正ポイント2】正しいメソッド名 (hasPurchasedProduct) を呼び出す
        return orderRepository.hasPurchasedProduct(memberId, productId);
    }

    // ⑤レビューを新規登録または更新する (ビジネスロジック)
    public void saveOrUpdateReview(Long memberId, ReviewForm form) {
        // 既存のレビューがあるかチェック
        Optional<ProductReviewView> existingReview = getMemberReview(memberId, form.getProductId());

        if (existingReview.isPresent()) {
            // すでに存在すれば「更新」
            reviewRepository.updateReview(memberId, form);
        } else {
            // 存在しなければ「新規登録」
            reviewRepository.insertReview(memberId, form);
        }
    }

}