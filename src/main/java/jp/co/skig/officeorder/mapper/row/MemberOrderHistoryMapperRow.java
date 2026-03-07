package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 購入履歴一覧の1注文分を表す行。
 */
public record MemberOrderHistoryMapperRow(
        String orderNumber,
        OffsetDateTime orderDatetime,
        BigDecimal totalAmount,
        String orderStatus
) {
}
