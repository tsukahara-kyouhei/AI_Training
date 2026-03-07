package jp.co.skig.officeorder.service.mail;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.TaskScheduler;

import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationMailServiceTest {

    private JavaMailSender mailSender;
    private TaskScheduler mailRetryScheduler;
    private NotificationMailService notificationMailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mailRetryScheduler = mock(TaskScheduler.class);

        JavaMailSenderImpl delegate = new JavaMailSenderImpl();
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> delegate.createMimeMessage());

        AppProperties appProperties = new AppProperties();
        appProperties.getMail().setFrom("no-reply@test.local");
        appProperties.getMail().setReplyTo("support@test.local");
        appProperties.getMail().setSiteUrl("http://localhost:8080/");
        appProperties.getMail().setContactEmail("support@test.local");

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-07T10:15:30Z"), ZoneId.of("Asia/Tokyo"));
        MailTemplateRenderer mailTemplateRenderer = new MailTemplateRenderer(new DefaultResourceLoader());
        notificationMailService = new NotificationMailService(
                mailSender,
                mailRetryScheduler,
                appProperties,
                mailTemplateRenderer,
                fixedClock
        );
    }

    /**
     * 会員登録完了メール送信時は、テンプレートから件名・本文を描画し、謝辞文言も本文テンプレート内の固定文言として送信されることを確認する。
     */
    @Test
    void sendMemberRegistrationCompleteMail_rendersTemplateBasedMailBody() throws Exception {
        notificationMailService.sendMemberRegistrationCompleteMail(
                memberSessionUser(7L, "member@example.com", "山田", "花子")
        );

        MimeMessage message = captureSentMail();
        assertThat(message.getSubject()).isEqualTo("【OFFICE ORDER】会員登録完了のお知らせ");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("member@example.com");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("support@test.local");
        assertThat(message.getContent().toString())
                .contains("山田 花子 様")
                .contains("会員ID: MEM0000007")
                .contains("ご登録ありがとうございます。")
                .contains("http://localhost:8080/")
                .contains("support@test.local");
        verifyNoInteractions(mailRetryScheduler);
    }

    /**
     * 注文完了メール送信時は、注文番号を件名へ差し込み、注文明細・金額内訳・問い合わせ先を本文テンプレート経由で描画することを確認する。
     */
    @Test
    void sendOrderCompleteMail_rendersTemplateBasedOrderMail() throws Exception {
        OrderCompleteMailPayload payload = new OrderCompleteMailPayload(
                "ORD20260307-000123",
                OffsetDateTime.parse("2026-03-07T19:05:00+09:00"),
                "山田",
                "太郎",
                "customer@example.com",
                "1010001",
                "東京都",
                "千代田区",
                "丸の内1-1-1",
                5,
                true,
                "0312345678",
                new BigDecimal("78000"),
                new BigDecimal("3000"),
                new BigDecimal("0"),
                new BigDecimal("8100"),
                new BigDecimal("89100"),
                List.of(
                        new OrderCompleteMailPayload.OrderItemLine(
                                "Nordis ワークデスク 幅140cm",
                                "ホワイト",
                                2,
                                new BigDecimal("39000"),
                                new BigDecimal("78000")
                        )
                )
        );

        notificationMailService.sendOrderCompleteMail(payload);

        MimeMessage message = captureSentMail();
        assertThat(message.getSubject()).isEqualTo("【OFFICE ORDER】ご注文ありがとうございます（注文番号: ORD20260307-000123）");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("customer@example.com");
        assertThat(message.getContent().toString())
                .contains("注文番号: ORD20260307-000123")
                .contains("注文日時: 2026-03-07 19:05:00")
                .contains("氏名: 山田 太郎")
                .contains("住所: 〒101-0001 東京都千代田区丸の内1-1-1")
                .contains("エレベーター: あり")
                .contains("- Nordis ワークデスク 幅140cm / ホワイト / 数量: 2 / 単価: 39,000円 / 小計: 78,000円")
                .contains("商品小計: 78,000円")
                .contains("組立・設置費: 3,000円")
                .contains("合計: 89,100円（税込）")
                .contains("support@test.local");
    }

    /**
     * 送信APIへ渡された MimeMessage を取得する。
     *
     * @return 送信対象の MimeMessage
     */
    private MimeMessage captureSentMail() {
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        return messageCaptor.getValue();
    }
}
