package jp.co.skig.officeorder.mapper.row;

/**
 * 売れ筋ランキング再計算で使う、商品ごとの販売数量集計結果。
 */
public record BatchPopularRankingCandidateMapperRow(
        Long productId,
        Integer soldQuantity1m
) {
}
