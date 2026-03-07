package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.batch.BatchExecutionStopAcceptedResponse;
import jp.co.skig.officeorder.service.batch.BatchAlreadyRunningException;
import jp.co.skig.officeorder.service.batch.BatchExecutionNotFoundException;
import jp.co.skig.officeorder.service.batch.BatchExecutionNotRunningException;
import jp.co.skig.officeorder.service.batch.BatchExecutionService;
import jp.co.skig.officeorder.service.batch.UnknownBatchJobException;
import jp.co.skig.officeorder.model.batch.BatchErrorResponse;
import jp.co.skig.officeorder.model.batch.BatchExecutionAcceptedResponse;
import jp.co.skig.officeorder.model.batch.BatchExecutionSummaryResponse;
import jp.co.skig.officeorder.model.batch.BatchJobSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ローカル開発時のバッチ起動・参照APIを提供するController。
 */
@Profile("local")
@RestController
@RequestMapping("/internal/batch")
public class InternalBatchController {

    /** バッチAPIログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(InternalBatchController.class);

    /** バッチ実行制御サービス。 */
    private final BatchExecutionService batchExecutionService;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * 内部バッチAPI Controllerを生成する。
     *
     * @param batchExecutionService バッチ実行制御サービス
     * @param messageSource 利用者向けメッセージ取得元
     */
    public InternalBatchController(BatchExecutionService batchExecutionService,
                                   MessageSource messageSource) {
        this.batchExecutionService = batchExecutionService;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * サポート対象ジョブ一覧を返す。
     *
     * @return ジョブサマリ一覧
     */
    @GetMapping("/jobs")
    public List<BatchJobSummaryResponse> listJobs() {
        return batchExecutionService.listJobs();
    }

    /**
     * 指定ジョブの実行履歴を返す。
     *
     * @param jobName ジョブ名
     * @param limit 返却件数上限
     * @return 実行履歴またはエラー応答
     */
    @GetMapping("/jobs/{jobName}/executions")
    public ResponseEntity<?> listExecutions(@PathVariable("jobName") String jobName,
                                            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        try {
            List<BatchExecutionSummaryResponse> response = batchExecutionService.listExecutions(jobName, limit);
            return ResponseEntity.ok(response);
        } catch (UnknownBatchJobException ex) {
            log.warn("event={} job={} reason={}",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    jobName,
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BatchErrorResponse("BATCH_JOB_NOT_FOUND", ex.getMessage()));
        }
    }

    /**
     * 指定ジョブを手動起動する。
     *
     * @param jobName ジョブ名
     * @return 起動受付結果またはエラー応答
     */
    @PostMapping("/jobs/{jobName}/executions")
    public ResponseEntity<?> startJob(@PathVariable("jobName") String jobName) {
        try {
            BatchExecutionAcceptedResponse response = batchExecutionService.submitJob(jobName);
            return ResponseEntity.accepted().body(response);
        } catch (BatchAlreadyRunningException ex) {
            log.warn("event={} job={} reason={}",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    jobName,
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BatchErrorResponse("BATCH_ALREADY_RUNNING", ex.getMessage()));
        } catch (UnknownBatchJobException ex) {
            log.warn("event={} job={} reason={}",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    jobName,
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BatchErrorResponse("BATCH_JOB_NOT_FOUND", ex.getMessage()));
        } catch (RuntimeException ex) {
            log.error("event={} job={} reason=start_failed",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    jobName,
                    ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BatchErrorResponse("SERVER_ERROR", message("business.batch.startFailed")));
        }
    }

    /**
     * 指定実行IDのジョブへ停止要求を送る。
     *
     * @param executionId 実行ID
     * @return 停止受付結果またはエラー応答
     */
    @PostMapping("/executions/{executionId}/stop")
    public ResponseEntity<?> stopExecution(@PathVariable("executionId") long executionId) {
        try {
            BatchExecutionStopAcceptedResponse response = batchExecutionService.stopExecution(executionId);
            return ResponseEntity.accepted().body(response);
        } catch (BatchExecutionNotFoundException ex) {
            log.warn("event={} executionId={} reason={}",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    executionId,
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BatchErrorResponse("BATCH_EXECUTION_NOT_FOUND", ex.getMessage()));
        } catch (BatchExecutionNotRunningException ex) {
            log.warn("event={} executionId={} reason={}",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    executionId,
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BatchErrorResponse("BATCH_EXECUTION_NOT_RUNNING", ex.getMessage()));
        } catch (RuntimeException ex) {
            log.error("event={} executionId={} reason=stop_failed",
                    LogEvent.BATCH_REQUEST_REJECTED.value(),
                    executionId,
                    ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BatchErrorResponse("SERVER_ERROR", message("business.batch.stopFailed")));
        }
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

