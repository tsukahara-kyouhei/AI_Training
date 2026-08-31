package jp.co.skig.officeorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.skig.officeorder.mapper.row.ProductReviewMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductReviewSummaryMapperRow;

@Mapper
public interface ReviewMapper {

    /**
     * 商品のレビュー平均評価と件数を取得する。
     *
     * @param productId 商品ID
     * @return レビュー集計
     */
    ProductReviewSummaryMapperRow selectReviewSummary(
            @Param("productId") long productId);

    /**
     * 商品の公開レビューを最新順で取得する。
     *
     * @param productId 商品ID
     * @return レビュー一覧
     */
    List<ProductReviewMapperRow> selectProductReviews(
            @Param("productId") long productId);

    /**
     * 会員自身のレビューを取得する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return レビュー。存在しない場合はnull
     */
    ProductReviewMapperRow selectMemberReview(
            @Param("memberId") long memberId,
            @Param("productId") long productId);

    /**
     * 会員が対象商品を購入したことがあるか確認する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return 購入履歴があればtrue
     */
    boolean existsPurchasedProduct(
            @Param("memberId") long memberId,
            @Param("productId") long productId);

    /**
     * レビューを新規登録する。
     */
    int insertReview(
            @Param("memberId") long memberId,
            @Param("productId") long productId,
            @Param("rating") int rating,
            @Param("title") String title,
            @Param("body") String body);

    /**
     * 会員自身のレビューを更新する。
     */
    int updateReview(
            @Param("memberId") long memberId,
            @Param("productId") long productId,
            @Param("rating") int rating,
            @Param("title") String title,
            @Param("body") String body);
}