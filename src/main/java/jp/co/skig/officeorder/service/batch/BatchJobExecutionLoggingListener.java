package jp.co.skig.officeorder.service.batch;

import java.time.Duration;
import java.time.LocalDateTime;

import jp.co.skig.officeorder.logging.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch のジョブ開始・終了を共通フォーマットで記録するリスナー。
 *
 * <p>
 * ジョブ名、実行ID、トリガ種別、実行時間をここで一元的に記録し、
 * 個別ジョブ側では業務結果のログに集中できるようにする。
 */
@Component
public class BatchJobExecutionLoggingListener implements JobExecutionListener {

    /** バッチ実行ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(BatchJobExecutionLoggingListener.class);

    /**
     * ジョブ開始時に開始ログを出力する。
     *
     * @param jobExecution 開始対象のジョブ実行
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String trigger = jobExecution.getJobParameters().getString("trigger", "unknown");
        log.info("event={} job={} executionId={} trigger={}",
                LogEvent.BATCH_EXECUTION_START.value(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                trigger);
    }

    /**
     * ジョブ終了時に状態別の完了ログを出力する。
     *
     * <p>
     * 正常終了は INFO、停止は WARN、それ以外の失敗は ERROR で記録する。
     *
     * @param jobExecution 終了したジョブ実行
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long durationMs = calculateDuration(jobExecution.getStartTime(), jobExecution.getEndTime());
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("event={} job={} executionId={} status={} durationMs={} exitCode={}",
                    LogEvent.BATCH_EXECUTION_END.value(),
                    jobName,
                    jobExecution.getId(),
                    jobExecution.getStatus(),
                    durationMs,
                    jobExecution.getExitStatus().getExitCode());
            return;
        }
        if (jobExecution.getStatus() == BatchStatus.STOPPED) {
            log.warn("event={} job={} executionId={} status={} durationMs={} exitCode={}",
                    LogEvent.BATCH_EXECUTION_FAILED.value(),
                    jobName,
                    jobExecution.getId(),
                    jobExecution.getStatus(),
                    durationMs,
                    jobExecution.getExitStatus().getExitCode());
            return;
        }
        log.error("event={} job={} executionId={} status={} durationMs={} exitCode={} exitDescription={}",
                LogEvent.BATCH_EXECUTION_FAILED.value(),
                jobName,
                jobExecution.getId(),
                jobExecution.getStatus(),
                durationMs,
                jobExecution.getExitStatus().getExitCode(),
                jobExecution.getExitStatus().getExitDescription());
    }

    /**
     * 実行時間をミリ秒で計算する。
     *
     * @param start 開始時刻
     * @param end   終了時刻
     * @return 実行時間ミリ秒。時刻が欠けている場合は {@code -1}
     */
    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return -1L;
        }
        return Duration.between(start, end).toMillis();
    }
}
