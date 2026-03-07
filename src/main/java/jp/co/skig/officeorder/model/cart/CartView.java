package jp.co.skig.officeorder.model.cart;

import java.util.List;

/**
 * カート画面全体の表示モデル。
 */
public record CartView(
        List<CartLineView> items,
        int itemTypeCount,
        int totalQuantity,
        CartSummaryView summary
) {
    /**
     * 表示対象の明細がない空カートかを判定する。
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}

