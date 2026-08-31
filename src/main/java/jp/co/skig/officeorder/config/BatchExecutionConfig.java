package jp.co.skig.officeorder.config;

import org.springframework.batch.core.configuration.JobLocator;
//import org.springframework.batch.core.configuration.JobRegistry;
//import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
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
@Configuration
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

    /*
     * 非同期実行用の JobOperator を生成する。
     *
     * @param jobRepository        ジョブリポジトリ
     * @param batchJobTaskExecutor バッチ実行用TaskExecutor
     * @return JobOperator
     * @throws Exception 初期化失敗時
     */
    /* 
    @Bean(name = "asyncJobOperator")
    @Primary
    public JobOperator asyncJobOperator(
            JobRepository jobRepository,
            @Qualifier("batchJobTaskExecutor") TaskExecutor batchJobTaskExecutor) throws Exception {
    */
        /*
         * TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();
         * jobOperator.setJobRepository(jobRepository);
         * jobOperator.setTaskExecutor(batchJobTaskExecutor);
         * jobOperator.afterPropertiesSet();
         * 
         * 
         * return jobOperator;
         */
        /* 
        JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
        factory.setJobRepository(jobRepository);
        factory.setTaskExecutor(batchJobTaskExecutor);
        factory.afterPropertiesSet();

        return factory.getObject();
    }
        */
}