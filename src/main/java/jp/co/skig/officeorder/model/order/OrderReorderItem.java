package jp.co.skig.officeorder.model.order;

/**
 * 再購入処理でカートへ戻すための最小情報。
 */
public record OrderReorderItem(
                Long productVariantId,
                String productCode,
                int quantity,
                Boolean assemblyRequested) {
}
