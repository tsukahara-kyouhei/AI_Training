package jp.co.skig.officeorder.testutil;

import java.math.BigDecimal;
import java.util.List;

import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;

/**
 * カート画面・注文導線テストで共通利用するカートフィクスチャ集。
 */
public final class CartTestFixtures {

    private CartTestFixtures() {
    }

    /**
     * 商品1明細を持つ標準カートを返す。
     *
     * @return カート表示モデル
     */
    public static CartView cartView() {
        return new CartView(
                List.of(new CartLineView(
                        1L,
                        1L,
                        "Nordis ワークデスク 幅120cm",
                        "P0001-C01",
                        "ホワイト",
                        BigDecimal.valueOf(12000),
                        4,
                        true,
                        BigDecimal.valueOf(3000),
                        true,
                        1,
                        "/products/1"
                )),
                1,
                1,
                new CartSummaryView(
                        BigDecimal.valueOf(12000),
                        BigDecimal.valueOf(3000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1500),
                        BigDecimal.valueOf(16500)
                )
        );
    }
}
