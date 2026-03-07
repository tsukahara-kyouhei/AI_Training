package jp.co.skig.officeorder.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.logging.LogMaskingUtil;
import jp.co.skig.officeorder.web.auth.AuthRedirectUtils;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

/**
 * Spring Security の認証・認可・セッション動作を定義する設定。
 */
@Configuration
public class SecurityConfig {

    /** 認証系ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    /** remember-me チェックボックスのパラメータ名。 */
    private static final String REMEMBER_ME_PARAMETER = "remember_me";
    /** remember-me Cookie名。 */
    private static final String REMEMBER_ME_COOKIE_NAME = "office_order_remember_me";
    /** remember-me の有効期限。 */
    private static final int REMEMBER_ME_TOKEN_VALIDITY_SECONDS = 60 * 60 * 24 * 30;
    /** 会員専用領域のパス接頭辞。 */
    private static final String MEMBER_AREA_PATH = "/mypage";
    /** remember-me トークン署名キー。 */
    private final String rememberMeKey;

    /**
     * Security設定を生成する。
     *
     * @param appProperties 独自アプリ設定
     */
    public SecurityConfig(AppProperties appProperties) {
        this.rememberMeKey = appProperties.getSecurity().getRememberMeKey();
    }

    /**
     * パスワードハッシュ化に使うエンコーダを返す。
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認証・認可・remember-me・ログアウトのフィルタチェーンを構成する。
     *
     * @param http HttpSecurity
     * @param userDetailsService 会員認証情報取得サービス
     * @return SecurityFilterChain
     * @throws Exception 構成失敗時
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserDetailsService userDetailsService) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/internal/batch/**")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/mypage/**").hasRole("MEMBER")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(this::handleAuthRequired))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeParameter(REMEMBER_ME_PARAMETER)
                        .rememberMeCookieName(REMEMBER_ME_COOKIE_NAME)
                        .tokenValiditySeconds(REMEMBER_ME_TOKEN_VALIDITY_SECONDS)
                        .key(rememberMeKey)
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", REMEMBER_ME_COOKIE_NAME)
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .invalidSessionStrategy(this::handleInvalidSession)
                );
        return http.build();
    }

    /**
     * ログイン成功時のリダイレクト先決定とログ出力を行うハンドラ。
     *
     * @return AuthenticationSuccessHandler
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectPath = AuthRedirectUtils.sanitizeRedirectPath(request.getParameter("redirectPath"));
            if (!StringUtils.hasText(redirectPath)) {
                redirectPath = "/mypage/orders";
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof MemberPrincipal memberPrincipal) {
                log.info("event={} memberId={} redirectPath={}",
                        LogEvent.AUTH_LOGIN_SUCCESS.value(),
                        memberPrincipal.getMemberId(),
                        redirectPath);
            } else {
                log.info("event={} redirectPath={}", LogEvent.AUTH_LOGIN_SUCCESS.value(), redirectPath);
            }
            response.sendRedirect(request.getContextPath() + redirectPath);
        };
    }

    /**
     * ログイン失敗時の画面遷移とログ出力を行うハンドラ。
     *
     * @return AuthenticationFailureHandler
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String redirectPath = AuthRedirectUtils.sanitizeRedirectPath(request.getParameter("redirectPath"));
            StringBuilder location = new StringBuilder(request.getContextPath()).append("/login?error=true");
            if (StringUtils.hasText(redirectPath)) {
                location.append("&redirect=").append(UriUtils.encodeQueryParam(redirectPath, StandardCharsets.UTF_8));
            }
            String maskedEmail = LogMaskingUtil.maskEmail(request.getParameter("email"));
            log.warn("event={} email={} reason={}",
                    LogEvent.AUTH_LOGIN_FAILURE.value(),
                    maskedEmail,
                    exception.getClass().getSimpleName());
            response.sendRedirect(location.toString());
        };
    }

    /**
     * 認証必須ページへの未認証アクセス時にログイン画面へ誘導する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @param ex 認証例外
     * @throws IOException リダイレクト失敗時
     */
    private void handleAuthRequired(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        String requestPath = buildRequestPathWithQuery(request);
        String redirectPath = UriUtils.encodeQueryParam(requestPath, StandardCharsets.UTF_8);
        log.info("event={} path={}", LogEvent.AUTH_REQUIRED_REDIRECT.value(), requestPath);
        response.sendRedirect(request.getContextPath() + "/login?redirect=" + redirectPath);
    }

    /**
     * 無効セッション検出時のリダイレクトを制御する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     * @throws IOException リダイレクト失敗時
     */
    private void handleInvalidSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
        expireSessionCookie(request, response);
        String requestPath = buildRequestPathWithQuery(request);
        if (isMemberAreaRequest(request)) {
            String redirectPath = UriUtils.encodeQueryParam(requestPath, StandardCharsets.UTF_8);
            log.info("event={} path={} reason=invalid_session_member_area",
                    LogEvent.AUTH_REQUIRED_REDIRECT.value(),
                    requestPath);
            response.sendRedirect(request.getContextPath() + "/login?expired=true&redirect=" + redirectPath);
            return;
        }
        log.info("event={} path={} reason=invalid_session_public_area",
                LogEvent.SESSION_CLEARED.value(),
                requestPath);
        response.sendRedirect(requestPath);
    }

    /**
     * 現在リクエストが会員専用領域か判定する。
     *
     * @param request 現在リクエスト
     * @return 会員専用領域なら {@code true}
     */
    private boolean isMemberAreaRequest(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String prefix = (contextPath == null ? "" : contextPath) + MEMBER_AREA_PATH;
        String path = request.getRequestURI();
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    /**
     * クエリ文字列を含めた現在リクエストパスを組み立てる。
     *
     * @param request 現在リクエスト
     * @return リクエストパス
     */
    private String buildRequestPathWithQuery(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            requestPath = requestPath + "?" + request.getQueryString();
        }
        return requestPath;
    }

    /**
     * JSESSIONID Cookie を明示的に破棄する。
     *
     * @param request 現在リクエスト
     * @param response 現在レスポンス
     */
    private void expireSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie expired = new Cookie("JSESSIONID", "");
        String contextPath = request.getContextPath();
        expired.setPath(StringUtils.hasText(contextPath) ? contextPath : "/");
        expired.setMaxAge(0);
        expired.setHttpOnly(true);
        expired.setSecure(request.isSecure());
        response.addCookie(expired);
    }
}
