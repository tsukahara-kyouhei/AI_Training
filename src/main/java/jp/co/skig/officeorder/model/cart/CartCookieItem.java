package jp.co.skig.officeorder.model.cart;

/**
 * Cookie に保存するカート1行分の最小情報。
 */
public record CartCookieItem(
                long productVariantId,
                int quantity,
                Boolean assemblyRequested) {
}
