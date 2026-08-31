package jp.co.skig.officeorder.service.batch;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.batch.BatchExecutionStopAcceptedResponse;
import jp.co.skig.officeorder.model.batch.BatchExecutionAcceptedResponse;
import jp.co.skig.officeorder.model.batch.BatchExecutionSummaryResponse;
import jp.co.skig.officeorder.model.batch.BatchJobSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

/**
 * 集計系バッチの起動・停止・実行状況参照を扱うサービス。
 *
 * <p>
 * 内製のバッチ管理画面やスケジューラから呼ばれ、
 * サポート対象ジョブ名の検証、二重起動防止、実行履歴の整形を一箇所で行う。
 */
@Service
public class BatchExecutionService {

    /** バッチ実行制御ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(BatchExecutionService.class);
    /** このシステムから起動可能なジョブ名一覧。 */
    private static final Set<String> SUPPORTED_JOB_NAMES = Set.of(
            BatchJobNames.POPULAR_RANKING,
            BatchJobNames.RECOMMENDED_RELATED);

    /** Spring Batch の起動・停止API。 */
    private final JobOperator jobOperator;
    /** Spring Batch の実行状況参照API。 */
    private final JobExplorer jobExplorer;
    /** 応答日時と起動パラメータ時刻の基準となる Clock。 */
    private final Clock appClock;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * バッチ実行制御サービスを生成する。
     *
     * @param jobOperator   ジョブ起動・停止API
     * @param jobExplorer   ジョブ実行状況参照API
     * @param appClock      応答時刻の基準となる Clock
     * @param messageSource 利用者向けメッセージ取得元
     */
    public BatchExecutionService(JobOperator jobOperator,
            JobExplorer jobExplorer,
            Clock appClock,
            MessageSource messageSource) {
        this.jobOperator = jobOperator;
        this.jobExplorer = jobExplorer;
        this.appClock = appClock;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * サポート対象ジョブの一覧と最新実行状況を返す。
     *
     * @return ジョブごとのサマリ一覧
     */
    public List<BatchJobSummaryResponse> listJobs() {
        List<BatchJobSummaryResponse> jobs = new ArrayList<>();
        for (String jobName : SUPPORTED_JOB_NAMES) {
            JobExecution latest = findLatestExecution(jobName);
            boolean running = !jobExplorer.findRunningJobExecutions(jobName).isEmpty();
            jobs.add(new BatchJobSummaryResponse(
                    jobName,
                    running,
                    latest == null ? null : latest.getStatus().name(),
                    latest == null ? null : String.valueOf(latest.getId()),
                    latest == null ? null : toOffsetDateTime(latest.getStartTime()),
                    latest == null ? null : toOffsetDateTime(latest.getEndTime())));
        }
        jobs.sort(Comparator.comparing(BatchJobSummaryResponse::jobName));
        return jobs;
    }

    /**
     * 指定ジョブの実行履歴を新しい順で返す。
     *
     * @param jobName 対象ジョブ名
     * @param limit   返却件数上限
     * @return 実行履歴一覧
     */
    public List<BatchExecutionSummaryResponse> listExecutions(String jobName, int limit) {
        validateJobName(jobName);
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        return loadExecutions(jobName, normalizedLimit).stream()
                .limit(normalizedLimit)
                .map(this::toExecutionResponse)
                .toList();
    }

    /**
     * 手動実行としてジョブを起動する。
     *
     * @param jobName 起動対象ジョブ名
     * @return 受付結果
     */
    public BatchExecutionAcceptedResponse submitJob(String jobName) {
        return submit(jobName, "manual");
    }

    /**
     * アプリ起動時の初回集計ジョブを起動する。
     */
    public void runStartupJobs() {
        runTriggeredJobs("startup");
    }

    /**
     * 毎時実行の定期集計ジョブを起動する。
     */
    public void runScheduledHourlyJobs() {
        runTriggeredJobs("scheduled");
    }

    /**
     * 指定実行IDのジョブに停止要求を送る。
     *
     * @param executionId 停止対象の実行ID
     * @return 停止受付結果
     */
    public BatchExecutionStopAcceptedResponse stopExecution(long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            throw new BatchExecutionNotFoundException(message("business.batch.executionNotFound", executionId));
        }
        if (!execution.getStatus().isRunning()) {
            throw new BatchExecutionNotRunningException(message("business.batch.executionNotRunning", executionId));
        }
        boolean stopAccepted;
        try {
            stopAccepted = jobOperator.stop(executionId);
        } catch (Exception ex) {
            throw new IllegalStateException(message("business.batch.stopFailed"), ex);
        }
        if (!stopAccepted) {
            throw new BatchExecutionNotRunningException(message("business.batch.executionNotRunning", executionId));
        }
        log.info("event={} job={} executionId={}",
                LogEvent.BATCH_STOP_REQUESTED.value(),
                execution.getJobInstance().getJobName(),
                executionId);
        return new BatchExecutionStopAcceptedResponse(
                String.valueOf(executionId),
                OffsetDateTime.now(appClock),
                message("flash.batch.stopAccepted"));
    }

    /**
     * 指定トリガでサポート対象ジョブを順に起動する。
     *
     * <p>
     * ジョブ単位の起動失敗は他ジョブに波及させず、ログのみ残して継続する。
     *
     * @param trigger 起動種別
     */
    private void runTriggeredJobs(String trigger) {
        log.info("event={} trigger={} jobs={}",
                LogEvent.BATCH_JOBS_TRIGGER_REQUESTED.value(),
                trigger,
                SUPPORTED_JOB_NAMES.size());
        for (String jobName : SUPPORTED_JOB_NAMES) {
            try {
                submit(jobName, trigger);
            } catch (BatchAlreadyRunningException ex) {
                log.info("event={} trigger={} job={} reason=already_running",
                        LogEvent.BATCH_TRIGGER_SKIPPED.value(),
                        trigger,
                        jobName);
            } catch (RuntimeException ex) {
                log.error("event={} trigger={} job={} reason=job_trigger_failed",
                        LogEvent.BATCH_TRIGGER_FAILED.value(),
                        trigger,
                        jobName,
                        ex);
            }
        }
    }

    /**
     * ジョブ名とトリガ種別を付けてジョブ起動を受け付ける。
     *
     * <p>
     * 実行中判定を通過した場合のみ Spring Batch に起動要求を渡す。
     *
     * @param jobName 起動対象ジョブ名
     * @param trigger 起動種別
     * @return 起動受付結果
     */
    private BatchExecutionAcceptedResponse submit(String jobName, String trigger) {
        validateJobName(jobName);
        if (!jobExplorer.findRunningJobExecutions(jobName).isEmpty()) {
            throw new BatchAlreadyRunningException(message("business.batch.alreadyRunning", jobName));
        }
        long executionId;
        try {
            executionId = jobOperator.start(jobName, buildStartParameters(trigger));
        } catch (Exception ex) {
            throw new IllegalStateException(message("business.batch.startFailed"), ex);
        }
        OffsetDateTime acceptedAt = OffsetDateTime.now(appClock);
        log.info("event={} job={} executionId={} trigger={}",
                LogEvent.BATCH_REQUEST_ACCEPTED.value(),
                jobName,
                executionId,
                trigger);
        return new BatchExecutionAcceptedResponse(
                String.valueOf(executionId),
                jobName,
                acceptedAt,
                message("flash.batch.startAccepted"));
    }

    /**
     * 指定ジョブの実行履歴を読み込み、作成日時降順に並べる。
     *
     * @param jobName 対象ジョブ名
     * @param limit   取得対象のジョブインスタンス件数
     * @return 並び替え済みの実行履歴
     */
    private List<JobExecution> loadExecutions(String jobName, int limit) {
        List<JobExecution> executions = new ArrayList<>();
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, limit);
        for (JobInstance instance : instances) {
            executions.addAll(jobExplorer.getJobExecutions(instance));
        }
        executions.sort(Comparator.comparing(
                JobExecution::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return executions;
    }

    /**
     * 最新の実行履歴を1件返す。
     *
     * @param jobName 対象ジョブ名
     * @return 最新実行。存在しない場合は {@code null}
     */
    private JobExecution findLatestExecution(String jobName) {
        List<JobExecution> executions = loadExecutions(jobName, 1);
        if (executions.isEmpty()) {
            return null;
        }
        return executions.getFirst();
    }

    /**
     * Spring Batch の実行情報を画面返却用 DTO に変換する。
     *
     * @param execution 変換対象の実行情報
     * @return 画面返却用サマリ
     */
    private BatchExecutionSummaryResponse toExecutionResponse(JobExecution execution) {
        return new BatchExecutionSummaryResponse(
                String.valueOf(execution.getId()),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getExitStatus() == null ? null : execution.getExitStatus().getExitCode(),
                toOffsetDateTime(execution.getCreateTime()),
                toOffsetDateTime(execution.getStartTime()),
                toOffsetDateTime(execution.getEndTime()),
                execution.getJobParameters().getString("trigger"));
    }

    /**
     * Spring Batch の {@link LocalDateTime} をアプリ時刻帯の {@link OffsetDateTime} に変換する。
     *
     * @param value 変換対象時刻
     * @return オフセット付き時刻。入力が {@code null} の場合は {@code null}
     */
    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(appClock.getZone()).toOffsetDateTime();
    }

    /**
     * サポート対象外ジョブ名を拒否する。
     *
     * @param jobName 検証対象ジョブ名
     */
    private void validateJobName(String jobName) {
        if (!SUPPORTED_JOB_NAMES.contains(jobName)) {
            throw new UnknownBatchJobException(message("business.batch.jobNotFound", jobName));
        }
    }

    /**
     * Spring Batch 起動用パラメータを生成する。
     *
     * <p>
     * requestedAt を付与して同一ジョブの再実行時にも別実行として扱えるようにする。
     *
     * @param trigger 起動種別
     * @return 起動パラメータ
     */
    private Properties buildStartParameters(String trigger) {
        Properties properties = new Properties();
        properties.setProperty("trigger", trigger);
        properties.setProperty("requestedAt", String.valueOf(appClock.millis()));
        return properties;
    }

    /**
     * 利用者向けメッセージを取得する。
     *
     * @param code メッセージコード
     * @param args 埋め込み引数
     * @return 解決済みメッセージ
     */
    private String message(String code, Object... args) {
        return messages.getMessage(code, args);
    }
}
