package jp.co.skig.officeorder.web;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.logging.RequestIdMdcFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.webmvc.error.ErrorController;

/**
 * ブラウザ向けエラーページと API 向け汎用エラー応答を返す Controller。
 *
 * <p>予期しない例外時でもヘッダや DB 依存の共通部品を使わず、
 * 最低限の情報だけで応答を返せるようにしている。
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class AppErrorController implements ErrorController {

    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * エラーControllerを生成する。
     *
     * @param messageSource 利用者向けメッセージ取得元
     */
    public AppErrorController(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * エラー内容に応じて HTML 画面または JSON 応答を返す。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param model 画面モデル
     * @return HTML 表示時はテンプレート名、JSON 応答時は ResponseEntity
     */
    @RequestMapping
    public Object handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        ErrorDescriptor descriptor = resolveErrorDescriptor(request);
        if (expectsJson(request)) {
            return ResponseEntity.status(descriptor.status())
                    .body(buildJsonBody(request, descriptor));
        }

        response.setStatus(descriptor.status().value());
        model.addAttribute("statusCode", descriptor.status().value());
        model.addAttribute("errorTitle", descriptor.title());
        model.addAttribute("errorMessage", descriptor.message());
        model.addAttribute("requestId", resolveRequestId(request));
        model.addAttribute("errorPath", resolveErrorPath(request));
        return descriptor.viewName();
    }

    /**
     * ステータスコードから画面表示内容を決定する。
     *
     * @param request 現在リクエスト
     * @return エラー表示定義
     */
    private ErrorDescriptor resolveErrorDescriptor(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        if (status == HttpStatus.NOT_FOUND) {
            return new ErrorDescriptor(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    message("error.page.notFound.title"),
                    message("error.page.notFound.message"),
                    "error/error");
        }
        if (status.is4xxClientError()) {
            return new ErrorDescriptor(
                    status,
                    "REQUEST_ERROR",
                    message("error.page.requestError.title"),
                    message("error.page.requestError.message"),
                    "error/error");
        }
        return new ErrorDescriptor(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SERVER_ERROR",
                message("error.page.serverError.title"),
                message("error.page.serverError.message"),
                "error/500");
    }

    /**
     * エラー応答用の HTTP ステータスを取得する。
     *
     * @param request 現在リクエスト
     * @return HTTP ステータス
     */
    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object rawStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (rawStatus instanceof Integer statusCode) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * クライアントが JSON 応答を優先しているか判定する。
     *
     * @param request 現在リクエスト
     * @return JSON 応答を返すべきなら {@code true}
     */
    private boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains("application/json");
    }

    /**
     * JSON 応答本文を組み立てる。
     *
     * @param request 現在リクエスト
     * @param descriptor エラー表示定義
     * @return JSON 応答本文
     */
    private Map<String, Object> buildJsonBody(HttpServletRequest request, ErrorDescriptor descriptor) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", descriptor.status().value());
        body.put("code", descriptor.code());
        body.put("message", descriptor.title() + "。 " + descriptor.message());
        body.put("requestId", resolveRequestId(request));
        body.put("path", resolveErrorPath(request));
        return body;
    }

    /**
     * 画面表示や問い合わせ案内に使う requestId を取得する。
     *
     * @param request 現在リクエスト
     * @return requestId。未設定時は空文字
     */
    private String resolveRequestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdMdcFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "" : value.toString();
    }

    /**
     * 元のアクセスパスを取得する。
     *
     * @param request 現在リクエスト
     * @return エラー発生元のパス
     */
    private String resolveErrorPath(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return value == null ? request.getRequestURI() : value.toString();
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

    /**
     * エラー画面・API応答に使う表示定義。
     *
     * @param status HTTP ステータス
     * @param code エラーコード
     * @param title 見出し
     * @param message 補足メッセージ
     * @param viewName HTML 表示時のテンプレート名
     */
    private record ErrorDescriptor(HttpStatus status, String code, String title, String message, String viewName) {
    }
}
