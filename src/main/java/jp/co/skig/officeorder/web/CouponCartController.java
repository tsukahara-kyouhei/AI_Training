package jp.co.skig.officeorder.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponApiRequest;
import jp.co.skig.officeorder.model.coupon.CouponApiResponse;
import jp.co.skig.officeorder.model.coupon.CouponCartSummaryResponse;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.model.coupon.CouponValidationResult;
import jp.co.skig.officeorder.repository.CouponRepository;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.coupon.CouponSessionService;
import jp.co.skig.officeorder.service.coupon.CouponValidationException;
import jp.co.skig.officeorder.service.coupon.CouponValidationService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart/coupon")
public class CouponCartController {
    private final CartService cartService;
    private final CouponRepository couponRepository;
    private final CouponValidationService couponValidationService;
    private final CouponSessionService couponSessionService;
    private final MemberSessionService memberSessionService;

    public CouponCartController(CartService cartService,
                                CouponRepository couponRepository,
                                CouponValidationService couponValidationService,
                                CouponSessionService couponSessionService,
                                MemberSessionService memberSessionService) {
        this.cartService = cartService;
        this.couponRepository = couponRepository;
        this.couponValidationService = couponValidationService;
        this.couponSessionService = couponSessionService;
        this.memberSessionService = memberSessionService;
    }

    @PostMapping
    public ResponseEntity<CouponApiResponse> apply(@RequestBody CouponApiRequest body,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    HttpSession session) {
        Long memberId = memberSessionService.currentMember(session)
                .map(member -> member.memberId())
                .orElse(null);
        String requestedCode = body == null || body.couponCode() == null ? null : body.couponCode().trim();
        try {
            couponSessionService.clearIfMemberChanged(session, memberId);
            if (!StringUtils.hasText(requestedCode)) {
                throw invalid(CouponErrorCode.COUPON_NOT_FOUND);
            }
            Coupon coupon = couponRepository.findByCode(requestedCode)
                    .orElseThrow(() -> invalid(CouponErrorCode.COUPON_NOT_FOUND));
            if (!coupon.active()) {
                throw invalid(CouponErrorCode.COUPON_NOT_FOUND);
            }
            String existingCode = couponSessionService.getCouponCode(session);
            if (StringUtils.hasText(existingCode) && !existingCode.equals(requestedCode)) {
                throw invalid(CouponErrorCode.COUPON_ALREADY_APPLIED);
            }

            CartView cart = cartService.getCart(request, response);
            CouponValidationResult result = couponValidationService.validateCoupon(coupon, cart, memberId);
            if (!result.valid()) {
                throw invalid(result.errorCode());
            }
            couponSessionService.apply(session, coupon.code(), memberId);
            CartView discountedCart = cartService.recalculateWithCouponDiscount(cart, result.discountAmount());
            return ResponseEntity.ok(CouponApiResponse.success(
                    toSummary(discountedCart, result.discountAmount(), coupon.code(), coupon.name())));
        } catch (CouponValidationException ex) {
            return ResponseEntity.status(httpStatus(ex.errorCode()))
                    .body(CouponApiResponse.failure(ex.errorCode().name(), ex.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<CouponApiResponse> remove(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    HttpSession session) {
        couponSessionService.clear(session);
        CartView cart = cartService.getCart(request, response);
        return ResponseEntity.ok(CouponApiResponse.success(
                toSummary(cart, java.math.BigDecimal.ZERO, null, null)));
    }

    private CouponValidationException invalid(CouponErrorCode errorCode) {
        return new CouponValidationException(errorCode, couponValidationService.message(errorCode));
    }

    private HttpStatus httpStatus(CouponErrorCode errorCode) {
        return switch (errorCode) {
            case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case COUPON_ALREADY_APPLIED, COUPON_USAGE_LIMIT_REACHED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private CouponCartSummaryResponse toSummary(CartView cart,
                                                java.math.BigDecimal discount,
                                                String code,
                                                String name) {
        java.math.BigDecimal discountedSubtotal = cart.summary().productSubtotal().subtract(discount)
                .max(java.math.BigDecimal.ZERO);
        java.math.BigDecimal total = discountedSubtotal.add(cart.summary().assemblyFeeTotal())
                .add(cart.summary().taxAmount()).add(cart.summary().shippingFee());
        return new CouponCartSummaryResponse(
                cart.summary().productSubtotal(),
                discount,
                discountedSubtotal,
                cart.summary().assemblyFeeTotal(),
                cart.summary().taxAmount(),
                cart.summary().shippingFee(),
                total,
                code,
                name
        );
    }
}
