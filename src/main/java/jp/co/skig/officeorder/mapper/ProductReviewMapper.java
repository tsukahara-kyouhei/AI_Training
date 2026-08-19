package jp.co.skig.officeorder.mapper;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import jp.co.skig.officeorder.model.product.ProductReview;
import jp.co.skig.officeorder.model.product.ProductReviewStat;

@Mapper
public interface ProductReviewMapper {

    void insert(ProductReview review);

    void update(ProductReview review);

    Optional<ProductReview> findById(@Param("id") Long id);

    Optional<ProductReview> findByMemberIdAndProductId(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId
    );

    List<ProductReview> findByProductId(@Param("productId") Long productId);

    ProductReviewStat findStatByProductId(@Param("productId") Long productId);

    boolean existsPurchasedOrder(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId
    );
}
