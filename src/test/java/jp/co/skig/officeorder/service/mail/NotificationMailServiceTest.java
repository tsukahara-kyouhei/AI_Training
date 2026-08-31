package jp.co.skig.officeorder.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.service.mail.MailTemplateRenderer;

@ExtendWith(MockitoExtension.class)
class NotificationMailServiceTest {

        @Mock
        private JavaMailSender mailSender;

        @Mock
        private TaskScheduler mailRetryScheduler;

        @Mock
        private MailTemplateRenderer mailTemplateRenderer;

        @Mock
        private Clock appClock;

        @Mock
        private MimeMessage mimeMessage;

        private NotificationMailService service;

        @BeforeEach
        void setUp() {

                AppProperties appProperties = new AppProperties();

                appProperties.getMail().setFrom("from@example.com");
                appProperties.getMail().setReplyTo("reply@example.com");
                appProperties.getMail().setSiteUrl("https://example.com");
                appProperties.getMail().setContactEmail("contact@example.com");

                service = new NotificationMailService(
                                mailSender,
                                mailRetryScheduler,
                                appProperties,
                                mailTemplateRenderer,
                                appClock);
        }

        @Test
        void sendMemberRegistrationCompleteMail_正常系_メールを送信する()
                        throws Exception {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "user@example.com",
                                "山田",
                                "太郎");

                when(mailTemplateRenderer.renderSubject(
                                eq("member-registration.subject"),
                                anyMap()))
                                .thenReturn("会員登録完了のお知らせ");

                when(mailTemplateRenderer.renderBody(
                                eq("member-registration-body"),
                                anyMap()))
                                .thenReturn("会員登録ありがとうございます。");

                when(mailSender.createMimeMessage())
                                .thenReturn(mimeMessage);

                assertThatCode(() -> service.sendMemberRegistrationCompleteMail(member)).doesNotThrowAnyException();

                verify(mailSender)
                                .createMimeMessage();

                verify(mailSender)
                                .send(mimeMessage);

                verify(mailTemplateRenderer)
                                .renderSubject(
                                                eq("member-registration.subject"),
                                                anyMap());

                verify(mailTemplateRenderer)
                                .renderBody(
                                                eq("member-registration-body"),
                                                anyMap());
        }

        @Test
        void sendMemberRegistrationCompleteMail_異常系_会員がnullなら送信しない() {

                service.sendMemberRegistrationCompleteMail(null);

                verifyNoInteractions(mailSender);
                verifyNoInteractions(mailTemplateRenderer);
        }

        @Test
        void sendMemberRegistrationCompleteMail_異常系_メールアドレスが空なら送信しない() {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "",
                                "山田",
                                "太郎");

                service.sendMemberRegistrationCompleteMail(member);

                verifyNoInteractions(mailSender);
                verifyNoInteractions(mailTemplateRenderer);
        }

        @Test
        void sendOrderCompleteMail_正常系_メールを送信する()
                        throws Exception {

                OrderCompleteMailPayload payload = createPayload();

                when(mailTemplateRenderer.renderSubject(
                                eq("order-complete.subject"),
                                anyMap()))
                                .thenReturn("ご注文ありがとうございます");

                when(mailTemplateRenderer.renderBody(
                                eq("order-complete-body"),
                                anyMap()))
                                .thenReturn("注文内容");

                when(mailSender.createMimeMessage())
                                .thenReturn(mimeMessage);

                service.sendOrderCompleteMail(payload);

                verify(mailSender)
                                .createMimeMessage();

                verify(mailSender)
                                .send(mimeMessage);

                verify(mailTemplateRenderer)
                                .renderSubject(
                                                eq("order-complete.subject"),
                                                anyMap());

                verify(mailTemplateRenderer)
                                .renderBody(
                                                eq("order-complete-body"),
                                                anyMap());
        }

        @Test
        void sendOrderCompleteMail_異常系_payloadがnullなら送信しない() {

                service.sendOrderCompleteMail(null);

                verifyNoInteractions(mailSender);
                verifyNoInteractions(mailTemplateRenderer);
        }

        @Test
        void sendOrderCompleteMail_異常系_メールアドレスが空なら送信しない() {

                OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                                "ORD20260817-000001",
                                OffsetDateTime.now(),
                                "山田",
                                "太郎",
                                "",
                                "1000001",
                                "東京都",
                                "千代田区",
                                "1-1-1",
                                3,
                                true,
                                "0312345678",
                                BigDecimal.valueOf(10000),
                                BigDecimal.valueOf(3000),
                                BigDecimal.valueOf(800),
                                BigDecimal.valueOf(1380),
                                BigDecimal.valueOf(15180),
                                List.of());

                service.sendOrderCompleteMail(payload);

                verifyNoInteractions(mailSender);
                verifyNoInteractions(mailTemplateRenderer);
        }

        @Test
        void sendMemberRegistrationCompleteMail_メール送信失敗時は再送をスケジュールする()
                        throws Exception {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "user@example.com",
                                "山田",
                                "太郎");

                when(mailTemplateRenderer.renderSubject(
                                eq("member-registration.subject"),
                                anyMap()))
                                .thenReturn("会員登録完了");

                when(mailTemplateRenderer.renderBody(
                                eq("member-registration-body"),
                                anyMap()))
                                .thenReturn("本文");

                when(mailSender.createMimeMessage())
                                .thenReturn(mimeMessage);

                doThrow(new org.springframework.mail.MailSendException("送信失敗"))
                                .when(mailSender)
                                .send(mimeMessage);

                when(appClock.instant())
                                .thenReturn(Instant.parse("2026-08-17T01:00:00Z"));

                service.sendMemberRegistrationCompleteMail(member);

                verify(mailRetryScheduler)
                                .schedule(
                                                any(Runnable.class),
                                                eq(Instant.parse("2026-08-17T01:00:10Z")));
        }

        @Test
        void sendMemberRegistrationCompleteMail_リトライ上限では再送しない()
                        throws Exception {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "user@example.com",
                                "山田",
                                "太郎");

                when(mailTemplateRenderer.renderSubject(
                                eq("member-registration.subject"),
                                anyMap()))
                                .thenReturn("会員登録完了");

                when(mailTemplateRenderer.renderBody(
                                eq("member-registration-body"),
                                anyMap()))
                                .thenReturn("本文");

                when(mailSender.createMimeMessage())
                                .thenReturn(mimeMessage);

                doThrow(new org.springframework.mail.MailSendException("送信失敗"))
                                .when(mailSender)
                                .send(mimeMessage);

                when(appClock.instant())
                                .thenReturn(Instant.parse("2026-08-17T01:00:00Z"));

                service.sendMemberRegistrationCompleteMail(member);

                ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

                verify(mailRetryScheduler)
                                .schedule(
                                                captor.capture(),
                                                any(Instant.class));

                // 1回目の失敗 → 2回目を実行
                captor.getValue().run();

                verify(mailRetryScheduler, times(2))
                                .schedule(
                                                any(Runnable.class),
                                                any(Instant.class));
        }

        private OrderCompleteMailPayload createPayload() {

                OrderCompleteMailPayload.OrderItemLine item = new OrderCompleteMailPayload.OrderItemLine(
                                "ワークデスク",
                                "ホワイト",
                                2,
                                BigDecimal.valueOf(10000),
                                BigDecimal.valueOf(20000));

                return new OrderCompleteMailPayload(
                                "ORD20260817-000001",
                                OffsetDateTime.of(
                                                2026,
                                                8,
                                                17,
                                                10,
                                                30,
                                                0,
                                                0,
                                                ZoneOffset.ofHours(9)),
                                "山田",
                                "太郎",
                                "user@example.com",
                                "1000001",
                                "東京都",
                                "千代田区",
                                "1-1-1",
                                3,
                                true,
                                "0312345678",
                                BigDecimal.valueOf(20000),
                                BigDecimal.valueOf(6000),
                                BigDecimal.valueOf(800),
                                BigDecimal.valueOf(2680),
                                BigDecimal.valueOf(29480),
                                List.of(item));
        }
}
