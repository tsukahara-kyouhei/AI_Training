package jp.co.skig.officeorder.service.review;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jp.co.skig.officeorder.model.product.ProductReviewForm;
import jp.co.skig.officeorder.model.product.ProductReviewView;
import jp.co.skig.officeorder.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

        @Mock
        private ReviewRepository reviewRepository;

        private ReviewService reviewService;

        @BeforeEach
        void setUp() {
                reviewService = new ReviewService(reviewRepository);
        }

        @Test
        void findMemberReview_レビューが存在する場合_レビューを返す() {
                ProductReviewView review = new ProductReviewView(
                                1L,
                                10L,
                                5,
                                "とても良い商品",
                                "使いやすくて満足しています。",
                                OffsetDateTime.now());

                when(reviewRepository.findMemberReview(10L, 100L))
                                .thenReturn(Optional.of(review));

                Optional<ProductReviewView> result = reviewService.findMemberReview(10L, 100L);

                assertTrue(result.isPresent());
                assertEquals(review, result.get());

                verify(reviewRepository).findMemberReview(10L, 100L);
        }

        @Test
        void findMemberReview_レビューが存在しない場合_emptyを返す() {
                when(reviewRepository.findMemberReview(10L, 100L))
                                .thenReturn(Optional.empty());

                Optional<ProductReviewView> result = reviewService.findMemberReview(10L, 100L);

                assertTrue(result.isEmpty());

                verify(reviewRepository).findMemberReview(10L, 100L);
        }

        @Test
        void existsPurchasedProduct_購入履歴がある場合_trueを返す() {
                when(reviewRepository.existsPurchasedProduct(10L, 100L))
                                .thenReturn(true);

                boolean result = reviewService.existsPurchasedProduct(10L, 100L);

                assertTrue(result);

                verify(reviewRepository).existsPurchasedProduct(10L, 100L);
        }

        @Test
        void existsPurchasedProduct_購入履歴がない場合_falseを返す() {
                when(reviewRepository.existsPurchasedProduct(10L, 100L))
                                .thenReturn(false);

                boolean result = reviewService.existsPurchasedProduct(10L, 100L);

                assertFalse(result);

                verify(reviewRepository).existsPurchasedProduct(10L, 100L);
        }

        @Test
        void createReview_正常系_レビューを登録する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(5);
                form.setTitle("  とても良い商品  ");
                form.setBody("  使いやすくて満足しています。  ");

                when(reviewRepository.existsPurchasedProduct(10L, 100L))
                                .thenReturn(true);

                reviewService.createReview(10L, 100L, form);

                verify(reviewRepository).existsPurchasedProduct(10L, 100L);
                verify(reviewRepository).findMemberReview(10L, 100L);
                verify(reviewRepository).insertReview(
                                10L,
                                100L,
                                5,
                                "とても良い商品",
                                "使いやすくて満足しています。");
        }

        @Test
        void createReview_異常系_入力がnullなら例外を送出する() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.createReview(10L, 100L, null));

                assertEquals("レビュー入力がありません。", exception.getMessage());

                verifyNoInteractions(reviewRepository);
        }

        @Test
        void createReview_異常系_評価が範囲外なら例外を送出する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(6);
                form.setTitle("タイトル");
                form.setBody("本文");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.createReview(10L, 100L, form));

                assertEquals(
                                "評価は1～5の範囲で指定してください。",
                                exception.getMessage());

                verifyNoInteractions(reviewRepository);
        }

        @Test
        void createReview_異常系_本文が空なら例外を送出する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(5);
                form.setTitle("タイトル");
                form.setBody("   ");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.createReview(10L, 100L, form));

                assertEquals(
                                "レビュー本文は必須です。",
                                exception.getMessage());

                verifyNoInteractions(reviewRepository);
        }

        @Test
        void createReview_異常系_未購入なら例外を送出する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(5);
                form.setTitle("タイトル");
                form.setBody("本文");

                when(reviewRepository.existsPurchasedProduct(10L, 100L))
                                .thenReturn(false);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.createReview(10L, 100L, form));

                assertEquals(
                                "購入履歴のある商品だけレビューを投稿できます。",
                                exception.getMessage());

                verify(reviewRepository).existsPurchasedProduct(10L, 100L);
                verify(reviewRepository, never())
                                .findMemberReview(anyLong(), anyLong());
                verify(reviewRepository, never())
                                .insertReview(
                                                anyLong(),
                                                anyLong(),
                                                anyInt(),
                                                any(),
                                                anyString());
        }

        @Test
        void createReview_異常系_レビュー登録済みなら例外を送出する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(5);
                form.setTitle("タイトル");
                form.setBody("本文");

                ProductReviewView existingReview = new ProductReviewView(
                                1L,
                                10L,
                                5,
                                "既存レビュー",
                                "本文",
                                OffsetDateTime.now());

                when(reviewRepository.existsPurchasedProduct(10L, 100L))
                                .thenReturn(true);

                when(reviewRepository.findMemberReview(10L, 100L))
                                .thenReturn(Optional.of(existingReview));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> reviewService.createReview(10L, 100L, form));

                assertEquals(
                                "この商品にはすでにレビューが登録されています。",
                                exception.getMessage());

                verify(reviewRepository).existsPurchasedProduct(10L, 100L);
                verify(reviewRepository).findMemberReview(10L, 100L);
                verify(reviewRepository, never())
                                .insertReview(
                                                anyLong(),
                                                anyLong(),
                                                anyInt(),
                                                any(),
                                                anyString());
        }

        @Test
        void updateReview_正常系_レビューを更新する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(4);
                form.setTitle("  更新タイトル  ");
                form.setBody("  更新した本文  ");

                ProductReviewView existingReview = new ProductReviewView(
                                1L,
                                10L,
                                5,
                                "既存タイトル",
                                "本文",
                                OffsetDateTime.now());

                when(reviewRepository.findMemberReview(10L, 100L))
                                .thenReturn(Optional.of(existingReview));

                reviewService.updateReview(10L, 100L, form);

                verify(reviewRepository).findMemberReview(10L, 100L);
                verify(reviewRepository).updateReview(
                                10L,
                                100L,
                                4,
                                "更新タイトル",
                                "更新した本文");
        }

        @Test
        void updateReview_異常系_レビューが存在しないなら例外を送出する() {
                ProductReviewForm form = new ProductReviewForm();
                form.setRating(4);
                form.setTitle("タイトル");
                form.setBody("本文");

                when(reviewRepository.findMemberReview(10L, 100L))
                                .thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.updateReview(10L, 100L, form));

                assertEquals(
                                "更新対象のレビューが見つかりません。",
                                exception.getMessage());

                verify(reviewRepository).findMemberReview(10L, 100L);
                verify(reviewRepository, never())
                                .updateReview(
                                                anyLong(),
                                                anyLong(),
                                                anyInt(),
                                                any(),
                                                anyString());
        }
}