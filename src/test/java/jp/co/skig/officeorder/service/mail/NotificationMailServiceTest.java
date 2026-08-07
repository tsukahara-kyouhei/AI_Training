package jp.co.skig.officeorder.service.mail;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationMailServiceTest {

    private final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    private final TaskScheduler mailRetryScheduler = Mockito.mock(TaskScheduler.class);
    private final MailTemplateRenderer mailTemplateRenderer = Mockito.mock(MailTemplateRenderer.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
    private NotificationMailService service;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getMail().setFrom("no-reply@example.com");
        appProperties.getMail().setReplyTo("support@example.com");
        appProperties.getMail().setSiteUrl("https://example.com");
        appProperties.getMail().setContactEmail("support@example.com");
        service = new NotificationMailService(mailSender, mailRetryScheduler, appProperties, mailTemplateRenderer,
                clock);
    }

    @Test
    void sendMemberRegistrationCompleteMail_whenMemberEmailMissing_shouldDoNothing() {
        assertDoesNotThrow(
                () -> service.sendMemberRegistrationCompleteMail(new MemberSessionUser(1L, "", "Yamada", "Taro")));
        Mockito.verifyNoInteractions(mailSender);
    }

    @Test
    void sendOrderCompleteMail_whenCustomerEmailMissing_shouldDoNothing() {
        OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                null,
                OffsetDateTime.now(clock),
                "Yamada",
                "Taro",
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of());
        assertDoesNotThrow(() -> service.sendOrderCompleteMail(payload));
        Mockito.verifyNoInteractions(mailSender);
    }

    @Test
    void sendMemberRegistrationCompleteMail_whenSendSucceeds_shouldInvokeMailSender() throws MessagingException {
        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailTemplateRenderer.renderSubject(anyString(), any())).thenReturn("subject");
        when(mailTemplateRenderer.renderBody(anyString(), any())).thenReturn("body");

        service.sendMemberRegistrationCompleteMail(new MemberSessionUser(2L, "user@example.com", "Yamada", "Taro"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderCompleteMail_whenMailSendFails_shouldScheduleRetry() throws MessagingException {
        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailTemplateRenderer.renderSubject(anyString(), any())).thenReturn("subject");
        when(mailTemplateRenderer.renderBody(anyString(), any())).thenReturn("body");
        Mockito.doThrow(new MailSendException("failed")).when(mailSender).send(mimeMessage);

        OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                "ORD20250101-000001",
                OffsetDateTime.now(clock),
                "Yamada",
                "Taro",
                "user@example.com",
                "1234567",
                "Tokyo",
                "Chiyoda",
                "1-1",
                1,
                true,
                "03-1234-5678",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of());

        assertDoesNotThrow(() -> service.sendOrderCompleteMail(payload));

        verify(mailRetryScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
    }
}
