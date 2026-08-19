package jp.co.skig.officeorder.service.product;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jp.co.skig.officeorder.mapper.ProductReviewMapper;
import jp.co.skig.officeorder.model.product.ProductReview;
import jp.co.skig.officeorder.model.product.ProductReviewForm;
import jp.co.skig.officeorder.model.product.ProductReviewStat;

@Service
@Transactional(readOnly = true)
public class ProductReviewService {

    private final ProductReviewMapper productReviewMapper;

    public ProductReviewService(ProductReviewMapper productReviewMapper) {
        this.productReviewMapper = productReviewMapper;
    }

    /**
     * レビューを新規投稿する
     */
    @Transactional
    public void createReview(Long memberId, ProductReviewForm form) {
        // 購入履歴の検証
        if (!productReviewMapper.existsPurchasedOrder(memberId, form.productId())) {
            throw new IllegalStateException("購入履歴がない商品はレビューできません。");
        }

        // 重複投稿の検証
        if (productReviewMapper.findByMemberIdAndProductId(memberId, form.productId()).isPresent()) {
            throw new IllegalStateException("この商品には既にレビューを投稿済みです。");
        }

        ProductReview review = new ProductReview(
                null,
                memberId,
                form.productId(),
                form.rating(),
                form.title(),
                form.content(),
                true,
                null,
                null
        );

        productReviewMapper.insert(review);
    }

    /**
     * 商品のレビュー一覧を取得する
     */
    public List<ProductReview> getReviewsByProductId(Long productId) {
        return productReviewMapper.findByProductId(productId);
    }

    /**
     * 商品のレビュー集計情報を取得する
     */
    public ProductReviewStat getReviewStatByProductId(Long productId) {
        return productReviewMapper.findStatByProductId(productId);
    }

    /**
     * 会員が指定商品のレビューを投稿可能か判定する
     */
    public boolean canWriteReview(Long memberId, Long productId) {
        if (memberId == null) {
            return false;
        }
        boolean hasPurchased = productReviewMapper.existsPurchasedOrder(memberId, productId);
        boolean hasReviewed = productReviewMapper.findByMemberIdAndProductId(memberId, productId).isPresent();
        return hasPurchased && !hasReviewed;
    }
}
