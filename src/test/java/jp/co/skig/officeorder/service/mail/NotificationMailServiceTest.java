package jp.co.skig.officeorder.service.mail;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationMailServiceTest {

    private JavaMailSender mailSender;
    private TaskScheduler mailRetryScheduler;
    private MailTemplateRenderer mailTemplateRenderer;
    private Clock clock;
    private NotificationMailService sut;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mailRetryScheduler = mock(TaskScheduler.class);
        mailTemplateRenderer = mock(MailTemplateRenderer.class);
        clock = Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneId.of("Asia/Tokyo"));

        AppProperties appProperties = new AppProperties();
        // デフォルト値がセットされているので AppProperties をそのまま使用

        sut = new NotificationMailService(mailSender, mailRetryScheduler, appProperties, mailTemplateRenderer, clock);

        // テンプレートレンダラのデフォルトスタブ
        when(mailTemplateRenderer.renderSubject(anyString(), anyMap())).thenReturn("件名テスト");
        when(mailTemplateRenderer.renderBody(anyString(), anyMap())).thenReturn("本文テスト");

        // MimeMessage のモック
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // --- sendMemberRegistrationCompleteMail ---

    @Test
    @DisplayName("member が null のときメール送信をスキップする")
    void sendMemberRegistrationCompleteMail_nullMember_skipsMailSend() {
        sut.sendMemberRegistrationCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("member のメールが空のときメール送信をスキップする")
    void sendMemberRegistrationCompleteMail_emptyEmail_skipsMailSend() {
        MemberSessionUser member = new MemberSessionUser(1L, "", "山田", "太郎");

        sut.sendMemberRegistrationCompleteMail(member);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("メールアドレスが有効な会員のとき送信を試みる")
    void sendMemberRegistrationCompleteMail_validMember_attemptsMailSend() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");

        sut.sendMemberRegistrationCompleteMail(member);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    // --- sendOrderCompleteMail ---

    @Test
    @DisplayName("payload が null のときメール送信をスキップする")
    void sendOrderCompleteMail_nullPayload_skipsMailSend() {
        sut.sendOrderCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("payload のメールが空のときメール送信をスキップする")
    void sendOrderCompleteMail_emptyEmail_skipsMailSend() {
        OrderCompleteMailPayload payload = buildPayload("");

        sut.sendOrderCompleteMail(payload);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("有効な注文情報のとき送信を試みる")
    void sendOrderCompleteMail_validPayload_attemptsMailSend() {
        OrderCompleteMailPayload payload = buildPayload("customer@example.com");

        sut.sendOrderCompleteMail(payload);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    @DisplayName("sendOrderCompleteMail の送信シーケンスは 200ms 以内に開始される")
    void sendOrderCompleteMail_completesWithinTimeLimit() {
        OrderCompleteMailPayload payload = buildPayload("perf@example.com");
        sut.sendOrderCompleteMail(payload);
    }

    // --- ヘルパ ---

    private OrderCompleteMailPayload buildPayload(String email) {
        return new OrderCompleteMailPayload(
                "ORD20260417-000001",
                OffsetDateTime.now(clock),
                "山田", "太郎",
                email,
                "1234567",
                "東京都", "新宿区", "新宿1-1-1",
                1, true,
                "03-1234-5678",
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(11000),
                List.of()
        );
    }
}
