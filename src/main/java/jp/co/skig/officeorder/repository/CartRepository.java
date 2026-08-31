package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.CartMapper;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import org.springframework.stereotype.Repository;

/**
 * カート表示に必要な商品スナップショットと税率を取得するリポジトリ。
 */
@Repository
public class CartRepository {

    /** カート用SQLを呼び出す MyBatis Mapper。 */
    private final CartMapper cartMapper;
    /** 販売期間・税率判定に使う共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * カートリポジトリを生成する。
     *
     * @param cartMapper      カートMapper
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public CartRepository(CartMapper cartMapper, AppTimeProvider appTimeProvider) {
        this.cartMapper = cartMapper;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * カート内商品バリアントの表示用スナップショットを取得する。
     *
     * @param productVariantIds 商品バリアントID一覧
     * @return バリアントIDをキーにした商品スナップショット
     */
    public Map<Long, CartProductSnapshot> findProductSnapshotsByVariantIds(List<Long> productVariantIds) {
        if (productVariantIds == null || productVariantIds.isEmpty()) {
            return Map.of();
        }
        List<CartProductSnapshot> rows = cartMapper.selectCartProductSnapshots(
                productVariantIds,
                appTimeProvider.nowOffsetDateTime());
        Map<Long, CartProductSnapshot> snapshots = new HashMap<>();
        for (CartProductSnapshot snapshot : rows) {
            snapshots.put(snapshot.productVariantId(), snapshot);
        }
        return snapshots;
    }

    /**
     * 現在有効な消費税率を取得する。
     *
     * @return 税率百分率。未設定時は 10
     */
    public BigDecimal findCurrentTaxRatePercent() {
        BigDecimal taxRate = cartMapper.selectCurrentTaxRatePercent(appTimeProvider.nowOffsetDateTime());
        return taxRate == null ? BigDecimal.TEN : taxRate;
    }
}
