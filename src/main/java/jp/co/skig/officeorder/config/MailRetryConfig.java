package jp.co.skig.officeorder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * メール再送用スケジューラを定義する設定。
 */
@Configuration
public class MailRetryConfig {

    /**
     * メール再送用 TaskScheduler を生成する。
     *
     * @return TaskScheduler
     */
    @Bean(name = "mailRetryScheduler")
    public TaskScheduler mailRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("mail-retry-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.initialize();
        return scheduler;
    }
}
