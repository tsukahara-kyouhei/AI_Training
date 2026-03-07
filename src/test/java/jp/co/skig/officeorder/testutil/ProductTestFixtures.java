package jp.co.skig.officeorder.testutil;

import java.math.BigDecimal;
import java.util.List;

import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductSeriesLinkView;
import jp.co.skig.officeorder.model.product.ProductVariantView;

/**
 * 商品一覧・商品詳細テストで共通利用するフィクスチャ集。
 */
public final class ProductTestFixtures {

    private ProductTestFixtures() {
    }

    /**
     * 候補未設定の空の絞り込み候補束を返す。
     *
     * @return 空の絞り込み候補
     */
    public static ProductFilterOptionsBundle emptyOptionsBundle() {
        return new ProductFilterOptionsBundle(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 商品カードの標準例を返す。
     *
     * @param productId 商品ID
     * @param productName 商品名
     * @param detailUrl 詳細URL
     * @return 商品カード
     */
    public static ProductCardView productCardView(long productId, String productName, String detailUrl) {
        return new ProductCardView(productId, productName, "12,000円", List.of("#ffffff"), "P0001-C01", true, detailUrl);
    }

    /**
     * 商品詳細表示の標準例を返す。
     *
     * @return 商品詳細表示モデル
     */
    public static ProductDetailView productDetailView() {
        ProductVariantView selectedVariant = new ProductVariantView(
                11L,
                "P0001-C01",
                1L,
                "ホワイト",
                "#ffffff",
                BigDecimal.valueOf(12000),
                "12,000",
                4
        );
        return new ProductDetailView(
                1L,
                "Nordis ワークデスク 幅120cm",
                "説明",
                "desk",
                true,
                100L,
                "幅120cm",
                BigDecimal.TEN,
                BigDecimal.valueOf(12000),
                "12,000",
                BigDecimal.valueOf(13200),
                "13,200",
                BigDecimal.valueOf(3000),
                "3,000",
                selectedVariant,
                List.of(selectedVariant),
                List.of(new ProductSeriesLinkView(2L, "Nordis ワークデスク 幅140cm", false)),
                List.of(),
                false
        );
    }
}
