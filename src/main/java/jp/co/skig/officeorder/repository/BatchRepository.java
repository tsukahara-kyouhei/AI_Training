package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.mapper.BatchMapper;
import org.springframework.stereotype.Repository;

/**
 * 集計系バッチが利用する抽出・保存処理をまとめたリポジトリ。
 *
 * <p>SQLで取得した集計元データを、バッチサービスが扱いやすい record へ変換して返す。
 */
@Repository
public class BatchRepository {

    /** 集計系SQLを呼び出す MyBatis Mapper。 */
    private final BatchMapper batchMapper;

    /**
     * バッチリポジトリを生成する。
     *
     * @param batchMapper 集計系Mapper
     */
    public BatchRepository(BatchMapper batchMapper) {
        this.batchMapper = batchMapper;
    }

    /**
     * 売れ筋ランキング算出用の販売数量候補を取得する。
     *
     * @param sinceAt 集計開始日時
     * @param asOf 販売期間内判定の基準日時
     * @return 商品別販売数量候補
     */
    public List<PopularRankingCandidate> findPopularRankingCandidates(OffsetDateTime sinceAt, OffsetDateTime asOf) {
        return batchMapper.selectPopularRankingCandidates(sinceAt, asOf).stream()
                .map(row -> new PopularRankingCandidate(
                        row.productId(),
                        row.soldQuantity1m()
                ))
                .toList();
    }

    /**
     * 指定日の売れ筋ランキングを全置換で保存する。
     *
     * @param rankingDate ランキング日
     * @param rankings 保存対象ランキング
     */
    public void replacePopularRankings(LocalDate rankingDate, List<PopularRankingCandidate> rankings) {
        batchMapper.deletePopularRankingsByDate(rankingDate);
        for (int i = 0; i < rankings.size(); i++) {
            PopularRankingCandidate candidate = rankings.get(i);
            batchMapper.insertPopularRanking(rankingDate, i + 1, candidate.productId(), candidate.soldQuantity1m());
        }
    }

    /**
     * おすすめ関連商品算出用の注文内商品出現情報を取得する。
     *
     * @param sinceAt 集計開始日時
     * @return 注文内商品出現情報
     */
    public List<OrderProductOccurrence> findOrderProductOccurrences(OffsetDateTime sinceAt) {
        return batchMapper.selectOrderProductOccurrences(sinceAt).stream()
                .map(row -> new OrderProductOccurrence(
                        row.orderId(),
                        row.productId()
                ))
                .toList();
    }

    /**
     * 指定日のおおすすめ関連商品結果を全置換で保存する。
     *
     * @param recommendationDate 算出日
     * @param rows 保存対象行
     */
    public void replaceRecommendedRelated(LocalDate recommendationDate, List<RecommendedRelatedRow> rows) {
        batchMapper.deleteRecommendedRelatedByDate(recommendationDate);
        for (RecommendedRelatedRow row : rows) {
            batchMapper.insertRecommendedRelated(
                    recommendationDate,
                    row.sourceProductId(),
                    row.rank(),
                    row.recommendedProductId(),
                    row.score()
            );
        }
    }

    /**
     * 売れ筋ランキング算出用の商品別販売数量。
     *
     * @param productId 商品ID
     * @param soldQuantity1m 直近1か月販売数量
     */
    public record PopularRankingCandidate(
            long productId,
            int soldQuantity1m
    ) {
    }

    /**
     * おすすめ関連商品算出用の注文内商品出現情報。
     *
     * @param orderId 注文ID
     * @param productId 商品ID
     */
    public record OrderProductOccurrence(
            long orderId,
            long productId
    ) {
    }

    /**
     * おすすめ関連商品の保存行。
     *
     * @param sourceProductId 元商品ID
     * @param recommendedProductId 推薦商品ID
     * @param rank 順位
     * @param score 類似度スコア
     */
    public record RecommendedRelatedRow(
            long sourceProductId,
            long recommendedProductId,
            int rank,
            BigDecimal score
    ) {
    }
}





