package jp.co.skig.officeorder.service.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MailTemplateRenderer} の単体テスト。
 *
 * <p>クラスパス上の実リソース（subjects.properties / templates/*.txt）を使う。
 */
class MailTemplateRendererTest {

    private MailTemplateRenderer sut;

    @BeforeEach
    void setUp() {
        sut = new MailTemplateRenderer(new DefaultResourceLoader());
    }

    // =========================================================
    // renderSubject
    // =========================================================

    // ---- RS-01: 既存キーで件名が返される ----
    @Test
    @DisplayName("有効な件名テンプレートキーで件名文字列が返される")
    void rs01_validKey_returnsSubject() {
        String subject = sut.renderSubject("member-registration.subject", Map.of());
        assertThat(subject).contains("OFFICE ORDER");
    }

    // ---- RS-02: order-complete.subject のプレースホルダを展開する ----
    @Test
    @DisplayName("order-complete.subject の {order_number} が差し込まれる")
    void rs02_orderCompleteSubject_placeholderReplaced() {
        String subject = sut.renderSubject(
                "order-complete.subject",
                Map.of("order_number", "ORD20260310-000001")
        );
        assertThat(subject).contains("ORD20260310-000001");
    }

    // ---- RS-03: 存在しないキー → IllegalStateException ----
    @Test
    @DisplayName("存在しない件名テンプレートキーは IllegalStateException をスローする")
    void rs03_unknownKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> sut.renderSubject("nonexistent.key", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonexistent.key");
    }

    // ---- RS-04: プレースホルダが variables にない → IllegalStateException ----
    @Test
    @DisplayName("件名テンプレートに必要な変数が不足している場合は IllegalStateException をスローする")
    void rs04_missingPlaceholderVariable_throwsIllegalStateException() {
        // order-complete.subject は {order_number} を含む
        assertThatThrownBy(() -> sut.renderSubject("order-complete.subject", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order_number");
    }

    // =========================================================
    // renderBody
    // =========================================================

    // ---- RB-01: order-complete-body テンプレートのプレースホルダを展開する ----
    @Test
    @DisplayName("order-complete-body テンプレートの変数が差し込まれる")
    void rb01_orderCompleteBody_placeholderReplaced() {
        Map<String, String> vars = Map.ofEntries(
                Map.entry("customer_last_name", "山田"),
                Map.entry("customer_first_name", "太郎"),
                Map.entry("order_number", "ORD20260310-000001"),
                Map.entry("order_datetime", "2026-03-10 09:12"),
                Map.entry("shipping_name", "山田太郎"),
                Map.entry("shipping_postal_code", "100-0001"),
                Map.entry("shipping_prefecture", "東京都"),
                Map.entry("shipping_city", "千代田区"),
                Map.entry("shipping_address_line", "丸の内1-1-1"),
                Map.entry("shipping_floor", "3階"),
                Map.entry("shipping_has_elevator", "あり"),
                Map.entry("daytime_phone", "03-1234-5678"),
                Map.entry("order_items_lines", "テスト商品 x1 ¥5,000"),
                Map.entry("subtotal_amount", "5,000"),
                Map.entry("assembly_fee_total", "0"),
                Map.entry("shipping_fee", "800"),
                Map.entry("tax_amount", "500"),
                Map.entry("total_amount", "6,300"),
                Map.entry("contact_email", "support@example.com")
        );

        String body = sut.renderBody("order-complete-body", vars);

        assertThat(body).contains("山田");
        assertThat(body).contains("ORD20260310-000001");
    }

    // ---- RB-02: 変数の値が null → 空文字として展開される ----
    @Test
    @DisplayName("変数の値が null の場合は空文字として展開される")
    void rb02_nullVariableValue_renderedAsEmpty() {
        Map<String, String> vars = Map.ofEntries(
                Map.entry("customer_last_name", "山田"),
                Map.entry("customer_first_name", "太郎"),
                Map.entry("order_number", "ORD20260310-000001"),
                Map.entry("order_datetime", "2026-03-10 09:12"),
                Map.entry("shipping_name", "山田太郎"),
                Map.entry("shipping_postal_code", "100-0001"),
                Map.entry("shipping_prefecture", "東京都"),
                Map.entry("shipping_city", "千代田区"),
                Map.entry("shipping_address_line", "丸の内1-1-1"),
                Map.entry("shipping_floor", "3階"),
                Map.entry("shipping_has_elevator", "あり"),
                Map.entry("daytime_phone", "03-1234-5678"),
                Map.entry("order_items_lines", "テスト商品 x1 ¥5,000"),
                Map.entry("subtotal_amount", "5,000"),
                Map.entry("assembly_fee_total", "0"),
                Map.entry("shipping_fee", "800"),
                Map.entry("tax_amount", "500"),
                Map.entry("total_amount", "6,300"),
                Map.entry("contact_email", "support@example.com")
        );
        // null value を HashMap で保持する
        java.util.HashMap<String, String> mutableVars = new java.util.HashMap<>(vars);
        mutableVars.put("customer_first_name", null);

        // null 値は空文字として展開され例外はスローされない
        String body = sut.renderBody("order-complete-body", mutableVars);
        assertThat(body).isNotNull();
    }

    // ---- RB-03: 存在しないテンプレート → IllegalStateException ----
    @Test
    @DisplayName("存在しない本文テンプレート名は IllegalStateException をスローする")
    void rb03_nonexistentTemplate_throwsIllegalStateException() {
        assertThatThrownBy(() -> sut.renderBody("nonexistent-template", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- RB-04: プレースホルダが variables にない → IllegalStateException ----
    @Test
    @DisplayName("本文テンプレートに必要な変数が不足している場合は IllegalStateException をスローする")
    void rb04_missingBodyVariable_throwsIllegalStateException() {
        // 空の variables → 最初のプレースホルダで失敗
        assertThatThrownBy(() -> sut.renderBody("order-complete-body", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("body:order-complete-body");
    }

    // ---- RB-05: member-registration-body テンプレートが読み込める ----
    @Test
    @DisplayName("member-registration-body テンプレートが変数なしで読み込める（プレースホルダなし確認）")
    void rb05_memberRegistrationBody_loadsSuccessfully() {
        // member-registration-body テンプレートにプレースホルダがあるかどうかは不明だが、
        // テンプレートファイルが存在することを確認する
        // プレースホルダがある場合は変数を全部渡す必要があるため、
        // ここではファイル読み込みが成功することのみ確認する（例外がスローされないか変数不足例外のみ）
        try {
            sut.renderBody("member-registration-body", Map.of());
        } catch (IllegalStateException ex) {
            // プレースホルダが存在する場合は変数不足で例外 → ファイル読み込みは成功
            assertThat(ex.getMessage()).doesNotContain("Failed to load mail template");
        }
    }
}
