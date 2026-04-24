package jp.co.skig.officeorder.mapper.row;

import java.math.BigDecimal;

/**
 * レビュー集計結果（平均評価・件数）の1行分のマッピング。
 */
public record ReviewSummaryMapperRow(
        long productId,
        int reviewCount,
        BigDecimal averageRating
) {
}
