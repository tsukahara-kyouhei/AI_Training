package jp.co.skig.officeorder.service.mail;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jp.co.skig.officeorder.common.MoneyFormatter;
import jp.co.skig.officeorder.config.AppProperties;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.logging.LogMaskingUtil;
import jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 顧客向け通知メールを送信するサービス。
 *
 * <p>
 * 会員登録完了メールと注文完了メールを対象とし、
 * 一時的な送信失敗時は一定回数まで遅延再送する。
 */
@Service
public class NotificationMailService {

    /** メール送信ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(NotificationMailService.class);
    /** 再送試行までの待機時間。 */
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofSeconds(10),
            Duration.ofMinutes(1),
            Duration.ofMinutes(5));
    /** 注文メール本文に記載する日時フォーマット。 */
    private static final DateTimeFormatter ORDER_DATETIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 実メール送信を担当する MailSender。 */
    private final JavaMailSender mailSender;
    /** 再送試行をスケジュールする TaskScheduler。 */
    private final TaskScheduler mailRetryScheduler;
    /** 送信元メールアドレス。 */
    private final String fromAddress;
    /** 返信先メールアドレス。 */
    private final String replyToAddress;
    /** サイトURL。 */
    private final String siteUrl;
    /** 問い合わせ先メールアドレス。 */
    private final String contactEmail;
    /** メール件名・本文テンプレートを描画するレンダラ。 */
    private final MailTemplateRenderer mailTemplateRenderer;
    /** 再送時刻計算に使う Clock。 */
    private final Clock appClock;

    /**
     * 通知メールサービスを生成する。
     *
     * @param mailSender           メール送信コンポーネント
     * @param mailRetryScheduler   再送スケジューラ
     * @param appProperties        独自アプリ設定
     * @param mailTemplateRenderer 件名・本文テンプレートレンダラ
     * @param appClock             再送時刻計算に使う Clock
     */
    public NotificationMailService(JavaMailSender mailSender,
            @Qualifier("mailRetryScheduler") TaskScheduler mailRetryScheduler,
            AppProperties appProperties,
            MailTemplateRenderer mailTemplateRenderer,
            Clock appClock) {
        this.mailSender = mailSender;
        this.mailRetryScheduler = mailRetryScheduler;
        this.fromAddress = appProperties.getMail().getFrom();
        this.replyToAddress = appProperties.getMail().getReplyTo();
        this.siteUrl = appProperties.getMail().getSiteUrl();
        this.contactEmail = appProperties.getMail().getContactEmail();
        this.mailTemplateRenderer = mailTemplateRenderer;
        this.appClock = appClock;
    }

    /**
     * 会員登録完了メールを送信する。
     *
     * @param member 登録完了した会員情報
     */
    public void sendMemberRegistrationCompleteMail(MemberSessionUser member) {
        if (member == null || !StringUtils.hasText(member.email())) {
            return;
        }
        String subject = mailTemplateRenderer.renderSubject("member-registration.subject", Map.of());
        String body = buildMemberRegistrationBody(member);
        sendWithRetry("member-registration", member.email(), subject, body);
    }

    /**
     * 注文完了メールを送信する。
     *
     * @param payload 注文完了メール本文生成に必要な情報
     */
    public void sendOrderCompleteMail(OrderCompleteMailPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.customerEmail())) {
            return;
        }
        String subject = mailTemplateRenderer.renderSubject(
                "order-complete.subject",
                Map.of("order_number", nullToDash(payload.orderNumber())));
        String body = buildOrderCompleteBody(payload);
        sendWithRetry("order-complete", payload.customerEmail(), subject, body);
    }

    /**
     * 宛先が有効な場合だけ再送付きメール送信を開始する。
     *
     * @param mailType メール種別
     * @param to       宛先メールアドレス
     * @param subject  件名
     * @param body     本文
     */
    private void sendWithRetry(String mailType, String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            log.warn("event={} type={} reason=empty_recipient",
                    LogEvent.MAIL_SEND_SKIPPED.value(),
                    mailType);
            return;
        }
        sendAttempt(mailType, to, subject, body, 0);
    }

    /**
     * メール送信を実行し、失敗時は再送回数上限まで遅延再試行する。
     *
     * @param mailType     メール種別
     * @param to           宛先メールアドレス
     * @param subject      件名
     * @param body         本文
     * @param failureCount これまでの失敗回数
     */
    private void sendAttempt(String mailType, String to, String subject, String body, int failureCount) {
        int attemptNumber = failureCount + 1;
        String maskedTo = LogMaskingUtil.maskEmail(to);
        try {
            sendPlainTextMail(to, subject, body);
            log.info("event={} type={} to={} attempt={}",
                    LogEvent.MAIL_SEND_SUCCESS.value(),
                    mailType,
                    maskedTo,
                    attemptNumber);
        } catch (MailException | MessagingException ex) {
            if (failureCount >= RETRY_DELAYS.size()) {
                log.error("event={} type={} to={} attempts={} reason=retry_exhausted",
                        LogEvent.MAIL_SEND_FAILED_FINAL.value(),
                        mailType,
                        maskedTo,
                        attemptNumber,
                        ex);
                return;
            }
            Duration delay = RETRY_DELAYS.get(failureCount);
            Instant nextAttemptAt = Instant.now(appClock).plus(delay);
            int nextAttemptNumber = attemptNumber + 1;
            log.warn("event={} type={} to={} attempt={} retryAfterSec={} nextAttempt={} reason=send_failed",
                    LogEvent.MAIL_SEND_RETRY.value(),
                    mailType,
                    maskedTo,
                    attemptNumber,
                    delay.toSeconds(),
                    nextAttemptNumber,
                    ex);
            mailRetryScheduler.schedule(
                    () -> sendAttempt(mailType, to, subject, body, failureCount + 1),
                    nextAttemptAt);
        }
    }

    /**
     * テキストメールを1通送信する。
     *
     * @param to      宛先メールアドレス
     * @param subject 件名
     * @param body    本文
     * @throws MessagingException メール構築に失敗した場合
     */
    private void sendPlainTextMail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(fromAddress);
        helper.setReplyTo(replyToAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(message);
    }

    /**
     * 会員登録完了メール本文を組み立てる。
     *
     * @param member 会員情報
     * @return メール本文
     */
    private String buildMemberRegistrationBody(MemberSessionUser member) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("last_name", nullToEmpty(member.lastName()));
        variables.put("first_name", nullToEmpty(member.firstName()));
        variables.put("member_id", formatMemberCode(member.memberId()));
        variables.put("site_url", siteUrl);
        variables.put("contact_email", contactEmail);
        return mailTemplateRenderer.renderBody("member-registration-body", variables);
    }

    /**
     * 注文完了メール本文を組み立てる。
     *
     * @param payload 注文完了メール用情報
     * @return メール本文
     */
    private String buildOrderCompleteBody(OrderCompleteMailPayload payload) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("customer_last_name", nullToEmpty(payload.customerLastName()));
        variables.put("customer_first_name", nullToEmpty(payload.customerFirstName()));
        variables.put("order_number", nullToDash(payload.orderNumber()));
        variables.put("order_datetime", formatOrderDatetime(payload.orderDatetime()));
        variables.put("shipping_name", buildShippingName(payload));
        variables.put("shipping_postal_code", formatPostalCode(payload.shippingPostalCode()));
        variables.put("shipping_prefecture", nullToEmpty(payload.shippingPrefecture()));
        variables.put("shipping_city", nullToEmpty(payload.shippingCity()));
        variables.put("shipping_address_line", nullToEmpty(payload.shippingAddressLine()));
        variables.put("shipping_floor", formatFloor(payload.shippingFloor()));
        variables.put("shipping_has_elevator", Boolean.TRUE.equals(payload.shippingHasElevator()) ? "あり" : "なし");
        variables.put("daytime_phone", nullToDash(payload.daytimePhone()));
        variables.put("order_items_lines", buildOrderItemLines(payload));
        variables.put("subtotal_amount", MoneyFormatter.formatYen(payload.subtotalAmount()));
        variables.put("assembly_fee_total", MoneyFormatter.formatYen(payload.assemblyFeeTotal()));
        variables.put("shipping_fee", MoneyFormatter.formatYen(payload.shippingFee()));
        variables.put("tax_amount", MoneyFormatter.formatYen(payload.taxAmount()));
        variables.put("total_amount", MoneyFormatter.formatYen(payload.totalAmount()));
        variables.put("contact_email", contactEmail);
        return mailTemplateRenderer.renderBody("order-complete-body", variables);
    }

    /**
     * 注文完了メールの明細表示部を組み立てる。
     *
     * @param payload 注文完了メール用情報
     * @return 改行を含む明細表示文字列
     */
    private String buildOrderItemLines(OrderCompleteMailPayload payload) {
        if (payload.orderItems() == null || payload.orderItems().isEmpty()) {
            return "- 明細なし";
        }

        StringBuilder lines = new StringBuilder();
        for (OrderCompleteMailPayload.OrderItemLine item : payload.orderItems()) {
            if (lines.length() > 0) {
                lines.append('\n');
            }
            lines.append("- ")
                    .append(nullToDash(item.productName()))
                    .append(" / ")
                    .append(nullToDash(item.colorName()))
                    .append(" / 数量: ")
                    .append(item.quantity())
                    .append(" / 単価: ")
                    .append(MoneyFormatter.formatYen(item.unitPrice()))
                    .append("円 / 小計: ")
                    .append(MoneyFormatter.formatYen(item.lineSubtotal()))
                    .append("円");
        }
        return lines.toString();
    }

    /**
     * お届け情報の氏名表示を組み立てる。
     *
     * @param payload 注文完了メール用情報
     * @return 表示用氏名
     */
    private String buildShippingName(OrderCompleteMailPayload payload) {
        String lastName = nullToEmpty(payload.customerLastName()).trim();
        String firstName = nullToEmpty(payload.customerFirstName()).trim();
        String fullName = (lastName + " " + firstName).trim();
        return fullName.isEmpty() ? "-" : fullName;
    }

    /**
     * 会員IDをメール表示用コードへ変換する。
     *
     * @param memberId 会員ID
     * @return 会員コード
     */
    private String formatMemberCode(long memberId) {
        return String.format("MEM%07d", memberId);
    }

    /**
     * 郵便番号を表示用に整形する。
     *
     * @param postalCode 郵便番号
     * @return 表示用郵便番号
     */
    private String formatPostalCode(String postalCode) {
        if (!StringUtils.hasText(postalCode)) {
            return "-";
        }
        String value = postalCode.trim();
        if (value.length() == 7) {
            return value.substring(0, 3) + "-" + value.substring(3);
        }
        return value;
    }

    /**
     * 階数表示用文字列を返す。
     *
     * @param floor 階数
     * @return 表示用階数
     */
    private String formatFloor(Integer floor) {
        if (floor == null) {
            return "-";
        }
        return floor + "階";
    }

    /**
     * 注文日時をメール本文用に整形する。
     *
     * @param orderDatetime 注文日時
     * @return 表示用日時
     */
    private String formatOrderDatetime(OffsetDateTime orderDatetime) {
        if (orderDatetime == null) {
            return "-";
        }
        return ORDER_DATETIME_FORMATTER.format(orderDatetime);
    }

    /**
     * 値が空の場合はダッシュへ置き換える。
     *
     * @param value 変換対象
     * @return 変換後文字列
     */
    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    /**
     * 値が {@code null} の場合は空文字を返す。
     *
     * @param value 変換対象
     * @return 変換後文字列
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

}
