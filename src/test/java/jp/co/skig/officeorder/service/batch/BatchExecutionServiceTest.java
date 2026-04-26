package jp.co.skig.officeorder.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.MessageSource;

import jp.co.skig.officeorder.model.batch.BatchExecutionStopAcceptedResponse;

/**
 * {@link BatchExecutionService} のユニットテスト。
 *
 * <p>
 * B-01〜B-09 のテストケースを網羅する。
 */
@ExtendWith(MockitoExtension.class)
class BatchExecutionServiceTest {

  private static final long EXECUTION_ID = 42L;
  private static final String VALID_JOB_NAME = BatchJobNames.POPULAR_RANKING;
  private static final String INVALID_JOB_NAME = "nonexistent";

  private JobOperator jobOperator;
  private JobExplorer jobExplorer;
  private BatchExecutionService service;

  @BeforeEach
  void setUp() {
    jobOperator = mock(JobOperator.class);
    jobExplorer = mock(JobExplorer.class);
    Clock clock = Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("UTC"));
    MessageSource ms = mock(MessageSource.class);
    lenient().when(ms.getMessage(any(String.class), any(Object[].class), any(Locale.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    service = new BatchExecutionService(jobOperator, jobExplorer, clock, ms);
  }

  // ============================================================
  // B-01〜B-05: stopExecution
  // ============================================================

  @Nested
  @DisplayName("stopExecution")
  class StopExecution {

    @Test
    @DisplayName("B-01: executionId が存在しない場合は BatchExecutionNotFoundException")
    void executionNotFound_throwsBatchExecutionNotFoundException() {
      when(jobExplorer.getJobExecution(EXECUTION_ID)).thenReturn(null);

      assertThatThrownBy(() -> service.stopExecution(EXECUTION_ID))
          .isInstanceOf(BatchExecutionNotFoundException.class);
    }

    @Test
    @DisplayName("B-02: 実行中でない JobExecution の場合は BatchExecutionNotRunningException")
    void executionNotRunning_throwsBatchExecutionNotRunningException() {
      JobExecution execution = mock(JobExecution.class);
      when(jobExplorer.getJobExecution(EXECUTION_ID)).thenReturn(execution);
      when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);

      assertThatThrownBy(() -> service.stopExecution(EXECUTION_ID))
          .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    @Test
    @DisplayName("B-03: jobOperator.stop() が例外を投げる場合は IllegalStateException")
    void jobOperatorStopThrows_throwsIllegalStateException() throws Exception {
      JobExecution execution = runningExecution();
      when(jobExplorer.getJobExecution(EXECUTION_ID)).thenReturn(execution);
      when(jobOperator.stop(EXECUTION_ID)).thenThrow(new RuntimeException("stop failed"));

      assertThatThrownBy(() -> service.stopExecution(EXECUTION_ID))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("B-04: jobOperator.stop() が false を返す場合は BatchExecutionNotRunningException")
    void jobOperatorStopReturnsFalse_throwsBatchExecutionNotRunningException() throws Exception {
      JobExecution execution = runningExecution();
      when(jobExplorer.getJobExecution(EXECUTION_ID)).thenReturn(execution);
      when(jobOperator.stop(EXECUTION_ID)).thenReturn(false);

      assertThatThrownBy(() -> service.stopExecution(EXECUTION_ID))
          .isInstanceOf(BatchExecutionNotRunningException.class);
    }

    @Test
    @DisplayName("B-05: 正常停止要求受付のとき BatchExecutionStopAcceptedResponse を返す")
    void stopAccepted_returnsStopAcceptedResponse() throws Exception {
      JobExecution execution = runningExecution();
      when(jobExplorer.getJobExecution(EXECUTION_ID)).thenReturn(execution);
      when(jobOperator.stop(EXECUTION_ID)).thenReturn(true);

      BatchExecutionStopAcceptedResponse response = service.stopExecution(EXECUTION_ID);

      assertThat(response).isNotNull();
      assertThat(response.executionId()).isEqualTo(String.valueOf(EXECUTION_ID));
    }

    /** 実行中状態の JobExecution モックを生成する。 */
    private JobExecution runningExecution() {
      JobExecution execution = mock(JobExecution.class);
      JobInstance instance = mock(JobInstance.class);
      when(execution.getStatus()).thenReturn(BatchStatus.STARTED);
      // stop() 失敗パスでは getJobInstance() まで届かないため lenient にする
      lenient().when(execution.getJobInstance()).thenReturn(instance);
      lenient().when(instance.getJobName()).thenReturn(VALID_JOB_NAME);
      return execution;
    }
  }

  // ============================================================
  // B-06〜B-09: listExecutions
  // ============================================================

  @Nested
  @DisplayName("listExecutions")
  class ListExecutions {

    @Test
    @DisplayName("B-06: 不正な jobName を指定すると UnknownBatchJobException")
    void invalidJobName_throwsUnknownBatchJobException() {
      assertThatThrownBy(() -> service.listExecutions(INVALID_JOB_NAME, 10))
          .isInstanceOf(UnknownBatchJobException.class);
    }

    @Test
    @DisplayName("B-07: limit=0 のとき 1 に補正されて getJobInstances が呼ばれる")
    void limitZero_normalizedToOne() {
      when(jobExplorer.getJobInstances(eq(VALID_JOB_NAME), eq(0), eq(1)))
          .thenReturn(List.of());

      service.listExecutions(VALID_JOB_NAME, 0);

      verify(jobExplorer).getJobInstances(VALID_JOB_NAME, 0, 1);
    }

    @Test
    @DisplayName("B-08: limit=100 のとき上限そのままで getJobInstances が呼ばれる")
    void limitOneHundred_noChange() {
      when(jobExplorer.getJobInstances(eq(VALID_JOB_NAME), eq(0), eq(100)))
          .thenReturn(List.of());

      service.listExecutions(VALID_JOB_NAME, 100);

      verify(jobExplorer).getJobInstances(VALID_JOB_NAME, 0, 100);
    }

    @Test
    @DisplayName("B-09: limit=101 のとき 100 に補正されて getJobInstances が呼ばれる")
    void limitOverMax_clampedToHundred() {
      when(jobExplorer.getJobInstances(eq(VALID_JOB_NAME), eq(0), eq(100)))
          .thenReturn(List.of());

      service.listExecutions(VALID_JOB_NAME, 101);

      verify(jobExplorer).getJobInstances(VALID_JOB_NAME, 0, 100);
    }
  }
}
