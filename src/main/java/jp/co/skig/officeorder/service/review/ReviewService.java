package jp.co.skig.officeorder.service.review;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.co.skig.officeorder.mapper.ReviewMapper;
import jp.co.skig.officeorder.mapper.row.ProductReviewMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductReviewSummaryMapperRow;
import jp.co.skig.officeorder.model.product.ProductReviewForm;
import jp.co.skig.officeorder.model.product.ProductReviewView;
import jp.co.skig.officeorder.repository.ReviewRepository;

@Service
public class ReviewService {

    /** レビュー永続化の窓口。 */
    private final ReviewRepository reviewRepository;

    /**
     * レビューサービスを生成する。
     *
     * @param reviewRepository
     */
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    /**
     * 会員自身のレビューを取得する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return レビュー。存在しない場合はempty
     */
    public java.util.Optional<ProductReviewView> findMemberReview(
            long memberId,
            long productId) {

        return reviewRepository.findMemberReview(memberId, productId);
    }

    /**
     * 会員が対象商品を購入したことがあるか確認する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return 購入履歴があればtrue
     */
    public boolean existsPurchasedProduct(long memberId, long productId) {
        return reviewRepository.existsPurchasedProduct(memberId, productId);
    }

    /**
     * レビューを新規登録する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @param form      レビュー入力
     */
    @Transactional
    public void createReview(
            long memberId,
            long productId,
            ProductReviewForm form) {

        validateForm(form);

        // 購入履歴がなければレビュー投稿不可
        if (!reviewRepository.existsPurchasedProduct(memberId, productId)) {
            throw new IllegalArgumentException(
                    "購入履歴のある商品だけレビューを投稿できます。");
        }

        // 1会員1商品につき1レビュー
        if (reviewRepository.findMemberReview(memberId, productId).isPresent()) {
            throw new IllegalStateException(
                    "この商品にはすでにレビューが登録されています。");
        }

        reviewRepository.insertReview(
                memberId,
                productId,
                form.getRating(),
                normalizeTitle(form.getTitle()),
                form.getBody().trim());
    }

    /**
     * 会員自身のレビューを更新する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @param form      レビュー入力
     */
    @Transactional
    public void updateReview(
            long memberId,
            long productId,
            ProductReviewForm form) {

        validateForm(form);

        // 自分のレビューが存在することを確認
        if (reviewRepository.findMemberReview(memberId, productId).isEmpty()) {
            throw new IllegalArgumentException(
                    "更新対象のレビューが見つかりません。");
        }

        reviewRepository.updateReview(
                memberId,
                productId,
                form.getRating(),
                normalizeTitle(form.getTitle()),
                form.getBody().trim());
    }

    /**
     * レビュー入力値を検証する。
     *
     * @param form レビュー入力
     */
    private void validateForm(ProductReviewForm form) {

        if (form == null) {
            throw new IllegalArgumentException("レビュー入力がありません。");
        }

        if (form.getRating() == null
                || form.getRating() < 1
                || form.getRating() > 5) {
            throw new IllegalArgumentException(
                    "評価は1～5の範囲で指定してください。");
        }

        if (!StringUtils.hasText(form.getBody())) {
            throw new IllegalArgumentException(
                    "レビュー本文は必須です。");
        }
    }

    /**
     * タイトルを正規化する。
     *
     * @param title タイトル
     * @return 前後空白を除去したタイトル。空ならnull
     */
    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.trim();
    }
}
