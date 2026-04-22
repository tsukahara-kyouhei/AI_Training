package jp.co.skig.officeorder.service.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.MessageSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link BatchExecutionService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class BatchExecutionServiceTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private MessageSource messageSource;

    private BatchExecutionService sut;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        sut = new BatchExecutionService(jobOperator, jobExplorer, FIXED_CLOCK, messageSource);
        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("error message");
    }

    // =========================================================
    // validateJobName (サポート対象ジョブ名の検証)
    // =========================================================

    // ---- VJ-01: サポート対象ジョブ名でも submitJob が正常に呼べる（二重起動なし）----
    @Test
    @DisplayName("サポート対象ジョブ名で submitJob が例外なく実行される")
    void vj01_supportedJobName_noException() throws Exception {
        String jobName = BatchJobNames.POPULAR_RANKING;
        when(jobExplorer.findRunningJobExecutions(jobName)).thenReturn(Set.of());
        when(jobOperator.start(eq(jobName), any())).thenReturn(1L);

        var result = sut.submitJob(jobName);

        assertThat(result.jobName()).isEqualTo(jobName);
    }

    // ---- VJ-02: サポート対象外ジョブ名 → UnknownBatchJobException ----
    @Test
    @DisplayName("サポート対象外ジョブ名では UnknownBatchJobException をスローする")
    void vj02_unsupportedJobName_throwsUnknownBatchJobException() {
        assertThatThrownBy(() -> sut.submitJob("unknown_job"))
                .isInstanceOf(UnknownBatchJobException.class);
    }

    // ---- VJ-03: listExecutions でもサポート対象外 → UnknownBatchJobException ----
    @Test
    @DisplayName("listExecutions でもサポート対象外ジョブは UnknownBatchJobException をスローする")
    void vj03_listExecutionsUnsupportedJob_throwsException() {
        assertThatThrownBy(() -> sut.listExecutions("unknown_job", 10))
                .isInstanceOf(UnknownBatchJobException.class);
    }

    // =========================================================
    // submitJob (二重起動チェック)
    // =========================================================

    // ---- SJ-01: 実行中のジョブがある → BatchAlreadyRunningException ----
    @Test
    @DisplayName("ジョブが実行中の場合は BatchAlreadyRunningException をスローする")
    void sj01_alreadyRunning_throwsBatchAlreadyRunningException() {
        String jobName = BatchJobNames.POPULAR_RANKING;
        JobExecution running = new JobExecution(1L);
        when(jobExplorer.findRunningJobExecutions(jobName)).thenReturn(Set.of(running));

        assertThatThrownBy(() -> sut.submitJob(jobName))
                .isInstanceOf(BatchAlreadyRunningException.class);
    }

    // ---- SJ-02: 正常起動 → 受付結果を返す ----
    @Test
    @DisplayName("ジョブが実行中でない場合は正常に起動受付結果を返す")
    void sj02_notRunning_returnsAcceptedResponse() throws Exception {
        String jobName = BatchJobNames.RECOMMENDED_RELATED;
        when(jobExplorer.findRunningJobExecutions(jobName)).thenReturn(Set.of());
        when(jobOperator.start(eq(jobName), any())).thenReturn(42L);

        var result = sut.submitJob(jobName);

        assertThat(result.executionId()).isEqualTo("42");
        assertThat(result.jobName()).isEqualTo(jobName);
        assertThat(result.acceptedAt()).isNotNull();
    }

    // ---- SJ-03: JobOperator が例外を投げる → IllegalStateException でラップ ----
    @Test
    @DisplayName("JobOperator が例外を投げた場合は IllegalStateException でラップする")
    void sj03_operatorThrows_illegalStateException() throws Exception {
        String jobName = BatchJobNames.POPULAR_RANKING;
        when(jobExplorer.findRunningJobExecutions(jobName)).thenReturn(Set.of());
        when(jobOperator.start(eq(jobName), any()))
                .thenThrow(new org.springframework.batch.core.launch.NoSuchJobException("no job"));

        assertThatThrownBy(() -> sut.submitJob(jobName))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================
    // stopExecution
    // =========================================================

    // ---- SE-01: 実行IDが存在しない → BatchExecutionNotFoundException ----
    @Test
    @DisplayName("存在しない実行IDでは BatchExecutionNotFoundException をスローする")
    void se01_executionNotFound_throwsBatchExecutionNotFoundException() {
        when(jobExplorer.getJobExecution(99L)).thenReturn(null);

        assertThatThrownBy(() -> sut.stopExecution(99L))
                .isInstanceOf(BatchExecutionNotFoundException.class);
    }

    // ---- SE-02: 実行中でない → BatchExecutionNotRunningException ----
    @Test
    @DisplayName("実行中でないジョブの停止は BatchExecutionNotRunningException をスローする")
    void se02_notRunning_throwsBatchExecutionNotRunningException() {
        JobExecution execution = makeJobExecution(1L, BatchStatus.COMPLETED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(execution);

        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    // ---- SE-03: 停止要求が受け付けられない (stop=false) → BatchExecutionNotRunningException ----
    @Test
    @DisplayName("stop() が false を返す場合は BatchExecutionNotRunningException をスローする")
    void se03_stopReturnsFalse_throwsBatchExecutionNotRunningException() throws Exception {
        JobExecution execution = makeJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(execution);
        when(jobOperator.stop(1L)).thenReturn(false);

        assertThatThrownBy(() -> sut.stopExecution(1L))
                .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    // ---- SE-04: 正常停止 → 受付結果を返す ----
    @Test
    @DisplayName("実行中ジョブへの停止要求が正常に受け付けられる")
    void se04_normalStop_returnsStopAcceptedResponse() throws Exception {
        JobExecution execution = makeJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(1L)).thenReturn(execution);
        when(jobOperator.stop(1L)).thenReturn(true);

        var result = sut.stopExecution(1L);

        assertThat(result.executionId()).isEqualTo("1");
        assertThat(result.requestedAt()).isNotNull();
    }

    // =========================================================
    // listExecutions (limit 正規化)
    // =========================================================

    // ---- LE-01: limit < 1 → 1 に正規化 ----
    @Test
    @DisplayName("limit が 0 の場合は 1 に正規化してリポジトリに渡す")
    void le01_limitZero_normalizedToOne() {
        String jobName = BatchJobNames.POPULAR_RANKING;
        when(jobExplorer.getJobInstances(jobName, 0, 1)).thenReturn(List.of());

        var result = sut.listExecutions(jobName, 0);

        assertThat(result).isEmpty();
    }

    // ---- LE-02: limit > 100 → 100 に正規化 ----
    @Test
    @DisplayName("limit が 100 超の場合は 100 に正規化してリポジトリに渡す")
    void le02_limitAbove100_normalizedTo100() {
        String jobName = BatchJobNames.POPULAR_RANKING;
        when(jobExplorer.getJobInstances(jobName, 0, 100)).thenReturn(List.of());

        var result = sut.listExecutions(jobName, 200);

        assertThat(result).isEmpty();
    }

    // ---- LE-03: 実行履歴なし → 空リスト ----
    @Test
    @DisplayName("実行履歴がない場合は空リストを返す")
    void le03_noExecutions_returnsEmptyList() {
        String jobName = BatchJobNames.POPULAR_RANKING;
        when(jobExplorer.getJobInstances(jobName, 0, 10)).thenReturn(List.of());

        var result = sut.listExecutions(jobName, 10);

        assertThat(result).isEmpty();
    }

    // =========================================================
    // helpers
    // =========================================================

    private JobExecution makeJobExecution(long id, BatchStatus status) {
        JobInstance instance = new JobInstance(1L, BatchJobNames.POPULAR_RANKING);
        JobExecution execution = new JobExecution(instance, id,
                new org.springframework.batch.core.JobParameters());
        execution.setStatus(status);
        return execution;
    }
}
