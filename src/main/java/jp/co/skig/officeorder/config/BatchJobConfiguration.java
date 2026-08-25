package jp.co.skig.officeorder.config;

import java.time.Clock;
import java.time.LocalDate;

import jp.co.skig.officeorder.service.batch.BatchJobExecutionLoggingListener;
import jp.co.skig.officeorder.service.batch.BatchJobNames;
import jp.co.skig.officeorder.service.batch.BatchJobService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 集計系バッチジョブ本体を定義する設定。
 */
@Configuration
public class BatchJobConfiguration {

    /**
     * 売れ筋ランキング再計算ジョブを生成する。
     *
     * @param jobRepository      ジョブリポジトリ
     * @param popularRankingStep 売れ筋ランキング集計ステップ
     * @param listener           ジョブ実行ログリスナー
     * @return 売れ筋ランキングジョブ
     */
    @Bean
    public Job popularRankingJob(JobRepository jobRepository,
            Step popularRankingStep,
            BatchJobExecutionLoggingListener listener) {
        return new JobBuilder(BatchJobNames.POPULAR_RANKING, jobRepository)
                .listener(listener)
                .start(popularRankingStep)
                .build();
    }

    /**
     * 売れ筋ランキング再計算ステップを生成する。
     *
     * @param jobRepository      ジョブリポジトリ
     * @param transactionManager トランザクションマネージャ
     * @param batchJobService    集計サービス
     * @param appClock           アプリ標準Clock
     * @return ステップ
     */
    @Bean
    public Step popularRankingStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BatchJobService batchJobService,
            Clock appClock) {
        return new StepBuilder("popularRankingStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate rankingDate = LocalDate.now(appClock);
                    batchJobService.executePopularRanking(rankingDate);
                    return null;
                }, transactionManager)
                .build();
    }

    /**
     * おすすめ関連商品再計算ジョブを生成する。
     *
     * @param jobRepository          ジョブリポジトリ
     * @param recommendedRelatedStep おすすめ関連商品集計ステップ
     * @param listener               ジョブ実行ログリスナー
     * @return おすすめ関連商品ジョブ
     */
    @Bean
    public Job recommendedRelatedJob(JobRepository jobRepository,
            Step recommendedRelatedStep,
            BatchJobExecutionLoggingListener listener) {
        return new JobBuilder(BatchJobNames.RECOMMENDED_RELATED, jobRepository)
                .listener(listener)
                .start(recommendedRelatedStep)
                .build();
    }

    /**
     * おすすめ関連商品再計算ステップを生成する。
     *
     * @param jobRepository      ジョブリポジトリ
     * @param transactionManager トランザクションマネージャ
     * @param batchJobService    集計サービス
     * @param appClock           アプリ標準Clock
     * @return ステップ
     */
    @Bean
    public Step recommendedRelatedStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BatchJobService batchJobService,
            Clock appClock) {
        return new StepBuilder("recommendedRelatedStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate recommendationDate = LocalDate.now(appClock);
                    batchJobService.executeRecommendedRelated(recommendationDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
