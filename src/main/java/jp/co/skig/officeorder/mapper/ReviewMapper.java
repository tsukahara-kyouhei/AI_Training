package jp.co.skig.officeorder.mapper;

import java.util.List;

import jp.co.skig.officeorder.mapper.row.ReviewMapperRow;
import jp.co.skig.officeorder.mapper.row.ReviewSummaryMapperRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品レビューの永続化を扱う Mapper。
 */
@Mapper
public interface ReviewMapper {

    ReviewSummaryMapperRow selectReviewSummary(@Param("productId") long productId);

    List<ReviewMapperRow> selectReviewsByProduct(@Param("productId") long productId,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    ReviewMapperRow selectByMemberAndProduct(@Param("memberId") long memberId,
                                              @Param("productId") long productId);

    ReviewMapperRow selectById(@Param("reviewId") long reviewId);

    Boolean existsPurchase(@Param("memberId") long memberId,
                           @Param("productId") long productId);

    int insertReview(@Param("memberId") long memberId,
                     @Param("productId") long productId,
                     @Param("rating") int rating,
                     @Param("title") String title,
                     @Param("body") String body,
                     @Param("isPublished") boolean isPublished);

    int updateReview(@Param("reviewId") long reviewId,
                     @Param("rating") int rating,
                     @Param("title") String title,
                     @Param("body") String body,
                     @Param("isPublished") boolean isPublished);

    int deleteReview(@Param("reviewId") long reviewId);

    List<ReviewSummaryMapperRow> selectReviewSummaries(@Param("productIds") List<Long> productIds);

    int countPublishedByProduct(@Param("productId") long productId);
}
