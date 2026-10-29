package jp.co.skig.officeorder.service.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.mapper.row.UserCouponUsageMapperRow;
import jp.co.skig.officeorder.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class CouponValidationServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-03T12:00:00+09:00");

    private CouponRepository couponRepository;
    private CouponValidationService service;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        AppTimeProvider timeProvider = mock(AppTimeProvider.class);
        when(timeProvider.nowOffsetDateTime()).thenReturn(NOW);
        service = new CouponValidationService(
                couponRepository,
                new ObjectMapper(),
                timeProvider,
                mock(MessageSource.class)
        );
    }

    @Test
    void percentageDiscountUsesFloorAndMaximumDiscount() {
        Coupon coupon = coupon("percentage", "80", "50", "0");
        CartView cart = cart(line(100L, "desk", "101"));

        var result = service.validateCoupon(coupon, cart, null);

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("50");
    }

    @Test
    void percentageDiscountFloorsFractionalYen() {
        Coupon coupon = coupon("percentage", "10", null, "0");
        CartView cart = cart(line(100L, "desk", "101"));

        var result = service.validateCoupon(coupon, cart, null);

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("10");
    }

    @Test
    void fixedDiscountCannotExceedTargetSubtotal() {
        Coupon coupon = coupon("fixed_amount", "200", null, "0");
        CartView cart = cart(line(100L, "desk", "101"));

        var result = service.validateCoupon(coupon, cart, null);

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("101");
    }

    @Test
    void validityStartAndEndInstantsAreInclusive() {
        Coupon coupon = new Coupon(
                1L,
                "TEST",
                "テストクーポン",
                "fixed_amount",
                new BigDecimal("10"),
                null,
                BigDecimal.ZERO,
                NOW,
                NOW,
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                """,
                null,
                null,
                0L,
                true
        );

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void expiredCouponIsRejectedBeforeTargetAndMinimumChecks() {
        Coupon coupon = coupon("fixed_amount", "10", null, "100");
        coupon = withValidity(coupon, NOW.minusDays(2), NOW.minusDays(1));

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "0")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_EXPIRED);
    }

    @Test
    void inactiveCouponIsReportedAsNotFound() {
        Coupon coupon = withActive(coupon("fixed_amount", "10", null, "0"), false);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    void categoryOrProductMatchesAndTargetSubtotalIncludesEveryMatchingLine() {
        Coupon coupon = withConditions(
                coupon("fixed_amount", "100", null, "90"),
                """
                {"allowed_category_ids":["chair"],"allowed_product_ids":[100],"target_user_type":"all"}
                """
        );
        CartView cart = cart(
                line(100L, "desk", "50"),
                line(101L, "chair", "40"),
                line(102L, "desk", "999")
        );

        var result = service.validateCoupon(coupon, cart, null);

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("90");
    }

    @Test
    void targetSubtotalOneYenBelowMinimumIsRejected() {
        Coupon coupon = coupon("fixed_amount", "10", null, "100");

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "99")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_MIN_AMOUNT_ERROR);
    }

    @Test
    void noMatchingTargetIsRejectedBeforeMinimumAmount() {
        Coupon coupon = coupon("fixed_amount", "10", null, "0");

        var result = service.validateCoupon(coupon, cart(line(100L, "chair", "100")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_TARGET_ERROR);
    }

    @Test
    void guestCannotUseCouponWithPerUserLimit() {
        Coupon coupon = withLimits(coupon("fixed_amount", "10", null, "0"), null, 1, 0L);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_USER_CONDITION_ERROR);
    }

    @Test
    void guestCannotUseNewMemberCoupon() {
        Coupon coupon = withConditions(
                coupon("fixed_amount", "10", null, "0"),
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"new"}
                """
        );

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_USER_CONDITION_ERROR);
        verify(couponRepository, never()).countCompletedOrders(1L);
    }

    @Test
    void newMemberCouponIsRejectedWhenMemberHasCompletedOrders() {
        Coupon coupon = withConditions(
                coupon("fixed_amount", "10", null, "0"),
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"new"}
                """
        );
        when(couponRepository.countCompletedOrders(1L)).thenReturn(1L);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), 1L);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_USER_CONDITION_ERROR);
    }

    @Test
    void newMemberCouponIsAcceptedWhenMemberHasNoCompletedOrders() {
        Coupon coupon = withConditions(
                coupon("fixed_amount", "10", null, "0"),
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"new"}
                """
        );
        when(couponRepository.countCompletedOrders(1L)).thenReturn(0L);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), 1L);

        assertThat(result.valid()).isTrue();
        verify(couponRepository).countCompletedOrders(1L);
    }

    @Test
    void totalUsageLimitAtCapacityIsRejected() {
        Coupon coupon = withLimits(coupon("fixed_amount", "10", null, "0"), 2L, null, 2L);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_USAGE_LIMIT_REACHED);
    }

    @Test
    void memberUsageLimitAtCapacityIsRejected() {
        Coupon coupon = withLimits(coupon("fixed_amount", "10", null, "0"), null, 2, 0L);
        when(couponRepository.findUserUsage(7L, coupon.id()))
                .thenReturn(Optional.of(new UserCouponUsageMapperRow(1L, 7L, coupon.id(), 2, NOW)));

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), 7L);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_USAGE_LIMIT_REACHED);
    }

    @Test
    void nullUsageLimitsAreUnlimited() {
        Coupon coupon = withLimits(coupon("fixed_amount", "10", null, "0"), null, null, 100L);

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), 7L);

        assertThat(result.valid()).isTrue();
        verify(couponRepository, never()).findUserUsage(7L, coupon.id());
    }

    @Test
    void percentageDiscountWithoutMaximumDiscountCanExceedFixedAmount() {
        Coupon coupon = coupon("percentage", "20", null, "0");

        var result = service.validateCoupon(coupon, cart(line(100L, "desk", "101")), null);

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("20");
    }

    @Test
    void zeroPricedTargetIsMatchedButFailsPositiveMinimumAmount() {
        Coupon coupon = coupon("fixed_amount", "200", null, "1");
        CartView cart = cart(line(100L, "desk", "0"));

        var result = service.validateCoupon(coupon, cart, null);

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(CouponErrorCode.COUPON_MIN_AMOUNT_ERROR);
    }

    @Test
    void registrationValidationRejectsMalformedAndMissingTargetConditions() {
        List<String> invalidConditions = List.of(
                "{",
                "null",
                """
                {"allowed_category_ids":null,"allowed_product_ids":[],"target_user_type":"all"}
                """,
                """
                {"allowed_category_ids":[1],"allowed_product_ids":[],"target_user_type":"all"}
                """,
                """
                {"allowed_category_ids":["invalid"],"allowed_product_ids":[],"target_user_type":"all"}
                """,
                """
                {"allowed_category_ids":[],"allowed_product_ids":[0],"target_user_type":"all"}
                """,
                """
                {"allowed_category_ids":[],"allowed_product_ids":[],"target_user_type":"unknown"}
                """
        );

        for (String conditions : invalidConditions) {
            assertThatThrownBy(() -> service.validateTargetConditionsForRegistration(conditions))
                    .as("conditions: %s", conditions)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void registrationValidationRejectsUnknownTargetConditionKeys() {
        assertThatThrownBy(() -> service.validateTargetConditionsForRegistration(
                """
                {"allowed_category_ids":[],"allowed_product_ids":[],"target_user_type":"all","other":true}
                """
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private Coupon coupon(String type, String value, String maxDiscount, String minimum) {
        return new Coupon(
                1L,
                "TEST",
                "テストクーポン",
                type,
                new BigDecimal(value),
                maxDiscount == null ? null : new BigDecimal(maxDiscount),
                new BigDecimal(minimum),
                NOW.minusDays(1),
                NOW.plusDays(1),
                """
                {"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}
                """,
                null,
                null,
                0L,
                true
        );
    }

    private Coupon withValidity(Coupon coupon, OffsetDateTime validFrom, OffsetDateTime validTo) {
        return new Coupon(coupon.id(), coupon.code(), coupon.name(), coupon.discountType(),
                coupon.discountValue(), coupon.maxDiscountAmount(), coupon.minPurchaseAmount(),
                validFrom, validTo, coupon.targetConditions(), coupon.maxUsageTotal(),
                coupon.maxUsagePerUser(), coupon.usedCountTotal(), coupon.active());
    }

    private Coupon withActive(Coupon coupon, boolean active) {
        return new Coupon(coupon.id(), coupon.code(), coupon.name(), coupon.discountType(),
                coupon.discountValue(), coupon.maxDiscountAmount(), coupon.minPurchaseAmount(),
                coupon.validFrom(), coupon.validTo(), coupon.targetConditions(), coupon.maxUsageTotal(),
                coupon.maxUsagePerUser(), coupon.usedCountTotal(), active);
    }

    private Coupon withConditions(Coupon coupon, String conditions) {
        return new Coupon(coupon.id(), coupon.code(), coupon.name(), coupon.discountType(),
                coupon.discountValue(), coupon.maxDiscountAmount(), coupon.minPurchaseAmount(),
                coupon.validFrom(), coupon.validTo(), conditions, coupon.maxUsageTotal(),
                coupon.maxUsagePerUser(), coupon.usedCountTotal(), coupon.active());
    }

    private Coupon withLimits(Coupon coupon, Long totalLimit, Integer memberLimit, long usedTotal) {
        return new Coupon(coupon.id(), coupon.code(), coupon.name(), coupon.discountType(),
                coupon.discountValue(), coupon.maxDiscountAmount(), coupon.minPurchaseAmount(),
                coupon.validFrom(), coupon.validTo(), coupon.targetConditions(), totalLimit,
                memberLimit, usedTotal, coupon.active());
    }

    private CartView cart(CartLineView... items) {
        List<CartLineView> lines = List.of(items);
        return new CartView(
                lines,
                lines.size(),
                lines.stream().mapToInt(CartLineView::quantity).sum(),
                new CartSummaryView(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    private CartLineView line(long productId, String categoryId, String price) {
        return new CartLineView(
                1L,
                productId,
                "商品",
                "P-001",
                categoryId,
                "ブラック",
                new BigDecimal(price),
                10,
                false,
                BigDecimal.ZERO,
                false,
                1,
                "/products/" + productId
        );
    }
}
