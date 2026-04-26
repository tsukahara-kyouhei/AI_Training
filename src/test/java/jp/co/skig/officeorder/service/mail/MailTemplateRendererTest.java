package jp.co.skig.officeorder.service.mail;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MailTemplateRenderer} の単体テスト。
 *
 * <p>テンプレートファイルがクラスパス（src/main/resources）に存在するため、
 * Spring コンテキストをスライスして ResourceLoader を取得する。
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE,
        classes = MailTemplateRenderer.class)
class MailTemplateRendererTest {

    @Autowired
    MailTemplateRenderer sut;

    // ── renderSubject ────────────────────────────────────────────────────

    @Test
    void 会員登録件名テンプレートをプレースホルダなしで展開できること() {
        var result = sut.renderSubject("member-registration.subject", Map.of());

        assertThat(result).isEqualTo("【OFFICE ORDER】会員登録完了のお知らせ");
    }

    @Test
    void 注文完了件名テンプレートをプレースホルダ込みで展開できること() {
        var result = sut.renderSubject("order-complete.subject", Map.of("order_number", "ORD20260101-000001"));

        assertThat(result).isEqualTo("【OFFICE ORDER】ご注文ありがとうございます（注文番号: ORD20260101-000001）");
    }

    @Test
    void 存在しない件名テンプレートキーを指定するとIllegalStateExceptionをスローすること() {
        assertThatThrownBy(() -> sut.renderSubject("no.such.key", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no.such.key");
    }

    // ── renderBody ───────────────────────────────────────────────────────

    @Test
    void 会員登録本文テンプレートをプレースホルダ込みで展開できること() {
        var vars = Map.of(
                "last_name", "山田",
                "first_name", "太郎",
                "member_id", "MEM0000001",
                "site_url", "https://example.com",
                "contact_email", "contact@example.com"
        );

        var result = sut.renderBody("member-registration-body", vars);

        assertThat(result).contains("山田 太郎 様");
        assertThat(result).contains("会員ID: MEM0000001");
        assertThat(result).contains("https://example.com");
        assertThat(result).contains("contact@example.com");
    }

    @Test
    void 存在しない本文テンプレートを指定するとIllegalStateExceptionをスローすること() {
        assertThatThrownBy(() -> sut.renderBody("no-such-template", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void テンプレート内の未定義プレースホルダがあるとIllegalStateExceptionをスローすること() {
        // subjects.properties に存在するキーを使いつつ、
        // 必要な変数を渡さずに本文テンプレートを展開しようとする
        assertThatThrownBy(() -> sut.renderBody("member-registration-body", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
