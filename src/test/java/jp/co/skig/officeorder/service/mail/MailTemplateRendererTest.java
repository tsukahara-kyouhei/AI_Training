package jp.co.skig.officeorder.service.mail;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailTemplateRendererTest {

    private final MailTemplateRenderer renderer = new MailTemplateRenderer(new DefaultResourceLoader());

    /**
     * 件名テンプレートは `mail/subjects.properties` から読み込み、注文番号のような差し込み項目を展開できることを確認する。
     */
    @Test
    void renderSubject_replacesPlaceholdersDefinedInSubjectProperties() {
        String rendered = renderer.renderSubject(
                "order-complete.subject",
                Map.of("order_number", "ORD20260307-000123")
        );

        assertThat(rendered).isEqualTo("【OFFICE ORDER】ご注文ありがとうございます（注文番号: ORD20260307-000123）");
    }

    /**
     * 本文テンプレートに必要な差し込み項目が不足している場合は、テンプレートと実装の不整合を早期検知できるよう例外を送出することを確認する。
     */
    @Test
    void renderBody_throwsWhenTemplateVariableIsMissing() {
        assertThatThrownBy(() -> renderer.renderBody(
                "member-registration-body",
                Map.of("last_name", "山田")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("member-registration-body")
                .hasMessageContaining("first_name");
    }
}
