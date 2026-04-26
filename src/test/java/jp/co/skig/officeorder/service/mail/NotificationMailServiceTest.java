package jp.co.skig.officeorder.service.mail;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    TaskScheduler mailRetryScheduler;

    @Mock
    MailTemplateRenderer mailTemplateRenderer;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.ofHours(9));

    NotificationMailService notificationMailService;

    @BeforeEach
    void setUp() {
        // AppProperties はデフォルト値を持つため、そのまま使用できる
        AppProperties appProperties = new AppProperties();
        notificationMailService = new NotificationMailService(
                mailSender, mailRetryScheduler, appProperties, mailTemplateRenderer, fixedClock);
    }

    // --- sendMemberRegistrationCompleteMail 早期リターン ---

    @Test
    void sendMemberRegistrationCompleteMail_nullMember_doesNotSendMail() {
        notificationMailService.sendMemberRegistrationCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendMemberRegistrationCompleteMail_memberWithNullEmail_doesNotSendMail() {
        MemberSessionUser member = new MemberSessionUser(1L, null, "山田", "太郎");

        notificationMailService.sendMemberRegistrationCompleteMail(member);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendMemberRegistrationCompleteMail_memberWithBlankEmail_doesNotSendMail() {
        MemberSessionUser member = new MemberSessionUser(1L, "  ", "山田", "太郎");

        notificationMailService.sendMemberRegistrationCompleteMail(member);

        verify(mailSender, never()).createMimeMessage();
    }

    // --- sendOrderCompleteMail 早期リターン ---

    @Test
    void sendOrderCompleteMail_nullPayload_doesNotSendMail() {
        notificationMailService.sendOrderCompleteMail(null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendOrderCompleteMail_blankCustomerEmail_doesNotSendMail() {
        OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                "ORD-001", null, "山田", "太郎", "  ",
                null, null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of());

        notificationMailService.sendOrderCompleteMail(payload);

        verify(mailSender, never()).createMimeMessage();
    }
}
