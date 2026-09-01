package jp.co.skig.officeorder.model.member;

import java.util.List;

/**
 * お気に入り一覧の商品パネル表示モデル。
 */
public record MemberFavoriteView(
                long productId,
                String productName,
                String priceText,
                String taxExcludedPriceText,
                List<String> colorCodes,
                String productCode,
                boolean inStock,
                String detailUrl) {
}
