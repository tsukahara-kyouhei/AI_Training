package jp.co.skig.officeorder.service.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailTemplateRendererTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource subjectResource;

    @Mock
    private Resource bodyResource;

    // ─── renderSubject ───────────────────────────────────────────────────

    @Test
    void renderSubject_known_key_with_no_variables_returns_subject_text() throws Exception {
        // Arrange
        String propertiesContent = "test.subject=テスト件名\n";
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectResource);
        when(subjectResource.getInputStream()).thenReturn(toStream(propertiesContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act
        String result = sut.renderSubject("test.subject", Map.of());

        // Assert
        assertThat(result).isEqualTo("テスト件名");
    }

    @Test
    void renderSubject_key_with_variable_substitutes_placeholder() throws Exception {
        // Arrange
        String propertiesContent = "order.subject=ご注文番号 {order_number} のご確認\n";
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectResource);
        when(subjectResource.getInputStream()).thenReturn(toStream(propertiesContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act
        String result = sut.renderSubject("order.subject", Map.of("order_number", "ORD001"));

        // Assert
        assertThat(result).isEqualTo("ご注文番号 ORD001 のご確認");
    }

    @Test
    void renderSubject_unknown_key_throws_illegal_state_exception() throws Exception {
        // Arrange
        String propertiesContent = "known.subject=件名\n";
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectResource);
        when(subjectResource.getInputStream()).thenReturn(toStream(propertiesContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act / Assert
        assertThatThrownBy(() -> sut.renderSubject("unknown.key", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown.key");
    }

    @Test
    void renderSubject_missing_variable_throws_illegal_state_exception() throws Exception {
        // Arrange
        String propertiesContent = "test.subject=ご注文 {order_number} の件\n";
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectResource);
        when(subjectResource.getInputStream()).thenReturn(toStream(propertiesContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act / Assert
        assertThatThrownBy(() -> sut.renderSubject("test.subject", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order_number");
    }

    // ─── renderBody ──────────────────────────────────────────────────────

    @Test
    void renderBody_template_with_variables_substitutes_all_placeholders() throws Exception {
        // Arrange
        String bodyContent = "こんにちは {last_name} 様\nご登録ありがとうございます。";
        when(resourceLoader.getResource("classpath:mail/templates/test-body.txt")).thenReturn(bodyResource);
        when(bodyResource.getInputStream()).thenReturn(toStream(bodyContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act
        String result = sut.renderBody("test-body", Map.of("last_name", "山田"));

        // Assert
        assertThat(result).isEqualTo("こんにちは 山田 様\nご登録ありがとうございます。");
    }

    @Test
    void renderBody_template_with_no_placeholders_returns_as_is() throws Exception {
        // Arrange
        String bodyContent = "プレースホルダなしの本文です。";
        when(resourceLoader.getResource("classpath:mail/templates/simple-body.txt")).thenReturn(bodyResource);
        when(bodyResource.getInputStream()).thenReturn(toStream(bodyContent));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act
        String result = sut.renderBody("simple-body", Map.of());

        // Assert
        assertThat(result).isEqualTo("プレースホルダなしの本文です。");
    }

    @Test
    void renderBody_resource_not_found_throws_illegal_state_exception() throws Exception {
        // Arrange
        when(resourceLoader.getResource(anyString())).thenReturn(bodyResource);
        when(bodyResource.getInputStream()).thenThrow(new IOException("not found"));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act / Assert
        assertThatThrownBy(() -> sut.renderBody("missing-template", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void renderSubject_subject_resource_io_exception_throws_illegal_state_exception() throws Exception {
        // loadSubjectTemplates() で IOException → IllegalStateException (L84-85)
        // Arrange
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectResource);
        when(subjectResource.getInputStream()).thenThrow(new IOException("file not found"));

        MailTemplateRenderer sut = new MailTemplateRenderer(resourceLoader);

        // Act / Assert
        assertThatThrownBy(() -> sut.renderSubject("any.key", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("subjects.properties");
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
