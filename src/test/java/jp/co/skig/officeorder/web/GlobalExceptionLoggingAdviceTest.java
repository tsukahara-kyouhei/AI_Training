package jp.co.skig.officeorder.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionLoggingAdviceTest {

    private final GlobalExceptionLoggingAdvice advice = new GlobalExceptionLoggingAdvice();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionLoggingAdvice.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    /**
     * 4xx の ResponseStatusException は WARN で記録しつつ、元の例外をそのまま再送出することを確認する。
     */
    @Test
    void logUnhandledException_logsWarnForClientErrorsAndRethrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");

        assertThatThrownBy(() -> advice.logUnhandledException(request, ex))
                .isSameAs(ex);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=unhandled_exception", "path=/missing", "status=404");
    }

    /**
     * 予期しない実行時例外は ERROR とスタックトレース付きで記録し、元の例外を再送出することを確認する。
     */
    @Test
    void logUnhandledException_logsErrorForServerErrorsAndRethrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products/1");
        RuntimeException ex = new RuntimeException("boom");

        assertThatThrownBy(() -> advice.logUnhandledException(request, ex))
                .isSameAs(ex);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("event=unhandled_exception", "path=/products/1", "message=boom");
        assertThat(event.getThrowableProxy()).isNotNull();
    }
}
