package jp.co.skig.officeorder.web;

import jakarta.servlet.http.HttpServletRequest;
import jp.co.skig.officeorder.logging.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * 未処理例外を共通フォーマットで記録する ControllerAdvice。
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionLoggingAdvice {

    /** 例外ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionLoggingAdvice.class);

    /**
     * Controllerで未処理の例外を記録し、元例外を再送出する。
     *
     * @param request 対象リクエスト
     * @param ex      発生例外
     * @throws Exception 元例外
     */
    @ExceptionHandler(Exception.class)
    public void logUnhandledException(HttpServletRequest request, Exception ex) throws Exception {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode().is4xxClientError()) {
            log.warn("event={} path={} status={} message={}",
                    LogEvent.UNHANDLED_EXCEPTION.value(),
                    request.getRequestURI(),
                    responseStatusException.getStatusCode().value(),
                    responseStatusException.getReason());
            throw ex;
        }
        log.error("event={} path={} message={}",
                LogEvent.UNHANDLED_EXCEPTION.value(),
                request.getRequestURI(),
                ex.getMessage(),
                ex);
        throw ex;
    }
}
