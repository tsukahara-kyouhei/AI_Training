package jp.co.skig.officeorder.service.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TaskScheduler mailRetryScheduler;

    @Mock
    private MailTemplateRenderer mailTemplateRenderer;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Mail appMailProperties;

    private Clock appClock;
    private NotificationMailService notificationMailService;

    @BeforeEach
    void setUp() {
        when(appMailProperties.getFrom()).thenReturn("from@example.com");
        when(appMailProperties.getReplyTo()).thenReturn("replyto@example.com");
        when(appMailProperties.getSiteUrl()).thenReturn("https://example.com");
        when(appMailProperties.getContactEmail()).thenReturn("contact@example.com");
        when(appProperties.getMail()).thenReturn(appMailProperties);

        appClock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneId.of("Asia/Tokyo"));

        notificationMailService = new NotificationMailService(
                mailSender,
                mailRetryScheduler,
                appProperties,
                mailTemplateRenderer,
                appClock
        );
    }

    @Nested
    @DisplayName("sendMemberRegistrationCompleteMailのテスト")
    class SendMemberRegistrationCompleteMailTest {

        @Test
        @DisplayName("会員情報がnullまたはメールアドレスが空の場合は送信をスキップすること")
        void shouldSkipWhenMemberOrEmailIsEmpty() {
            notificationMailService.sendMemberRegistrationCompleteMail(null);
            
            MemberSessionUser emptyEmailMember = new MemberSessionUser(1L, "", "テスト", "太郎");
            notificationMailService.sendMemberRegistrationCompleteMail(emptyEmailMember);

            verify(mailSender, never()).createMimeMessage();
        }

        @Test
        @DisplayName("正常に会員登録完了メールを送信できること")
        void shouldSendMemberRegistrationMailSuccessfully() {
            MemberSessionUser member = new MemberSessionUser(1L, "user@example.com", "山田", "太郎");
            MimeMessage mimeMessage = mock(MimeMessage.class);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(mailTemplateRenderer.renderSubject(eq("member-registration.subject"), anyMap()))
                    .thenReturn("会員登録完了のお知らせ");
            when(mailTemplateRenderer.renderBody(eq("member-registration-body"), anyMap()))
                    .thenReturn("登録完了本文");

            notificationMailService.sendMemberRegistrationCompleteMail(member);

            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("sendOrderCompleteMailのテスト")
    class SendOrderCompleteMailTest {

        @Test
        @DisplayName("ペイロードがnullまたはメールアドレスが空の場合は送信をスキップすること")
        void shouldSkipWhenPayloadOrEmailIsEmpty() {
            notificationMailService.sendOrderCompleteMail(null);

            verify(mailSender, never()).createMimeMessage();
        }

        @Test
        @DisplayName("正常に注文完了メールを送信できること")
        void shouldSendOrderCompleteMailSuccessfully() {
            OrderCompleteMailPayload.OrderItemLine item = new OrderCompleteMailPayload.OrderItemLine(
                    "オフィスチェア", "ブラック", 2, new BigDecimal("20000"), new BigDecimal("40000")
            );

            OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                    "ORD-20260817-001",            // 1. orderNumber
                    OffsetDateTime.now(appClock),  // 2. orderDatetime
                    "山田",                        // 3. customerLastName
                    "太郎",                        // 4. customerFirstName
                    "user@example.com",           // 5. customerEmail
                    "1000001",                     // 6. shippingPostalCode
                    "東京都",                      // 7. shippingPrefecture
                    "千代田区",                    // 8. shippingCity
                    "丸の内1-1-1",                 // 9. shippingAddressLine
                    3,                             // 10. shippingFloor
                    true,                          // 11. shippingHasElevator
                    "090-0000-0000",              // 12. daytimePhone
                    new BigDecimal("40000"),       // 13. subtotalAmount
                    new BigDecimal("1000"),        // 14. assemblyFeeTotal
                    new BigDecimal("500"),         // 15. shippingFee
                    new BigDecimal("4150"),        // 16. taxAmount
                    new BigDecimal("45650"),       // 17. totalAmount
                    List.of(item)                  // 18. orderItems
            );

            MimeMessage mimeMessage = mock(MimeMessage.class);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(mailTemplateRenderer.renderSubject(eq("order-complete.subject"), anyMap()))
                    .thenReturn("注文完了のお知らせ");
            when(mailTemplateRenderer.renderBody(eq("order-complete-body"), anyMap()))
                    .thenReturn("注文完了本文");

            notificationMailService.sendOrderCompleteMail(payload);

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("送信失敗時にリトライがスケジュールされること")
        void shouldScheduleRetryWhenMailSendFails() {
            MemberSessionUser member = new MemberSessionUser(1L, "user@example.com", "山田", "太郎");
            MimeMessage mimeMessage = mock(MimeMessage.class);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(mailTemplateRenderer.renderSubject(any(), anyMap())).thenReturn("件名");
            when(mailTemplateRenderer.renderBody(any(), anyMap())).thenReturn("本文");
            doThrow(new MailSendException("送信エラー")).when(mailSender).send(mimeMessage);

            notificationMailService.sendMemberRegistrationCompleteMail(member);

            verify(mailRetryScheduler).schedule(any(Runnable.class), any(Instant.class));
        }
    }
}