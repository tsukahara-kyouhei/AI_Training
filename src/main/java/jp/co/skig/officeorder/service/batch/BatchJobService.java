package jp.co.skig.officeorder.service.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.repository.BatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集計系バッチジョブの業務処理を担当するサービス。
 *
 * <p>
 * 売れ筋ランキングとおすすめ関連商品について、集計対象データの抽出条件、
 * 並び順、件数上限、保存方式をここで統一している。
 */
@Service
public class BatchJobService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);
    /** 売れ筋ランキングとして保存する最大件数。 */
    private static final int POPULAR_LIMIT = 10;
    /** 1商品あたり保存するおすすめ関連商品の最大件数。 */
    private static final int RELATED_LIMIT = 4;

    /** 集計元データ取得と集計結果保存を行うリポジトリ。 */
    private final BatchRepository batchRepository;
    /** バッチ計算の基準時刻を取得するアプリ共通Clock。 */
    private final Clock appClock;

    /**
     * 集計系バッチサービスを生成する。
     *
     * @param batchRepository 集計元データ取得と結果保存を担当するリポジトリ
     * @param appClock        バッチ計算時刻の基準となるアプリ共通Clock
     */
    public BatchJobService(BatchRepository batchRepository, Clock appClock) {
        this.batchRepository = batchRepository;
        this.appClock = appClock;
    }

    /**
     * 売れ筋ランキングを再計算し、当日分の結果を全置換で保存する。
     *
     * <p>
     * 処理概要:
     * <ul>
     * <li>算出時点から直近30日かつキャンセル以外の注文を対象にする</li>
     * <li>算出時点で販売期間内の商品だけを対象に販売数量を集計する</li>
     * <li>販売数量降順、同数時は {@code product_id} 昇順で並べる</li>
     * <li>上位10件を採用する</li>
     * <li>当日分の {@code popular_product_rankings} を削除して再挿入する</li>
     * </ul>
     *
     * <p>
     * 同日中の再実行では、同じ {@code rankingDate} の結果を最新内容で置き換える。
     *
     * @param rankingDate 算出日
     * @return 保存したランキング件数
     */
    @Transactional
    public int executePopularRanking(LocalDate rankingDate) {
        OffsetDateTime now = OffsetDateTime.now(appClock);
        OffsetDateTime sinceAt = now.minusDays(30);
        List<BatchRepository.PopularRankingCandidate> candidates = batchRepository.findPopularRankingCandidates(sinceAt,
                now);
        // 販売数量降順、同数時は product_id 昇順で上位10件を採用する。
        List<BatchRepository.PopularRankingCandidate> top = candidates.stream()
                .sorted(Comparator
                        .comparingInt(BatchRepository.PopularRankingCandidate::soldQuantity1m).reversed()
                        .thenComparingLong(BatchRepository.PopularRankingCandidate::productId))
                .limit(POPULAR_LIMIT)
                .toList();
        batchRepository.replacePopularRankings(rankingDate, top);
        log.info("event={} job={} date={} rows={}",
                LogEvent.BATCH_JOB_UPDATED.value(),
                BatchJobNames.POPULAR_RANKING,
                rankingDate,
                top.size());
        return top.size();
    }

    /**
     * おすすめ関連商品を再計算し、当日分の結果を全置換で保存する。
     *
     * <p>
     * 処理概要:
     * <ul>
     * <li>直近30日かつキャンセル以外の注文から、注文ごとの商品出現情報を取得する</li>
     * <li>同一注文内で共起した商品ペアを集計する</li>
     * <li>商品ごとの登場注文数と商品ペアの共起回数から、コサイン類似度を算出する</li>
     * <li>元商品の単位でスコア降順・商品ID昇順に並べ、上位4件を採用する</li>
     * <li>当日分の {@code recommended_related_products} を削除して再挿入する</li>
     * </ul>
     *
     * <p>
     * 同日中の再実行では、同じ {@code recommendationDate} の結果を最新内容で置き換える。
     *
     * @param recommendationDate 算出日
     * @return 保存したおすすめ関連商品の件数
     */
    @Transactional
    public int executeRecommendedRelated(LocalDate recommendationDate) {
        OffsetDateTime sinceAt = OffsetDateTime.now(appClock).minusDays(30);
        List<BatchRepository.OrderProductOccurrence> occurrences = batchRepository.findOrderProductOccurrences(sinceAt);
        List<BatchRepository.RecommendedRelatedRow> rows = buildRelatedRows(occurrences);
        batchRepository.replaceRecommendedRelated(recommendationDate, rows);
        log.info("event={} job={} date={} rows={}",
                LogEvent.BATCH_JOB_UPDATED.value(),
                BatchJobNames.RECOMMENDED_RELATED,
                recommendationDate,
                rows.size());
        return rows.size();
    }

    /**
     * 注文ごとの商品出現情報から、おすすめ関連商品の保存行を組み立てる。
     *
     * <p>
     * 各商品について、同一注文内で一緒に購入された商品を候補とし、
     * {@code coOccurrence / sqrt(orderCount(source) * orderCount(target))} で
     * コサイン類似度を算出する。結果は {@code score} 降順、同点時は
     * {@code recommended_product_id} 昇順で並べ、1商品あたり最大4件まで返す。
     *
     * @param occurrences 直近30日注文から抽出した {@code order_id × product_id} の出現情報
     * @return 保存対象のおすすめ関連商品行
     */
    private List<BatchRepository.RecommendedRelatedRow> buildRelatedRows(
            List<BatchRepository.OrderProductOccurrence> occurrences) {
        if (occurrences == null || occurrences.isEmpty()) {
            return List.of();
        }

        // 注文単位で重複のない商品集合へ正規化し、共起集計の入力にする。
        Map<Long, Set<Long>> productsByOrder = new LinkedHashMap<>();
        for (BatchRepository.OrderProductOccurrence occurrence : occurrences) {
            productsByOrder
                    .computeIfAbsent(occurrence.orderId(), ignored -> new HashSet<>())
                    .add(occurrence.productId());
        }

        // 商品ごとの登場注文数と、商品ペアごとの共起回数を集計する。
        Map<Long, Integer> productOrderCounts = new HashMap<>();
        Map<Long, Map<Long, Integer>> pairCounts = new HashMap<>();

        for (Set<Long> productIds : productsByOrder.values()) {
            for (Long productId : productIds) {
                productOrderCounts.merge(productId, 1, Integer::sum);
            }
            List<Long> sorted = productIds.stream().sorted().toList();
            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    long left = sorted.get(i);
                    long right = sorted.get(j);
                    pairCounts.computeIfAbsent(left, ignored -> new HashMap<>()).merge(right, 1, Integer::sum);
                    pairCounts.computeIfAbsent(right, ignored -> new HashMap<>()).merge(left, 1, Integer::sum);
                }
            }
        }

        List<BatchRepository.RecommendedRelatedRow> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Integer>> sourceEntry : pairCounts.entrySet()) {
            long sourceProductId = sourceEntry.getKey();
            int sourceCount = productOrderCounts.getOrDefault(sourceProductId, 0);
            if (sourceCount == 0) {
                continue;
            }

            List<RecommendationCandidate> candidates = new ArrayList<>();
            // コサイン類似度で候補スコアを算出する。
            for (Map.Entry<Long, Integer> targetEntry : sourceEntry.getValue().entrySet()) {
                long targetProductId = targetEntry.getKey();
                int targetCount = productOrderCounts.getOrDefault(targetProductId, 0);
                if (targetCount == 0) {
                    continue;
                }
                int coCount = targetEntry.getValue();
                double scoreValue = coCount / Math.sqrt((double) sourceCount * targetCount);
                BigDecimal score = BigDecimal.valueOf(scoreValue).setScale(6, RoundingMode.HALF_UP);
                candidates.add(new RecommendationCandidate(targetProductId, score));
            }

            candidates.sort(Comparator
                    .comparing(RecommendationCandidate::score).reversed()
                    .thenComparingLong(RecommendationCandidate::targetProductId));

            int rank = 1;
            // 商品詳細表示仕様に合わせ、元商品ごとに上位4件までを保存する。
            for (RecommendationCandidate candidate : candidates) {
                if (rank > RELATED_LIMIT) {
                    break;
                }
                result.add(new BatchRepository.RecommendedRelatedRow(
                        sourceProductId,
                        candidate.targetProductId(),
                        rank,
                        candidate.score()));
                rank++;
            }
        }
        return result;
    }

    /**
     * おすすめ関連商品算出時の一時候補。
     *
     * @param targetProductId 推奨先の商品ID
     * @param score           コサイン類似度から算出したスコア
     */
    private record RecommendationCandidate(
            long targetProductId,
            BigDecimal score) {
    }
}
