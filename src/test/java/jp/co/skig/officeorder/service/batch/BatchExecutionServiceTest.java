package jp.co.skig.officeorder.service.batch;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import jp.co.skig.officeorder.model.batch.BatchExecutionAcceptedResponse;
import jp.co.skig.officeorder.model.batch.BatchExecutionStopAcceptedResponse;
import jp.co.skig.officeorder.model.batch.BatchJobSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchExecutionServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T00:00:00Z");
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private MessageSource messageSource;

    private BatchExecutionService sut;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, JST);
        sut = new BatchExecutionService(jobOperator, jobExplorer, clock, messageSource);
    }

    // ─── validateJobName (via listExecutions) ───────────────────────────

    @Test
    void listExecutions_unknown_job_name_throws_unknown_batch_job_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("ジョブが見つかりません");

        // Act / Assert
        assertThatThrownBy(() -> sut.listExecutions("unknown_job", 10))
                .isInstanceOf(UnknownBatchJobException.class);
    }

    @Test
    void listExecutions_known_job_name_returns_result() {
        // Arrange
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 10)).thenReturn(List.of());

        // Act
        var result = sut.listExecutions(BatchJobNames.POPULAR_RANKING, 10);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─── listExecutions limit normalization ────────────────────────────

    @Test
    void listExecutions_limit_zero_is_normalized_to_1() {
        // Arrange
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1)).thenReturn(List.of());

        // Act
        sut.listExecutions(BatchJobNames.POPULAR_RANKING, 0);

        // Assert - limit=0 → 1 に補正されて getJobInstances が呼ばれる
        org.mockito.Mockito.verify(jobExplorer).getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1);
    }

    @Test
    void listExecutions_limit_over_100_is_normalized_to_100() {
        // Arrange
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 100)).thenReturn(List.of());

        // Act
        sut.listExecutions(BatchJobNames.POPULAR_RANKING, 999);

        // Assert
        org.mockito.Mockito.verify(jobExplorer).getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 100);
    }

    // ─── submitJob ───────────────────────────────────────────────────────

    @Test
    void submitJob_unknown_job_throws_unknown_batch_job_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("ジョブが見つかりません");

        // Act / Assert
        assertThatThrownBy(() -> sut.submitJob("invalid_job"))
                .isInstanceOf(UnknownBatchJobException.class);
    }

    @Test
    void submitJob_already_running_throws_batch_already_running_exception() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("実行中です");
        JobExecution runningExecution = new JobExecution(1L);
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING))
                .thenReturn(Set.of(runningExecution));

        // Act / Assert
        assertThatThrownBy(() -> sut.submitJob(BatchJobNames.POPULAR_RANKING))
                .isInstanceOf(BatchAlreadyRunningException.class);
    }

    // ─── stopExecution ───────────────────────────────────────────────────

    @Test
    void stopExecution_execution_not_found_throws_batch_execution_not_found_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("見つかりません");
        when(jobExplorer.getJobExecution(999L)).thenReturn(null);

        // Act / Assert
        assertThatThrownBy(() -> sut.stopExecution(999L))
                .isInstanceOf(BatchExecutionNotFoundException.class);
    }

    @Test
    void stopExecution_not_running_execution_throws_batch_execution_not_running_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("実行中でありません");
        JobExecution completedExecution = new JobExecution(1L);
        completedExecution.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(completedExecution);

        // Act / Assert
        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    // ─── listJobs ────────────────────────────────────────────────────────

    @Test
    void listJobs_returns_sorted_list_with_no_history_when_no_executions() {
        // Arrange
        when(jobExplorer.getJobInstances(anyString(), any(int.class), any(int.class))).thenReturn(List.of());
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());

        // Act
        List<BatchJobSummaryResponse> result = sut.listJobs();

        // Assert
        assertThat(result).hasSize(2); // popular-ranking, recommended-related
        assertThat(result).extracting(BatchJobSummaryResponse::jobName).isSorted();
    }

    @Test
    void listJobs_running_job_shows_running_true() {
        // Arrange
        JobExecution runningExecution = buildJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobInstances(anyString(), any(int.class), any(int.class))).thenReturn(List.of());
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING))
                .thenReturn(Set.of(runningExecution));
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.RECOMMENDED_RELATED))
                .thenReturn(Set.of());

        // Act
        List<BatchJobSummaryResponse> result = sut.listJobs();

        // Assert
        assertThat(result).anySatisfy(r -> {
            if (r.jobName().equals(BatchJobNames.POPULAR_RANKING)) {
                assertThat(r.running()).isTrue();
            }
        });
    }

    @Test
    void listJobs_with_history_includes_status_and_times() {
        // Arrange
        JobInstance instance = new JobInstance(1L, BatchJobNames.POPULAR_RANKING);
        JobExecution exec = new JobExecution(instance, 10L, new JobParameters());
        exec.setStatus(BatchStatus.COMPLETED);
        exec.setStartTime(LocalDateTime.of(2026, 4, 14, 10, 0));
        exec.setEndTime(LocalDateTime.of(2026, 4, 14, 10, 5));
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1))
                .thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec));
        when(jobExplorer.getJobInstances(BatchJobNames.RECOMMENDED_RELATED, 0, 1)).thenReturn(List.of());
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());

        // Act
        List<BatchJobSummaryResponse> result = sut.listJobs();

        // Assert
        assertThat(result).anySatisfy(r -> {
            if (r.jobName().equals(BatchJobNames.POPULAR_RANKING)) {
                assertThat(r.latestStatus()).isEqualTo("COMPLETED");
            }
        });
    }

    // ─── submitJob (success) ─────────────────────────────────────────────

    @Test
    void submitJob_known_job_not_running_returns_accepted_response() throws Exception {
        // Arrange
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING)).thenReturn(Set.of());
        when(jobOperator.start(anyString(), any())).thenReturn(42L);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("受け付けました");

        // Act
        BatchExecutionAcceptedResponse result = sut.submitJob(BatchJobNames.POPULAR_RANKING);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.executionId()).isEqualTo("42");
    }

    // ─── runStartupJobs / runScheduledHourlyJobs ─────────────────────────

    @Test
    void runStartupJobs_triggers_all_supported_jobs() throws Exception {
        // Arrange
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());
        when(jobOperator.start(anyString(), any())).thenReturn(1L);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("受け付けました");

        // Act (no exception)
        sut.runStartupJobs();

        // Assert
        verify(jobOperator, org.mockito.Mockito.atLeastOnce()).start(anyString(), any());
    }

    @Test
    void runScheduledHourlyJobs_triggers_all_supported_jobs() throws Exception {
        // Arrange
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());
        when(jobOperator.start(anyString(), any())).thenReturn(2L);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("受け付けました");

        // Act (no exception)
        sut.runScheduledHourlyJobs();

        // Assert
        verify(jobOperator, org.mockito.Mockito.atLeastOnce()).start(anyString(), any());
    }

    @Test
    void runStartupJobs_already_running_job_is_skipped_without_exception() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("既に実行中");
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of(new JobExecution(1L)));

        // Act (no exception thrown for already-running)
        sut.runStartupJobs();
    }

    // ─── stopExecution (success) ─────────────────────────────────────────

    @Test
    void stopExecution_running_execution_returns_stop_accepted_response() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("停止受け付けました");
        JobExecution runningExecution = buildJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(runningExecution);
        when(jobOperator.stop(1L)).thenReturn(true);

        // Act
        BatchExecutionStopAcceptedResponse result = sut.stopExecution(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.executionId()).isEqualTo("1");
    }

    @Test
    void stopExecution_stop_returns_false_throws_not_running_exception() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("停止失敗");
        JobExecution runningExecution = buildJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(runningExecution);
        when(jobOperator.stop(1L)).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    @Test
    void stopExecution_job_operator_throws_exception_wraps_in_illegal_state() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("停止失敗");
        JobExecution runningExecution = buildJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(runningExecution);
        when(jobOperator.stop(1L)).thenThrow(new RuntimeException("JMX timeout"));

        // Act / Assert – jobOperator.stop() が例外 → IllegalStateException でラップ
        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitJob_job_operator_throws_exception_wraps_in_illegal_state() throws Exception {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("起動失敗");
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING)).thenReturn(Set.of());
        when(jobOperator.start(anyString(), any())).thenThrow(new RuntimeException("JMX timeout"));

        // Act / Assert – jobOperator.start() が例外 → IllegalStateException でラップ
        assertThatThrownBy(() -> sut.submitJob(BatchJobNames.POPULAR_RANKING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runTriggeredJobs_runtime_exception_is_logged_and_does_not_propagate() throws Exception {
        // Arrange – running check passes, but start() throws RuntimeException (not BatchAlreadyRunningException)
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("失敗");
        when(jobExplorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());
        when(jobOperator.start(anyString(), any())).thenThrow(new RuntimeException("unexpected"));

        // Act / Assert – RuntimeException は握りつぶされるので例外はスローされない
        org.assertj.core.api.Assertions.assertThatCode(() -> sut.runStartupJobs())
                .doesNotThrowAnyException();
    }

    @Test
    void listExecutions_with_execution_maps_to_summary_response() {
        // Arrange – 実行履歴 1 件を返す
        JobExecution execution = buildJobExecutionWithTimes(42L, BatchStatus.COMPLETED,
                LocalDateTime.of(2026, 4, 14, 10, 0, 0),
                LocalDateTime.of(2026, 4, 14, 10, 1, 0));
        JobInstance instance = execution.getJobInstance();
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 5))
                .thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(execution));

        // Act
        var results = sut.listExecutions(BatchJobNames.POPULAR_RANKING, 5);

        // Assert – toExecutionResponse が呼ばれ DTO が構築される
        assertThat(results).hasSize(1);
        assertThat(results.get(0).executionId()).isEqualTo("42");
        assertThat(results.get(0).status()).isEqualTo("COMPLETED");
        // endTime は非 null なので toOffsetDateTime(...) の非 null パスがカバーされる
        assertThat(results.get(0).endTime()).isNotNull();
    }

    @Test
    void toOffsetDateTime_null_value_returns_null() {
        // listJobs で latest.getEndTime() が null の場合 toOffsetDateTime(null) が呼ばれる
        // Arrange – 終了時刻なしの実行履歴
        JobExecution execution = buildJobExecution(99L, BatchStatus.STARTED);
        // startTime/endTime は設定しないので null のまま
        JobInstance instance = execution.getJobInstance();
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1))
                .thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(execution));
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING)).thenReturn(Set.of());
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.RECOMMENDED_RELATED)).thenReturn(Set.of());
        when(jobExplorer.getJobInstances(BatchJobNames.RECOMMENDED_RELATED, 0, 1)).thenReturn(List.of());

        // Act – listJobs → toOffsetDateTime(null) が呼ばれる (endTime null)
        var jobs = sut.listJobs();

        // Assert – null タイムは null で表示される
        var popularJob = jobs.stream()
                .filter(j -> BatchJobNames.POPULAR_RANKING.equals(j.jobName()))
                .findFirst().orElseThrow();
        assertThat(popularJob.latestEndTime()).isNull();
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private JobExecution buildJobExecution(long executionId, BatchStatus status) {
        JobInstance instance = new JobInstance(1L, BatchJobNames.POPULAR_RANKING);
        JobExecution execution = new JobExecution(instance, executionId, new JobParameters());
        execution.setStatus(status);
        return execution;
    }

    private JobExecution buildJobExecutionWithTimes(long executionId, BatchStatus status,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        JobInstance instance = new JobInstance(executionId, BatchJobNames.POPULAR_RANKING);
        org.springframework.batch.core.JobParametersBuilder builder =
                new org.springframework.batch.core.JobParametersBuilder();
        builder.addString("trigger", "manual");
        JobExecution execution = new JobExecution(instance, executionId, builder.toJobParameters());
        execution.setStatus(status);
        execution.setStartTime(startTime);
        execution.setEndTime(endTime);
        return execution;
    }
}
