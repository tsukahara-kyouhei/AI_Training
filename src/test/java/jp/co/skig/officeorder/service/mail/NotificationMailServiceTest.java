package jp.co.skig.officeorder.service.mail;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T00:00:00Z");
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TaskScheduler mailRetryScheduler;

    @Mock
    private MailTemplateRenderer mailTemplateRenderer;

    @Mock
    private AppProperties appProperties;

    private NotificationMailService sut;

    @BeforeEach
    void setUp() {
        AppProperties.Mail mailConfig = new AppProperties.Mail();
        mailConfig.setFrom("from@example.com");
        mailConfig.setReplyTo("reply@example.com");
        mailConfig.setSiteUrl("https://example.com");
        mailConfig.setContactEmail("contact@example.com");
        when(appProperties.getMail()).thenReturn(mailConfig);
        Clock clock = Clock.fixed(FIXED_INSTANT, JST);
        sut = new NotificationMailService(mailSender, mailRetryScheduler, appProperties, mailTemplateRenderer, clock);
    }

    // ─── sendMemberRegistrationCompleteMail ─────────────────────────────

    @Test
    void sendMemberRegistrationCompleteMail_null_member_skips_send() {
        // Act
        sut.sendMemberRegistrationCompleteMail(null);

        // Assert
        verify(mailTemplateRenderer, never()).renderSubject(any(), any());
    }

    @Test
    void sendMemberRegistrationCompleteMail_member_with_empty_email_skips_send() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "", "山田", "太郎");

        // Act
        sut.sendMemberRegistrationCompleteMail(member);

        // Assert
        verify(mailTemplateRenderer, never()).renderSubject(any(), any());
    }

    @Test
    void sendMemberRegistrationCompleteMail_valid_member_renders_subject_and_body() throws Exception {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("件名");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");

        jakarta.mail.internet.MimeMessage mimeMessage =
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        sut.sendMemberRegistrationCompleteMail(member);

        // Assert
        verify(mailTemplateRenderer).renderSubject(any(), any());
        verify(mailTemplateRenderer).renderBody(any(), any());
    }

    // ─── sendOrderCompleteMail ───────────────────────────────────────────

    @Test
    void sendOrderCompleteMail_null_payload_skips_send() {
        // Act
        sut.sendOrderCompleteMail(null);

        // Assert
        verify(mailTemplateRenderer, never()).renderSubject(any(), any());
    }

    @Test
    void sendOrderCompleteMail_payload_with_empty_email_skips_send() {
        // Arrange
        OrderCompleteMailPayload payload = buildPayload("");

        // Act
        sut.sendOrderCompleteMail(payload);

        // Assert
        verify(mailTemplateRenderer, never()).renderSubject(any(), any());
    }

    @Test
    void sendOrderCompleteMail_valid_payload_renders_subject_and_body() throws Exception {
        // Arrange
        OrderCompleteMailPayload payload = buildPayload("order@example.com");
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("件名");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");

        jakarta.mail.internet.MimeMessage mimeMessage =
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        sut.sendOrderCompleteMail(payload);

        // Assert
        verify(mailTemplateRenderer).renderSubject(any(), any());
        verify(mailTemplateRenderer).renderBody(any(), any());
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private OrderCompleteMailPayload buildPayload(String email) {
        return new OrderCompleteMailPayload(
                "ORD20260414-000001",
                OffsetDateTime.now(Clock.fixed(FIXED_INSTANT, JST)),
                "山田", "太郎",
                email,
                "1234567", "東京都", "渋谷区", "渋谷1-1",
                3, true, "0312345678",
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(11800),
                List.of()
        );
    }

    private OrderCompleteMailPayload buildPayloadWithItems(String email) {
        List<OrderCompleteMailPayload.OrderItemLine> items = List.of(
                new OrderCompleteMailPayload.OrderItemLine(
                        "椅子A", "ブラック", 2, BigDecimal.valueOf(30000), BigDecimal.valueOf(60000)
                ),
                new OrderCompleteMailPayload.OrderItemLine(
                        "デスクB", "ホワイト", 1, BigDecimal.valueOf(50000), BigDecimal.valueOf(50000)
                )
        );
        return new OrderCompleteMailPayload(
                "ORD20260414-000002",
                null,  // null orderDatetime to cover formatOrderDatetime(null) branch
                "田中", "花子",
                email,
                null,  // null postal code to cover formatPostalCode(empty) branch (L315)
                "大阪府", "大阪市", "梅田1-1",
                null, false, "0612345678",  // null floor to cover formatFloor(null) branch
                BigDecimal.valueOf(60000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(6000),
                BigDecimal.valueOf(66000),
                items
        );
    }

    // ─── sendOrderCompleteMail (various payload branches) ────────────────

    @Test
    void sendOrderCompleteMail_with_items_and_null_datetime_and_short_postal_covers_helper_branches() throws Exception {
        // Arrange
        OrderCompleteMailPayload payload = buildPayloadWithItems("branch@example.com");
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("件名");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");

        jakarta.mail.internet.MimeMessage mimeMessage =
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act / Assert - covers buildOrderItemLines, formatFloor(null), formatOrderDatetime(null), formatPostalCode(length≠7)
        assertThatCode(() -> sut.sendOrderCompleteMail(payload)).doesNotThrowAnyException();
    }

    // ─── mail send failure / retry ───────────────────────────────────────

    @Test
    void sendOrderCompleteMail_send_failure_schedules_retry() throws Exception {
        // Arrange
        OrderCompleteMailPayload payload = buildPayload("retry@example.com");
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("件名");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");

        jakarta.mail.internet.MimeMessage mimeMessage =
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("send failed")).when(mailSender).send(mimeMessage);

        // Act
        sut.sendOrderCompleteMail(payload);

        // Assert - retry is scheduled
        verify(mailRetryScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
    }

    @Test
    void sendOrderCompleteMail_all_retries_exhausted_logs_final_failure() throws Exception {
        // Arrange
        OrderCompleteMailPayload payload = buildPayload("exhaust@example.com");
        when(mailTemplateRenderer.renderSubject(any(), any())).thenReturn("件名");
        when(mailTemplateRenderer.renderBody(any(), any())).thenReturn("本文");

        jakarta.mail.internet.MimeMessage mimeMessage =
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("permanent failure")).when(mailSender).send(mimeMessage);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // attempt=1 (failureCount=0): 失敗 → R1をスケジュール (遅延10s)
        sut.sendOrderCompleteMail(payload);
        verify(mailRetryScheduler).schedule(runnableCaptor.capture(), any(java.time.Instant.class));
        Runnable r1 = runnableCaptor.getValue();

        // attempt=2 (failureCount=1): R1実行 → 失敗 → R2をスケジュール (遅延1m)
        org.mockito.Mockito.reset(mailRetryScheduler);
        r1.run();
        verify(mailRetryScheduler).schedule(runnableCaptor.capture(), any(java.time.Instant.class));
        Runnable r2 = runnableCaptor.getValue();

        // attempt=3 (failureCount=2): R2実行 → 失敗 → R3をスケジュール (遅延5m)
        org.mockito.Mockito.reset(mailRetryScheduler);
        r2.run();
        verify(mailRetryScheduler).schedule(runnableCaptor.capture(), any(java.time.Instant.class));
        Runnable r3 = runnableCaptor.getValue();

        // attempt=4 (failureCount=3 >= RETRY_DELAYS.size()): R3実行 → 上限超過 → スケジュールなし
        org.mockito.Mockito.reset(mailRetryScheduler);
        r3.run();
        verify(mailRetryScheduler, never()).schedule(any(Runnable.class), any(java.time.Instant.class));
    }
}
