package jp.co.skig.officeorder.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponApiRequest;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.model.coupon.CouponValidationResult;
import jp.co.skig.officeorder.repository.CouponRepository;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.coupon.CouponSessionService;
import jp.co.skig.officeorder.service.coupon.CouponValidationService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CouponCartControllerTest {
    private CartService cartService;
    private CouponRepository couponRepository;
    private CouponValidationService couponValidationService;
    private CouponSessionService couponSessionService;
    private MemberSessionService memberSessionService;
    private CouponCartController controller;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private CartView cart;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        couponRepository = mock(CouponRepository.class);
        couponValidationService = mock(CouponValidationService.class);
        couponSessionService = mock(CouponSessionService.class);
        memberSessionService = mock(MemberSessionService.class);
        controller = new CouponCartController(cartService, couponRepository,
                couponValidationService, couponSessionService, memberSessionService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        cart = new CartView(
                List.of(new CartLineView(1L, 100L, "デスク", "P-001", "desk", "黒",
                        new BigDecimal("1000"), 5, false, BigDecimal.ZERO, false, 1, "/products/100")),
                1,
                1,
                new CartSummaryView(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("100"), new BigDecimal("1100"))
        );
        coupon = new Coupon(
                7L,
                "SAVE100",
                "100円割引",
                "fixed_amount",
                new BigDecimal("100"),
                null,
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2027-01-01T00:00:00Z"),
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                """,
                null,
                null,
                0L,
                true
        );
        when(memberSessionService.currentMember(session)).thenReturn(Optional.empty());
        when(couponSessionService.clearIfMemberChanged(session, null)).thenReturn(false);
        when(cartService.getCart(request, response)).thenReturn(cart);
    }

    @Test
    void applyReturnsCalculatedCartAndStoresCouponInSession() {
        when(couponRepository.findByCode("SAVE100")).thenReturn(Optional.of(coupon));
        when(couponSessionService.getCouponCode(session)).thenReturn(null);
        when(couponValidationService.validateCoupon(coupon, cart, null))
                .thenReturn(CouponValidationResult.valid(coupon, new BigDecimal("100")));
        when(cartService.recalculateWithCouponDiscount(cart, new BigDecimal("100"))).thenReturn(cart);

        var responseEntity = controller.apply(
                new CouponApiRequest(" SAVE100 "), request, response, session);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isTrue();
        assertThat(body.cart().couponDiscountAmount()).isEqualByComparingTo("100");
        assertThat(body.cart().discountedSubtotal()).isEqualByComparingTo("900");
        assertThat(body.cart().appliedCouponCode()).isEqualTo("SAVE100");
        assertThat(body.cart().appliedCouponName()).isEqualTo("100円割引");
        verify(couponSessionService).apply(session, "SAVE100", null);
        verify(cartService).recalculateWithCouponDiscount(cart, new BigDecimal("100"));
    }

    @Test
    void applyMissingCodeReturnsNotFoundWithoutRepositoryLookup() {
        var responseEntity = controller.apply(
                new CouponApiRequest("  "), request, response, session);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.errorCode()).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND.name());
        verify(couponRepository, never()).findByCode(any());
    }

    @Test
    void applyDifferentCouponReturnsConflictAndKeepsCurrentCoupon() {
        when(couponRepository.findByCode("SAVE100")).thenReturn(Optional.of(coupon));
        when(couponSessionService.getCouponCode(session)).thenReturn("EXISTING");

        var responseEntity = controller.apply(
                new CouponApiRequest("SAVE100"), request, response, session);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_APPLIED.name());
        verify(couponSessionService, never()).apply(session, "SAVE100", null);
        verify(cartService, never()).getCart(request, response);
    }

    @Test
    void removeClearsSessionAndReturnsUndiscountedCart() {
        var responseEntity = controller.remove(request, response, session);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isTrue();
        assertThat(body.cart().couponDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(body.cart().appliedCouponCode()).isNull();
        assertThat(body.cart().appliedCouponName()).isNull();
        verify(couponSessionService).clear(session);
        verify(cartService).getCart(request, response);
    }
}
