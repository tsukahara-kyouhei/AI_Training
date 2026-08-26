package jp.co.skig.officeorder.service.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.CouponForm;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import jp.co.skig.officeorder.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

        @Mock
        private CartCookieStore cartCookieStore;

        @Mock
        private CartRepository cartRepository;

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private MessageSource messageSource;

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        private CartService service;

        @BeforeEach
        void setUp() {
                service = new CartService(
                                cartCookieStore,
                                cartRepository,
                                orderRepository,
                                messageSource);
        }

        @Test
        void getCart_正常系_通常商品を取得できる() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 2, false);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                false,
                                BigDecimal.ZERO);

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.items())
                                .hasSize(1);

                assertThat(result.items().get(0).productVariantId())
                                .isEqualTo(1L);

                assertThat(result.items().get(0).quantity())
                                .isEqualTo(2);

                assertThat(result.summary().productSubtotal())
                                .isEqualByComparingTo(BigDecimal.valueOf(20000));

                assertThat(result.summary().assemblyFeeTotal())
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void getCart_正常系_組立希望の商品は組立費を計算する() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 2, true);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                true,
                                BigDecimal.valueOf(3000));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.summary().productSubtotal())
                                .isEqualByComparingTo(BigDecimal.valueOf(20000));

                assertThat(result.summary().assemblyFeeTotal())
                                .isEqualByComparingTo(BigDecimal.valueOf(6000));

                assertThat(result.items().get(0).assemblyRequested())
                                .isTrue();
        }

        @Test
        void getCart_正常系_組立対象外の商品には組立費を加算しない() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 2, true);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                false,
                                BigDecimal.valueOf(3000));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.summary().assemblyFeeTotal())
                                .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(result.items().get(0).assemblyRequested())
                                .isFalse();
        }

        @Test
        void getCart_正常系_商品代金と組立費は税込みとして扱う() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 1, true);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                true,
                                BigDecimal.valueOf(3000));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.summary().taxAmount())
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void getCart_正常系_税込5000円以上なら送料無料() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 1, false);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(5000),
                                10,
                                false,
                                BigDecimal.ZERO);

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.summary().taxAmount())
                                .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(result.summary().shippingFee())
                                .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(result.summary().totalAmount())
                                .isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @Test
        void getCart_異常系_存在しない商品は表示対象から除外する() {

                CartCookieItem cookieItem = new CartCookieItem(999L, 2, false);

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of());

                CartView result = service.getCart(request, response);

                assertThat(result.items())
                                .isEmpty();

                assertThat(result.totalQuantity())
                                .isZero();

                assertThat(result.summary().productSubtotal())
                                .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(result.summary().assemblyFeeTotal())
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void getCart_正常系_数量0は1に補正される() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 0, false);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                false,
                                BigDecimal.ZERO);

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.items().get(0).quantity())
                                .isEqualTo(1);

                assertThat(result.totalQuantity())
                                .isEqualTo(1);

                assertThat(result.summary().productSubtotal())
                                .isEqualByComparingTo(BigDecimal.valueOf(10000));
        }

        @Test
        void getCart_正常系_数量100は99に補正される() {

                CartCookieItem cookieItem = new CartCookieItem(1L, 100, false);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(100),
                                100,
                                false,
                                BigDecimal.ZERO);

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                CartView result = service.getCart(request, response);

                assertThat(result.items().get(0).quantity())
                                .isEqualTo(99);

                assertThat(result.totalQuantity())
                                .isEqualTo(99);

                assertThat(result.summary().productSubtotal())
                                .isEqualByComparingTo(BigDecimal.valueOf(9900));
        }

        @Test
        void countTotalQuantity_正常系_カート内の数量を合計する() {

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(
                                                new CartCookieItem(1L, 2, false),
                                                new CartCookieItem(2L, 3, false)));

                int result = service.countTotalQuantity(request);

                assertThat(result)
                                .isEqualTo(5);

                verify(cartCookieStore)
                                .load(request);
        }

        @Test
        void countTotalQuantity_正常系_数量0は1として合計する() {

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(
                                                new CartCookieItem(1L, 0, false),
                                                new CartCookieItem(2L, 2, false)));

                int result = service.countTotalQuantity(request);

                assertThat(result)
                                .isEqualTo(3);
        }

        @Test
        void countTotalQuantity_正常系_数量100は99として合計する() {

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(
                                                new CartCookieItem(1L, 100, false),
                                                new CartCookieItem(2L, 2, false)));

                int result = service.countTotalQuantity(request);

                assertThat(result)
                                .isEqualTo(101);
        }

        @Test
        void addItem_正常系_新しい商品をカートに追加できる() {

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                true,
                                BigDecimal.valueOf(3000));

                when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                                .thenReturn(Map.of(1L, snapshot));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of());

                service.addItem(
                                request,
                                response,
                                1L,
                                2,
                                true);

                verify(cartCookieStore)
                                .save(
                                                request,
                                                response,
                                                List.of(new CartCookieItem(1L, 2, true)));
        }

        @Test
        void addItem_正常系_同じ商品なら数量を加算する() {

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                true,
                                BigDecimal.valueOf(3000));

                when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                                .thenReturn(Map.of(1L, snapshot));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(
                                                new CartCookieItem(1L, 2, false)));

                service.addItem(
                                request,
                                response,
                                1L,
                                3,
                                true);

                verify(cartCookieStore)
                                .save(
                                                request,
                                                response,
                                                List.of(new CartCookieItem(1L, 5, true)));
        }

        @Test
        void addItem_正常系_数量加算後に99を超える場合は99に制限する() {

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(10000),
                                10,
                                false,
                                BigDecimal.ZERO);

                when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                                .thenReturn(Map.of(1L, snapshot));

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(
                                                new CartCookieItem(1L, 90, false)));

                service.addItem(
                                request,
                                response,
                                1L,
                                20,
                                false);

                verify(cartCookieStore)
                                .save(
                                                request,
                                                response,
                                                List.of(new CartCookieItem(1L, 99, null)));
        }

        @Test
        void calculateDiscountAmount_正常系_定額クーポンを適用できる() {
                CouponForm coupon = new CouponForm(
                                1L,
                                "OFF1000",
                                "fixed",
                                BigDecimal.valueOf(1000),
                                null,
                                null,
                                null,
                                null,
                                null,
                                true);

                when(orderRepository.findActiveCouponByCode("OFF1000"))
                                .thenReturn(Optional.of(coupon));

                BigDecimal result = service.calculateDiscountAmount(
                                "OFF1000",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @Test
        void calculateDiscountAmount_正常系_定率クーポンを適用できる() {
                CouponForm coupon = new CouponForm(
                                2L,
                                "OFF10",
                                "percentage",
                                BigDecimal.valueOf(10),
                                null,
                                null,
                                null,
                                null,
                                null,
                                true);

                when(orderRepository.findActiveCouponByCode("OFF10"))
                                .thenReturn(Optional.of(coupon));

                BigDecimal result = service.calculateDiscountAmount(
                                "OFF10",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        void calculateDiscountAmount_異常系_クーポンコードが空なら0円() {
                BigDecimal result = service.calculateDiscountAmount(
                                "",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.ZERO);

                verifyNoInteractions(orderRepository);
        }

        @Test
        void calculateDiscountAmount_異常系_小計が0円なら0円() {
                BigDecimal result = service.calculateDiscountAmount(
                                "OFF1000",
                                BigDecimal.ZERO);

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.ZERO);

                verifyNoInteractions(orderRepository);
        }

        @Test
        void calculateDiscountAmount_異常系_存在しないクーポンなら0円() {
                when(orderRepository.findActiveCouponByCode("INVALID"))
                                .thenReturn(Optional.empty());

                BigDecimal result = service.calculateDiscountAmount(
                                "INVALID",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateDiscountAmount_異常系_最低購入金額未達なら0円() {
                CouponForm coupon = new CouponForm(
                                3L,
                                "OFF1000",
                                "fixed",
                                BigDecimal.valueOf(1000),
                                BigDecimal.valueOf(10000),
                                null,
                                null,
                                null,
                                null,
                                true);

                when(orderRepository.findActiveCouponByCode("OFF1000"))
                                .thenReturn(Optional.of(coupon));

                BigDecimal result = service.calculateDiscountAmount(
                                "OFF1000",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateDiscountAmount_正常系_定額割引が小計を超える場合は小計まで() {
                CouponForm coupon = new CouponForm(
                                4L,
                                "OFF1000",
                                "fixed",
                                BigDecimal.valueOf(1000),
                                null,
                                null,
                                null,
                                null,
                                null,
                                true);

                when(orderRepository.findActiveCouponByCode("OFF1000"))
                                .thenReturn(Optional.of(coupon));

                BigDecimal result = service.calculateDiscountAmount(
                                "OFF1000",
                                BigDecimal.valueOf(500));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        void calculateDiscountAmount_正常系_クーポンコード前後の空白を除去する() {
                CouponForm coupon = new CouponForm(
                                5L,
                                "OFF1000",
                                "fixed",
                                BigDecimal.valueOf(1000),
                                null,
                                null,
                                null,
                                null,
                                null,
                                true);

                when(orderRepository.findActiveCouponByCode("OFF1000"))
                                .thenReturn(Optional.of(coupon));

                BigDecimal result = service.calculateDiscountAmount(
                                "  OFF1000  ",
                                BigDecimal.valueOf(5000));

                assertThat(result)
                                .isEqualByComparingTo(BigDecimal.valueOf(1000));

                verify(orderRepository)
                                .findActiveCouponByCode("OFF1000");
        }

        @Test
        void getCart_正常系_Cookieにクーポンがあれば割引額を反映する() {
                CartCookieItem cookieItem = new CartCookieItem(1L, 1, false);

                CartProductSnapshot snapshot = new CartProductSnapshot(
                                1L,
                                10L,
                                "ワークデスク",
                                "P0001-C01",
                                "ホワイト",
                                BigDecimal.valueOf(5000),
                                10,
                                false,
                                BigDecimal.ZERO);

                CouponForm coupon = new CouponForm(
                                1L,
                                "OFF1000",
                                "fixed",
                                BigDecimal.valueOf(1000),
                                null,
                                null,
                                null,
                                null,
                                null,
                                true);

                Cookie couponCookie = new Cookie("APPLIED_COUPON", "OFF1000");

                when(cartCookieStore.load(request))
                                .thenReturn(List.of(cookieItem));

                when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                                .thenReturn(Map.of(1L, snapshot));

                when(request.getCookies())
                                .thenReturn(new Cookie[] { couponCookie });

                when(orderRepository.findActiveCouponByCode("OFF1000"))
                                .thenReturn(Optional.of(coupon));

                CartView result = service.getCart(request, response);

                assertThat(result.summary().discountAmount())
                                .isEqualByComparingTo(BigDecimal.valueOf(1000));
        }
}