package jp.co.skig.officeorder.mapper.row;

/**
 * お気に入り商品パネルのカラー表示に使う色コード行。
 */
public record MemberColorCodeMapperRow(
        Long productId,
        String colorCode,
        Integer sortOrder
) {
}
