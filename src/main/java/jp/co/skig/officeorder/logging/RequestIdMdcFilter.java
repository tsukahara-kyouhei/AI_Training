package jp.co.skig.officeorder.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * リクエストIDと基本MDC項目を設定するFilter。
 */
public class RequestIdMdcFilter extends OncePerRequestFilter {

    /** クライアントと受け渡すリクエストIDヘッダ名。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** アプリ内で requestId を参照するリクエスト属性名。 */
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdMdcFilter.class.getName() + ".requestId";
    /** 受け入れるリクエストID形式。 */
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    /**
     * リクエスト開始時に requestId / path / method をMDCへ設定する。
     *
     * @param request     現在リクエスト
     * @param response    現在レスポンス
     * @param filterChain FilterChain
     * @throws ServletException フィルタ処理失敗時
     * @throws IOException      IO失敗時
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalizeRequestId(request.getHeader(REQUEST_ID_HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(REQUEST_ID_HEADER, requestId);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        MDC.put(LoggingMdcKeys.REQUEST_ID, requestId);
        MDC.put(LoggingMdcKeys.PATH, request.getRequestURI());
        MDC.put(LoggingMdcKeys.METHOD, request.getMethod());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(LoggingMdcKeys.MEMBER_ID);
            MDC.remove(LoggingMdcKeys.METHOD);
            MDC.remove(LoggingMdcKeys.PATH);
            MDC.remove(LoggingMdcKeys.REQUEST_ID);
        }
    }

    /**
     * 外部から受け取った requestId を許容形式へ正規化する。
     *
     * @param rawRequestId 入力値
     * @return 正規化済みrequestId。無効な場合は {@code null}
     */
    private String normalizeRequestId(String rawRequestId) {
        if (rawRequestId == null || rawRequestId.isBlank()) {
            return null;
        }
        String normalized = rawRequestId.trim();
        if (!REQUEST_ID_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }
}
