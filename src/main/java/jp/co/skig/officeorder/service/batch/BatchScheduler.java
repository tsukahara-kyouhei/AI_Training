package jp.co.skig.officeorder.service.batch;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 集計系バッチの起動トリガを束ねるスケジューラ。
 *
 * <p>
 * サーバー起動時と毎時0分の定期実行をここで受け、
 * 実際の起動判定や二重起動防止は {@link BatchExecutionService} に委譲する。
 */
@Component
public class BatchScheduler {

    /** バッチ起動制御を担当するサービス。 */
    private final BatchExecutionService batchExecutionService;

    /**
     * バッチスケジューラを生成する。
     *
     * @param batchExecutionService 起動対象ジョブの実行制御サービス
     */
    public BatchScheduler(BatchExecutionService batchExecutionService) {
        this.batchExecutionService = batchExecutionService;
    }

    /**
     * アプリ起動完了時に初回集計ジョブを起動する。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runStartupJobs() {
        batchExecutionService.runStartupJobs();
    }

    /**
     * 毎時0分に定期集計ジョブを起動する。
     */
    @Scheduled(cron = "${app.batch.hourly-cron:0 0 * * * *}", zone = "${app.time-zone:Asia/Tokyo}")
    public void runHourlyJobs() {
        batchExecutionService.runScheduledHourlyJobs();
    }
}
