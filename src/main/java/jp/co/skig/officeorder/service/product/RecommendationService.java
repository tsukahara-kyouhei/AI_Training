package jp.co.skig.officeorder.service.product;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * トップ画面「超マジお薦め」セクション向けのパーソナライズ推薦を担当するサービス。
 *
 * <p>以下の3段階フォールバック戦略で最大 {@value #TARGET_COUNT} 件の推薦商品を返す。
 * <ol>
 *   <li>購入履歴（直近6か月）を起点とした推薦関連商品</li>
 *   <li>最近見た商品を起点とした推薦関連商品</li>
 *   <li>売れ筋ランキングフォールバック</li>
 * </ol>
 */
@Service
public class RecommendationService {

    /** トップ画面パーソナライズセクションの表示件数上限。 */
    private static final int TARGET_COUNT = 4;
    /** 購入履歴の参照期間（月数）。 */
    private static final int PURCHASE_HISTORY_MONTHS = 6;

    /** 商品参照リポジトリ。 */
    private final ProductRepository productRepository;
    /** 注文参照リポジトリ。 */
    private final OrderRepository orderRepository;
    /** 時刻依存条件に使う共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * 推薦サービスを生成する。
     *
     * @param productRepository 商品リポジトリ
     * @param orderRepository 注文リポジトリ
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public RecommendationService(ProductRepository productRepository,
                                 OrderRepository orderRepository,
                                 AppTimeProvider appTimeProvider) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * トップ画面向けのパーソナライズ推薦商品を返す。
     *
     * <p>ログイン会員の場合は購入履歴を起点に推薦を取得した後、不足分を最近見た商品で補う。
     * 非ログイン会員または推薦が不足している場合は売れ筋ランキングで補完する。
     *
     * @param memberId ログイン中の会員ID。非ログインの場合は {@code null}
     * @param recentlyViewedIds セッションから取得した最近見た商品IDリスト
     * @return 推薦商品カード一覧（最大 {@value #TARGET_COUNT} 件）
     */
    public List<ProductCardView> findTopPersonalizedRecommendations(Long memberId,
                                                                    List<Long> recentlyViewedIds) {
        List<ProductCardView> result = new ArrayList<>();
        List<Long> excludedIds = new ArrayList<>();

        // Tier 1: 購入履歴（ログイン会員のみ）
        if (memberId != null) {
            OffsetDateTime from = appTimeProvider.nowOffsetDateTime().minusMonths(PURCHASE_HISTORY_MONTHS);
            List<Long> purchasedIds = orderRepository.findPurchasedProductIds(memberId, from);
            if (!purchasedIds.isEmpty()) {
                excludedIds.addAll(purchasedIds);
                List<ProductCardView> tier1 = productRepository.findCrossSellProducts(
                        purchasedIds, excludedIds, TARGET_COUNT);
                result.addAll(tier1);
                addNewIds(excludedIds, tier1);
            }
        }

        // Tier 2: 最近見た商品
        if (result.size() < TARGET_COUNT
                && recentlyViewedIds != null
                && !recentlyViewedIds.isEmpty()) {
            List<Long> tier2Exclude = new ArrayList<>(excludedIds);
            tier2Exclude.addAll(recentlyViewedIds);
            List<ProductCardView> tier2 = productRepository.findCrossSellProducts(
                    recentlyViewedIds, tier2Exclude, TARGET_COUNT - result.size());
            result.addAll(tier2);
            addNewIds(excludedIds, tier2);
        }

        // Tier 3: 売れ筋ランキングフォールバック
        if (result.size() < TARGET_COUNT) {
            List<ProductCardView> tier3 = productRepository.findRankedForRecommendation(
                    excludedIds, TARGET_COUNT - result.size());
            result.addAll(tier3);
        }

        return List.copyOf(result);
    }

    /**
     * 推薦結果から新規IDだけ除外リストへ追加する。
     */
    private void addNewIds(List<Long> excludedIds, List<ProductCardView> products) {
        for (ProductCardView p : products) {
            if (!excludedIds.contains(p.productId())) {
                excludedIds.add(p.productId());
            }
        }
    }
}
