package jp.co.skig.officeorder.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.mapper.row.BatchOrderProductOccurrenceMapperRow;
import jp.co.skig.officeorder.mapper.row.BatchPopularRankingCandidateMapperRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 売れ筋ランキングとおすすめ関連商品の再計算で使用する集計入出力を扱う Mapper。
 */
@Mapper
public interface BatchMapper {

    /**
     * 直近1か月の受注から売れ筋ランキング候補を商品単位で集計して取得する。
     */
    List<BatchPopularRankingCandidateMapperRow> selectPopularRankingCandidates(@Param("sinceAt") OffsetDateTime sinceAt,
            @Param("asOf") OffsetDateTime asOf);

    /**
     * 指定日の売れ筋ランキングを再生成する前に既存データを削除する。
     */
    int deletePopularRankingsByDate(@Param("rankingDate") LocalDate rankingDate);

    /**
     * 売れ筋ランキングの1件分を保存する。
     */
    int insertPopularRanking(@Param("rankingDate") LocalDate rankingDate,
            @Param("rank") int rank,
            @Param("productId") long productId,
            @Param("soldQuantity1m") int soldQuantity1m);

    /**
     * おすすめ関連商品再計算向けに、同一注文内に出現した商品IDを取得する。
     */
    List<BatchOrderProductOccurrenceMapperRow> selectOrderProductOccurrences(@Param("sinceAt") OffsetDateTime sinceAt);

    /**
     * 指定日のおすすめ関連商品を再生成する前に既存データを削除する。
     */
    int deleteRecommendedRelatedByDate(@Param("recommendationDate") LocalDate recommendationDate);

    /**
     * おすすめ関連商品の1件分を保存する。
     */
    int insertRecommendedRelated(@Param("recommendationDate") LocalDate recommendationDate,
            @Param("sourceProductId") long sourceProductId,
            @Param("rank") int rank,
            @Param("recommendedProductId") long recommendedProductId,
            @Param("score") BigDecimal score);
}
