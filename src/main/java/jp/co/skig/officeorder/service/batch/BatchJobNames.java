package jp.co.skig.officeorder.service.batch;

/**
 * 本システムで扱う集計系バッチジョブ名の定義。
 *
 * <p>
 * Scheduler、設定クラス、実行APIで同じ文字列を共有するため、
 * 定数に集約している。
 */
public final class BatchJobNames {

    /** 売れ筋ランキング再計算ジョブ名。 */
    public static final String POPULAR_RANKING = "popular-ranking";
    /** おすすめ関連商品再計算ジョブ名。 */
    public static final String RECOMMENDED_RELATED = "recommended-related";

    private BatchJobNames() {
    }
}
