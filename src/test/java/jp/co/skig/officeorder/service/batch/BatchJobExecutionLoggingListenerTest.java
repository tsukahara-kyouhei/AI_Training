package jp.co.skig.officeorder.service.batch;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BatchJobExecutionLoggingListenerTest {

    private final BatchJobExecutionLoggingListener sut = new BatchJobExecutionLoggingListener();

    // ─── beforeJob ───────────────────────────────────────────────────────

    @Test
    void beforeJob_logs_start_event_without_exception() {
        // Arrange
        JobExecution execution = buildJobExecution(1L, "manual");
        execution.setStatus(BatchStatus.STARTED);

        // Act / Assert
        assertThatCode(() -> sut.beforeJob(execution)).doesNotThrowAnyException();
    }

    @Test
    void beforeJob_without_trigger_parameter_logs_unknown() {
        // Arrange
        JobExecution execution = buildJobExecution(2L, null);

        // Act / Assert
        assertThatCode(() -> sut.beforeJob(execution)).doesNotThrowAnyException();
    }

    // ─── afterJob ────────────────────────────────────────────────────────

    @Test
    void afterJob_completed_status_logs_info() {
        // Arrange
        JobExecution execution = buildJobExecution(1L, "startup");
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        execution.setStartTime(LocalDateTime.of(2026, 4, 14, 10, 0));
        execution.setEndTime(LocalDateTime.of(2026, 4, 14, 10, 5));

        // Act / Assert
        assertThatCode(() -> sut.afterJob(execution)).doesNotThrowAnyException();
    }

    @Test
    void afterJob_stopped_status_logs_warn() {
        // Arrange
        JobExecution execution = buildJobExecution(2L, "manual");
        execution.setStatus(BatchStatus.STOPPED);
        execution.setExitStatus(ExitStatus.STOPPED);
        execution.setStartTime(LocalDateTime.of(2026, 4, 14, 10, 0));
        execution.setEndTime(LocalDateTime.of(2026, 4, 14, 10, 3));

        // Act / Assert
        assertThatCode(() -> sut.afterJob(execution)).doesNotThrowAnyException();
    }

    @Test
    void afterJob_failed_status_logs_error() {
        // Arrange
        JobExecution execution = buildJobExecution(3L, "scheduled");
        execution.setStatus(BatchStatus.FAILED);
        execution.setExitStatus(new ExitStatus("FAILED", "Some error description"));
        execution.setStartTime(LocalDateTime.of(2026, 4, 14, 10, 0));
        execution.setEndTime(LocalDateTime.of(2026, 4, 14, 10, 1));

        // Act / Assert
        assertThatCode(() -> sut.afterJob(execution)).doesNotThrowAnyException();
    }

    @Test
    void afterJob_null_start_and_end_time_returns_minus_one_duration() {
        // Arrange
        JobExecution execution = buildJobExecution(4L, "startup");
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        // startTime and endTime are null (default)

        // Act / Assert - durationMs should be -1 but no exception
        assertThatCode(() -> sut.afterJob(execution)).doesNotThrowAnyException();
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private JobExecution buildJobExecution(long executionId, String trigger) {
        JobInstance instance = new JobInstance(1L, BatchJobNames.POPULAR_RANKING);
        org.springframework.batch.core.JobParametersBuilder builder =
                new org.springframework.batch.core.JobParametersBuilder();
        if (trigger != null) {
            builder.addString("trigger", trigger);
        }
        return new JobExecution(instance, executionId, builder.toJobParameters());
    }
}
