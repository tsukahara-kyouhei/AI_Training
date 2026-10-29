package jp.co.skig.officeorder.service.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.Coupon;
import jp.co.skig.officeorder.model.coupon.CouponConditions;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.model.coupon.CouponValidationResult;
import jp.co.skig.officeorder.repository.CouponRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;

@Service
public class CouponValidationService {
    private static final List<String> CATEGORY_IDS = List.of("desk", "chair", "storage");

    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;
    private final AppTimeProvider appTimeProvider;
    private final MessageSourceAccessor messages;

    public CouponValidationService(CouponRepository couponRepository,
                                   ObjectMapper objectMapper,
                                   AppTimeProvider appTimeProvider,
                                   MessageSource messageSource) {
        this.couponRepository = couponRepository;
        this.objectMapper = objectMapper;
        this.appTimeProvider = appTimeProvider;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public CouponValidationResult validateCoupon(Coupon coupon,
                                                CartView cart,
                                                Long memberId) {
        if (coupon == null || !coupon.active()) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_NOT_FOUND);
        }

        OffsetDateTime now = appTimeProvider.nowOffsetDateTime();
        if (now.isBefore(coupon.validFrom()) || now.isAfter(coupon.validTo())) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_EXPIRED);
        }

        CouponConditions conditions;
        try {
            conditions = parseTargetConditions(coupon.targetConditions());
        } catch (IllegalArgumentException ex) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_TARGET_ERROR);
        }

        if (memberId == null && (coupon.maxUsagePerUser() != null
                || "new".equals(conditions.targetUserType()))) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_USER_CONDITION_ERROR);
        }
        if ("new".equals(conditions.targetUserType())
                && couponRepository.countCompletedOrders(memberId) != 0) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_USER_CONDITION_ERROR);
        }

        List<CartLineView> targetItems = cart == null ? List.of() : cart.items().stream()
                .filter(line -> conditions.allowedCategoryIds().contains(line.categoryId())
                        || conditions.allowedProductIds().contains(line.productId()))
                .toList();
        if (targetItems.isEmpty()) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_TARGET_ERROR);
        }

        BigDecimal targetSubtotal = targetItems.stream()
                .map(CartLineView::lineProductSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (targetSubtotal.compareTo(coupon.minPurchaseAmount()) < 0) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_MIN_AMOUNT_ERROR);
        }
        if (coupon.maxUsageTotal() != null && coupon.usedCountTotal() >= coupon.maxUsageTotal()) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }
        if (memberId != null && coupon.maxUsagePerUser() != null
                && couponRepository.findUserUsage(memberId, coupon.id())
                .map(usage -> usage.usageCount() >= coupon.maxUsagePerUser())
                .orElse(false)) {
            return CouponValidationResult.invalid(CouponErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }

        return CouponValidationResult.valid(coupon, calculateDiscount(coupon, targetSubtotal));
    }

    public CouponConditions parseTargetConditions(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("target_conditions must be an object");
            }
            JsonNode categoriesNode = root.get("allowed_category_ids");
            JsonNode productsNode = root.get("allowed_product_ids");
            JsonNode userTypeNode = root.get("target_user_type");
            if (categoriesNode == null || !categoriesNode.isArray()
                    || productsNode == null || !productsNode.isArray()
                    || userTypeNode == null || !userTypeNode.isTextual()) {
                throw new IllegalArgumentException("target_conditions is missing required values");
            }
            Iterator<String> keys = root.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!List.of("allowed_category_ids", "allowed_product_ids", "target_user_type").contains(key)) {
                    throw new IllegalArgumentException("target_conditions contains an unknown key");
                }
            }

            List<String> categories = new ArrayList<>();
            for (JsonNode category : categoriesNode) {
                if (!category.isTextual() || !CATEGORY_IDS.contains(category.textValue())) {
                    throw new IllegalArgumentException("target_conditions contains an invalid category");
                }
                categories.add(category.textValue());
            }

            List<Long> productIds = new ArrayList<>();
            for (JsonNode productId : productsNode) {
                if (!productId.isIntegralNumber() || !productId.canConvertToLong()
                        || productId.longValue() <= 0) {
                    throw new IllegalArgumentException("target_conditions contains an invalid product id");
                }
                productIds.add(productId.longValue());
            }

            String userType = userTypeNode.textValue();
            if (!"all".equals(userType) && !"new".equals(userType)) {
                throw new IllegalArgumentException("target_conditions contains an invalid user type");
            }
            return new CouponConditions(List.copyOf(categories), List.copyOf(productIds), userType);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("target_conditions is not valid JSON", ex);
        }
    }

    public void validateTargetConditionsForRegistration(String json) {
        parseTargetConditions(json);
    }

    public String message(CouponErrorCode errorCode) {
        return messages.getMessage("business.coupon." + errorCode.name());
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal targetSubtotal) {
        BigDecimal discount;
        if ("fixed_amount".equals(coupon.discountType())) {
            discount = coupon.discountValue();
        } else if ("percentage".equals(coupon.discountType())) {
            discount = targetSubtotal.multiply(coupon.discountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            if (coupon.maxDiscountAmount() != null) {
                discount = discount.min(coupon.maxDiscountAmount());
            }
        } else {
            throw new IllegalStateException("クーポンの割引種別が不正です。");
        }
        return discount.min(targetSubtotal);
    }
}
