package jp.co.skig.officeorder.mapper;

/**
 * 価格帯や寸法帯の検索条件を SQL に渡すための下限・上限ペア。
 */
public record RangeValue(
        Long minInclusive,
        Long maxInclusive
) {
}


