package jp.co.skig.officeorder.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import jp.co.skig.officeorder.mapper.ReviewMapper;
import jp.co.skig.officeorder.mapper.row.ProductReviewMapperRow;
import jp.co.skig.officeorder.model.product.ProductReviewView;

@Repository
public class ReviewRepository {
    /** レビューSQLを呼び出すMyBatis Mapper。 */
    private final ReviewMapper reviewMapper;

    /**
     * レビューリポジトリを生成する。
     *
     * @param reviewMapper レビューマッパー
     */
    public ReviewRepository(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    /**
     * 会員自身のレビューを取得する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return レビュー
     */
    public Optional<ProductReviewView> findMemberReview(
            long memberId,
            long productId) {

        ProductReviewMapperRow row = reviewMapper.selectMemberReview(memberId, productId);

        if (row == null) {
            return Optional.empty();
        }

        return Optional.of(new ProductReviewView(
                row.reviewId(),
                row.memberId(),
                row.rating(),
                row.title(),
                row.body(),
                row.createdAt()));
    }

    /**
     * 会員が対象商品を購入したことがあるか確認する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return 購入履歴があればtrue
     */
    public boolean existsPurchasedProduct(
            long memberId,
            long productId) {

        return reviewMapper.existsPurchasedProduct(memberId, productId);
    }

    /**
     * レビューを新規登録する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @param rating    評価
     * @param title     タイトル
     * @param body      本文
     */
    public void insertReview(
            long memberId,
            long productId,
            int rating,
            String title,
            String body) {

        reviewMapper.insertReview(
                memberId,
                productId,
                rating,
                title,
                body);
    }

    /**
     * 会員自身のレビューを更新する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @param rating    評価
     * @param title     タイトル
     * @param body      本文
     */
    public void updateReview(
            long memberId,
            long productId,
            int rating,
            String title,
            String body) {

        reviewMapper.updateReview(
                memberId,
                productId,
                rating,
                title,
                body);
    }
}
