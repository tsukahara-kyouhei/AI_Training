package jp.co.skig.officeorder.model.coupon;

import java.time.OffsetDateTime;

/**
 * クーポンモデル
 */
public record Coupon(
        Long id,
        String code,
        String name,
        String discountType,    // 'amount' (定額) または 'rate' (定率)
        Integer discountValue,  // 割引額 または 割引率(%)
        Integer minOrderAmount, // 最小購入金額
        OffsetDateTime startAt, // 有効期間（開始）
        OffsetDateTime endAt,   // 有効期間（終了）
        Integer usageLimit,     // 全体利用上限回数
        Boolean isActive,       // 有効フラグ
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
