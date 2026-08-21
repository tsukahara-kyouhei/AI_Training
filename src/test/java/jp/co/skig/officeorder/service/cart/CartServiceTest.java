package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartCookieStore cartCookieStore;
    private CartRepository cartRepository;
    private MessageSource messageSource;
    private MemberSessionService memberSessionService;
    private CartService service;
    private jp.co.skig.officeorder.service.coupon.CouponService couponService;

    @BeforeEach
    void setUp() {
        cartCookieStore = Mockito.mock(CartCookieStore.class);
        cartRepository = Mockito.mock(CartRepository.class);
        messageSource = Mockito.mock(MessageSource.class);
        memberSessionService = Mockito.mock(MemberSessionService.class);
        couponService = Mockito.mock(jp.co.skig.officeorder.service.coupon.CouponService.class);

        when(messageSource.getMessage(any(String.class), any(Object[].class), any())).thenReturn("message");

        service = new CartService(cartCookieStore, cartRepository, messageSource, couponService, memberSessionService);
    }

    @Test
    void countTotalQuantity_normalizesNegativeQuantity() {
        when(cartCookieStore.load(any(HttpServletRequest.class))).thenReturn(List.of(
                new CartCookieItem(1L, -1, false),
                new CartCookieItem(2L, 2, true)));

        int total = service.countTotalQuantity(new MockHttpServletRequest());

        assertEquals(3, total);
    }

    @Test
    void addItem_mergesExistingItemAndNormalizesAssembly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, true)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, new CartProductSnapshot(1L, 10L, "Desk", "D001", "Black", BigDecimal.valueOf(1000), 10, false,
                        BigDecimal.valueOf(500))));

        service.addItem(request, response, 1L, 2, true);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        List<CartCookieItem> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(3, saved.get(0).quantity());
        assertNull(saved.get(0).assemblyRequested());
    }

    @Test
    void updateItem_noExistingItem_doesNotSave() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());

        service.updateItem(request, response, 1L, 5, true);

        verify(cartCookieStore, never()).save(any(HttpServletRequest.class), any(HttpServletResponse.class), any());
    }

    @Test
    void getCart_normalizesRawItemsAndCalculatesSummary() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 0, true)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, new CartProductSnapshot(1L, 10L, "Desk", "D001", "Black", BigDecimal.valueOf(1000), 10, true,
                        BigDecimal.valueOf(200))));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cartView = service.getCart(request, response);

        assertEquals(1, cartView.items().size());
        assertEquals(1, cartView.totalQuantity());
        CartSummaryView summary = cartView.summary();
        assertEquals(BigDecimal.valueOf(1000), summary.productSubtotal());
        assertEquals(BigDecimal.valueOf(200), summary.assemblyFeeTotal());
        assertEquals(BigDecimal.valueOf(120), summary.taxAmount());
        assertEquals(BigDecimal.valueOf(800), summary.shippingFee());
        assertEquals(BigDecimal.valueOf(2120), summary.totalAmount());
        verify(cartCookieStore).save(eq(request), eq(response), any());
    }

    @Test
    void getCart_appliesCouponForLoggedInMember() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpSession session = request.getSession(true);
        session.setAttribute("appliedCouponCode", "SAVE10");
        when(memberSessionService.currentMember(session))
                .thenReturn(Optional.of(new MemberSessionUser(99L, "user@example.com", "山田", "太郎")));
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, false)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, new CartProductSnapshot(1L, 10L, "Desk", "D001", "Black", BigDecimal.valueOf(1000), 10, false,
                        BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
        when(couponService.validateAndCalculateDiscount(eq("SAVE10"), eq(99L), any(BigDecimal.class)))
                .thenReturn(BigDecimal.valueOf(100));

        CartView cartView = service.getCart(request, response);

        assertEquals("SAVE10", cartView.summary().appliedCouponCode());
        assertEquals(BigDecimal.valueOf(100), cartView.summary().couponDiscountAmount());
        assertEquals(BigDecimal.valueOf(1800), cartView.summary().totalAmount());
    }

    @Test
    void sanitizeRedirectPath_rejectsUnsafeValues() {
        assertNull(service.sanitizeRedirectPath(null));
        assertNull(service.sanitizeRedirectPath(""));
        assertNull(service.sanitizeRedirectPath("//unsafe"));
        assertNull(service.sanitizeRedirectPath("http://example.com"));
        assertEquals("/safe/path", service.sanitizeRedirectPath("/safe/path"));
    }
}
