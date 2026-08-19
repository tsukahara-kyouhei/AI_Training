package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.skig.officeorder.mapper.ProductReviewMapper;
import jp.co.skig.officeorder.model.product.ProductReview;
import jp.co.skig.officeorder.model.product.ProductReviewForm;
import jp.co.skig.officeorder.model.product.ProductReviewStat;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewMapper productReviewMapper;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Nested
    @DisplayName("createReviewメソッドのテスト")
    class CreateReviewTest {

        @Test
        @DisplayName("購入済みかつ未レビューの場合、正常に登録されること")
        void createReview_Success() {
            Long memberId = 1L;
            ProductReviewForm form = new ProductReviewForm(100L, 5, "最高", "とても良かった");

            when(productReviewMapper.existsPurchasedOrder(memberId, 100L)).thenReturn(true);
            when(productReviewMapper.findByMemberIdAndProductId(memberId, 100L)).thenReturn(Optional.empty());

            productReviewService.createReview(memberId, form);

            verify(productReviewMapper).insert(any(ProductReview.class));
        }

        @Test
        @DisplayName("未購入の場合、IllegalStateExceptionが発生すること")
        void createReview_NotPurchased_ThrowsException() {
            Long memberId = 1L;
            ProductReviewForm form = new ProductReviewForm(100L, 5, "最高", "とても良かった");

            when(productReviewMapper.existsPurchasedOrder(memberId, 100L)).thenReturn(false);

            assertThatThrownBy(() -> productReviewService.createReview(memberId, form))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("購入履歴がない商品はレビューできません。");
        }

        @Test
        @DisplayName("既にレビュー済みの場合、IllegalStateExceptionが発生すること")
        void createReview_AlreadyReviewed_ThrowsException() {
            Long memberId = 1L;
            ProductReviewForm form = new ProductReviewForm(100L, 5, "最高", "とても良かった");
            ProductReview existingReview = new ProductReview(1L, memberId, 100L, 5, "最高", "とても良かった", true, null, null);

            when(productReviewMapper.existsPurchasedOrder(memberId, 100L)).thenReturn(true);
            when(productReviewMapper.findByMemberIdAndProductId(memberId, 100L)).thenReturn(Optional.of(existingReview));

            assertThatThrownBy(() -> productReviewService.createReview(memberId, form))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("この商品には既にレビューを投稿済みです。");
        }
    }

    @Nested
    @DisplayName("getReviewsByProductIdメソッドのテスト")
    class GetReviewsByProductIdTest {

        @Test
        @DisplayName("商品IDに紐づくレビュー一覧を取得できること")
        void getReviewsByProductId_Success() {
            Long productId = 100L;
            List<ProductReview> mockReviews = List.of(
                    new ProductReview(1L, 1L, productId, 5, "良い", "良い", true, null, null)
            );
            when(productReviewMapper.findByProductId(productId)).thenReturn(mockReviews);

            List<ProductReview> result = productReviewService.getReviewsByProductId(productId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(productId);
        }
    }

    @Nested
    @DisplayName("getReviewStatByProductIdメソッドのテスト")
    class GetReviewStatByProductIdTest {

        @Test
        @DisplayName("商品IDに紐づく集計情報を取得できること")
        void getReviewStatByProductId_Success() {
            Long productId = 100L;
            ProductReviewStat mockStat = new ProductReviewStat(productId, 4.5, 10L);
            when(productReviewMapper.findStatByProductId(productId)).thenReturn(mockStat);

            ProductReviewStat result = productReviewService.getReviewStatByProductId(productId);

            assertThat(result.averageRating()).isEqualTo(4.5);
            assertThat(result.reviewCount()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("canWriteReviewメソッドのテスト")
    class CanWriteReviewTest {

        @Test
        @DisplayName("memberIdがnullの場合、falseを返すこと")
        void canWriteReview_NullMember_ReturnsFalse() {
            boolean result = productReviewService.canWriteReview(null, 100L);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("購入済みかつ未レビューの場合、trueを返すこと")
        void canWriteReview_PurchasedAndNotReviewed_ReturnsTrue() {
            Long memberId = 1L;
            Long productId = 100L;
            when(productReviewMapper.existsPurchasedOrder(memberId, productId)).thenReturn(true);
            when(productReviewMapper.findByMemberIdAndProductId(memberId, productId)).thenReturn(Optional.empty());

            boolean result = productReviewService.canWriteReview(memberId, productId);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("未購入の場合、falseを返すこと")
        void canWriteReview_NotPurchased_ReturnsFalse() {
            Long memberId = 1L;
            Long productId = 100L;
            when(productReviewMapper.existsPurchasedOrder(memberId, productId)).thenReturn(false);

            boolean result = productReviewService.canWriteReview(memberId, productId);
            assertThat(result).isFalse();
        }
    }
}
