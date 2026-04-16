package jp.co.skig.officeorder.service.batch;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchExecutionServiceTest {

    private JobOperator jobOperator;
    private JobExplorer jobExplorer;
    private Clock clock;
    private MessageSource messageSource;
    private BatchExecutionService sut;

    @BeforeEach
    void setUp() {
        jobOperator = mock(JobOperator.class);
        jobExplorer = mock(JobExplorer.class);
        clock = Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneId.of("Asia/Tokyo"));
        messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
        sut = new BatchExecutionService(jobOperator, jobExplorer, clock, messageSource);
    }

    // --- validateJobName ---

    @Test
    @DisplayName("未知のジョブ名を指定すると UnknownBatchJobException をスローする")
    void listExecutions_unknownJobName_throwsUnknownBatchJobException() {
        assertThatThrownBy(() -> sut.listExecutions("unknown-job", 10))
                .isInstanceOf(UnknownBatchJobException.class);
    }

    // --- listExecutions（limit 正規化）---

    @Test
    @DisplayName("limit が 0 以下のとき 1 に正規化され getJobInstances が呼ばれる")
    void listExecutions_limitZero_normalizesToOne() {
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1)).thenReturn(List.of());

        sut.listExecutions(BatchJobNames.POPULAR_RANKING, 0);

        verify(jobExplorer).getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 1);
    }

    @Test
    @DisplayName("limit が 100 超のとき 100 に正規化され getJobInstances が呼ばれる")
    void listExecutions_limitOver100_normalizesTo100() {
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 100)).thenReturn(List.of());

        sut.listExecutions(BatchJobNames.POPULAR_RANKING, 200);

        verify(jobExplorer).getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 100);
    }

    @Test
    @DisplayName("limit が 1〜100 の範囲はそのまま使われる")
    void listExecutions_validLimit_usesAsIs() {
        when(jobExplorer.getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 5)).thenReturn(List.of());

        sut.listExecutions(BatchJobNames.POPULAR_RANKING, 5);

        verify(jobExplorer).getJobInstances(BatchJobNames.POPULAR_RANKING, 0, 5);
    }

    // --- submitJob ---

    @Test
    @DisplayName("すでに実行中のとき BatchAlreadyRunningException をスローする")
    void submitJob_alreadyRunning_throwsAlreadyRunningException() {
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING))
                .thenReturn(Set.of(mock(JobExecution.class)));

        assertThatThrownBy(() -> sut.submitJob(BatchJobNames.POPULAR_RANKING))
                .isInstanceOf(BatchAlreadyRunningException.class);
    }

    @Test
    @DisplayName("実行中でない場合はジョブを起動して受付レスポンスを返す")
    void submitJob_notRunning_startsJobAndReturnsResponse() throws Exception {
        when(jobExplorer.findRunningJobExecutions(BatchJobNames.POPULAR_RANKING)).thenReturn(Set.of());
        when(jobOperator.start(eq(BatchJobNames.POPULAR_RANKING), any())).thenReturn(1L);

        var response = sut.submitJob(BatchJobNames.POPULAR_RANKING);

        assertThat(response).isNotNull();
        assertThat(response.executionId()).isEqualTo("1");
        assertThat(response.jobName()).isEqualTo(BatchJobNames.POPULAR_RANKING);
    }

    // --- stopExecution ---

    @Test
    @DisplayName("実行 ID が見つからない場合は BatchExecutionNotFoundException をスローする")
    void stopExecution_executionNotFound_throwsBatchExecutionNotFoundException() {
        when(jobExplorer.getJobExecution(999L)).thenReturn(null);

        assertThatThrownBy(() -> sut.stopExecution(999L))
                .isInstanceOf(BatchExecutionNotFoundException.class);
    }

    @Test
    @DisplayName("指定実行が実行中でない場合は BatchExecutionNotRunningException をスローする")
    void stopExecution_notRunning_throwsBatchExecutionNotRunningException() {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(execution);

        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    @Test
    @DisplayName("実行中の場合は停止要求を送り受付レスポンスを返す")
    void stopExecution_running_stopsAndReturnsResponse() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.STARTED);
        JobInstance jobInstance = mock(JobInstance.class);
        when(jobInstance.getJobName()).thenReturn(BatchJobNames.POPULAR_RANKING);
        when(execution.getJobInstance()).thenReturn(jobInstance);
        when(jobExplorer.getJobExecution(1L)).thenReturn(execution);
        when(jobOperator.stop(1L)).thenReturn(true);

        var response = sut.stopExecution(1L);

        assertThat(response).isNotNull();
        assertThat(response.executionId()).isEqualTo("1");
    }
}
