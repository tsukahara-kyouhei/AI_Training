package jp.co.skig.officeorder.mapper.row;

import java.time.OffsetDateTime;

/**
 * 注文ステータス履歴の1件分を表す行。
 */
public record MemberOrderStatusHistoryMapperRow(
                OffsetDateTime changedAt,
                String status) {
}
