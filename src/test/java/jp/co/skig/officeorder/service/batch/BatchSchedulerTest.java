package jp.co.skig.officeorder.service.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerTest {

    @Mock
    private BatchExecutionService batchExecutionService;

    private BatchScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new BatchScheduler(batchExecutionService);
    }

    // ─── runStartupJobs ─────────────────────────────────────────────────

    @Test
    void runStartupJobs_delegates_to_batch_execution_service() {
        // Act
        sut.runStartupJobs();

        // Assert
        verify(batchExecutionService).runStartupJobs();
    }

    // ─── runHourlyJobs ──────────────────────────────────────────────────

    @Test
    void runHourlyJobs_delegates_to_batch_execution_service() {
        // Act
        sut.runHourlyJobs();

        // Assert
        verify(batchExecutionService).runScheduledHourlyJobs();
    }
}
