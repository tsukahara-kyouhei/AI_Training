package jp.co.skig.officeorder.service.mail;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationMailService} の単体テスト。
 *
 * <p>テスト戦略: {@link MailTemplateRenderer} もモック化し、
 * {@link NotificationMailService} の責務（送信判定・再送スケジュール）に集中する。
 */
@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    TaskScheduler mailRetryScheduler;

    @Mock
    MailTemplateRenderer mailTemplateRenderer;

    NotificationMailService sut;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getMail().setFrom("no-reply@example.com");
        props.getMail().setReplyTo("support@example.com");
        props.getMail().setSiteUrl("http://localhost:8080/");
        props.getMail().setContactEmail("support@example.com");
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        sut = new NotificationMailService(mailSender, mailRetryScheduler, props, mailTemplateRenderer, clock);
    }

    // ── sendMemberRegistrationCompleteMail ────────────────────────────────

    @Test
    void 会員登録完了メールが正常に送信されること() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("登録完了");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");

        sut.sendMemberRegistrationCompleteMail(member);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void nullの会員を渡すと送信せずreturnすること() {
        sut.sendMemberRegistrationCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void メールアドレスが空の会員を渡すと送信せずreturnすること() {
        var member = new MemberSessionUser(1L, "", "山田", "太郎");

        sut.sendMemberRegistrationCompleteMail(member);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void メール送信失敗時に再送がスケジュールされること() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("登録完了");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");
        doThrow(new MailSendException("test failure")).when(mailSender).send(mimeMessage);
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");

        sut.sendMemberRegistrationCompleteMail(member);

        verify(mailRetryScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    // ── sendOrderCompleteMail ─────────────────────────────────────────────

    @Test
    void nullのpayloadを渡すと送信せずreturnすること() {
        sut.sendOrderCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void メールアドレスが空のpayloadを渡すと送信せずreturnすること() {
        var payload = new jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload(
                "ORD20250101-000001",
                java.time.OffsetDateTime.now(),
                "山田", "太郎",
                "",   // empty email
                "1000001", "東京都", "千代田区", "千代田1-1", 1, Boolean.TRUE,
                "0312345678",
                new java.math.BigDecimal("10000"),
                java.math.BigDecimal.ZERO,
                new java.math.BigDecimal("800"),
                new java.math.BigDecimal("1080"),
                new java.math.BigDecimal("11880"),
                java.util.List.of()
        );

        sut.sendOrderCompleteMail(payload);

        verify(mailSender, never()).createMimeMessage();
    }
}
