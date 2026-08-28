package jp.co.skig.officeorder.config;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Batch の非同期実行基盤を定義する設定。
 */
// @Configuration
@EnableScheduling
public class BatchExecutionConfig {

    /**
     * バッチジョブ実行用 TaskExecutor を生成する。
     *
     * @return TaskExecutor
     */
    @Bean(name = "batchJobTaskExecutor")
    public TaskExecutor batchJobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("batch-job-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    /**
     * 非同期ジョブランチャを生成する。
     *
     * @param jobRepository        ジョブリポジトリ
     * @param batchJobTaskExecutor バッチ実行用TaskExecutor
     * @return JobLauncher
     * @throws Exception 初期化失敗時
     */
    @Bean(name = "asyncJobLauncher")
    public JobLauncher asyncJobLauncher(JobRepository jobRepository,
            @Qualifier("batchJobTaskExecutor") TaskExecutor batchJobTaskExecutor) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchJobTaskExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * 非同期実行前提の JobOperator を生成する。
     *
     * @param jobRepository ジョブリポジトリ
     * @param jobRegistry   ジョブレジストリ
     * @return JobOperator
     * @throws Exception 初期化失敗時
     */
    @Bean(name = "asyncJobOperator")
    @Primary
    public JobOperator asyncJobOperator(JobRepository jobRepository,
            JobRegistry jobRegistry) throws Exception {
        SimpleJobOperator jobOperator = new SimpleJobOperator();
        // JobLauncher のセットは不要になったため削除！
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.afterPropertiesSet();
        return jobOperator;
    }
}