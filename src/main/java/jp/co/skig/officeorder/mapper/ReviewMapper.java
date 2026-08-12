package jp.co.skig.officeorder.mapper;

import jp.co.skig.officeorder.model.review.ProductReviewView;
import jp.co.skig.officeorder.model.review.ReviewForm;
import jp.co.skig.officeorder.model.review.ReviewSummaryView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ReviewMapper {

    // ①商品のレビュー一覧を取得する (productId, limit, offset)
    List<ProductReviewView> selectProductReviews(@Param("productId") Long productId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    // ②商品のレビュー件数を取得する (productId)
    // 戻り値は Long
    Long countProductReviews(@Param("productId") Long productId);

    // ③商品のレビュー集計（平均評価と件数）を取得する (productId)
    // 戻り値は ReviewSummaryView
    ReviewSummaryView selectProductReviewSummary(@Param("productId") Long productId);

    // ④特定の会員が書いた商品レビューを取得する (memberId, productId)
    // 戻り値は Optional<ProductReviewView>
    Optional<ProductReviewView> selectMemberProductReview(@Param("memberId") Long memberId,
            @Param("productId") Long productId);

    // ⑤レビューを新規登録する (memberId, form)
    // 戻り値は int (更新件数)
    int insertReview(@Param("memberId") Long memberId, @Param("form") ReviewForm form);

    // ⑥レビューを更新する (memberId, form)
    // 戻り値は int (更新件数)
    int updateReview(@Param("memberId") Long memberId, @Param("form") ReviewForm form);

}