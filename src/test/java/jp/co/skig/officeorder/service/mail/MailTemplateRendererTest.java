package jp.co.skig.officeorder.service.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailTemplateRendererTest {

    private ResourceLoader resourceLoader;
    private MailTemplateRenderer sut;

    @BeforeEach
    void setUp() {
        resourceLoader = mock(ResourceLoader.class);
        sut = new MailTemplateRenderer(resourceLoader);
    }

    // --- renderSubject ---

    @Test
    @DisplayName("renderSubject でプレースホルダを変数で置換した件名を返す")
    void renderSubject_withVariables_replacesPlaceholders() throws IOException {
        String subjectsProps = "order-complete.subject=ご注文完了 - 注文番号 {order_number}\n";
        Resource subjectsResource = mockResource(subjectsProps);
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectsResource);

        String result = sut.renderSubject("order-complete.subject", Map.of("order_number", "ORD20260417-000001"));

        assertThat(result).isEqualTo("ご注文完了 - 注文番号 ORD20260417-000001");
    }

    @Test
    @DisplayName("renderSubject でプレースホルダがない場合はそのまま返す")
    void renderSubject_noPlaceholders_returnsAsIs() throws IOException {
        String subjectsProps = "plain.subject=お知らせ\n";
        Resource subjectsResource = mockResource(subjectsProps);
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectsResource);

        String result = sut.renderSubject("plain.subject", Map.of());

        assertThat(result).isEqualTo("お知らせ");
    }

    @Test
    @DisplayName("未知のキーを指定すると IllegalStateException をスローする")
    void renderSubject_unknownKey_throwsIllegalStateException() throws IOException {
        String subjectsProps = "known.subject=テスト\n";
        Resource subjectsResource = mockResource(subjectsProps);
        when(resourceLoader.getResource("classpath:mail/subjects.properties")).thenReturn(subjectsResource);

        assertThatThrownBy(() -> sut.renderSubject("unknown.subject", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown mail subject template key");
    }

    // --- renderBody ---

    @Test
    @DisplayName("renderBody でプレースホルダを変数で置換した本文を返す")
    void renderBody_withVariables_replacesPlaceholders() throws IOException {
        String template = "{last_name} 様\nこの度はご注文いただきありがとうございました。";
        Resource bodyResource = mockResource(template);
        when(resourceLoader.getResource(startsWith("classpath:mail/templates/")))
                .thenReturn(bodyResource);

        String result = sut.renderBody("order-complete-body", Map.of("last_name", "山田"));

        assertThat(result).contains("山田 様");
        assertThat(result).contains("この度はご注文いただきありがとうございました。");
    }

    @Test
    @DisplayName("変数不足のとき IllegaStateException をスローする")
    void renderBody_missingVariable_throwsIllegalStateException() throws IOException {
        String template = "こんにちは {missing_var} さん";
        Resource bodyResource = mockResource(template);
        when(resourceLoader.getResource(startsWith("classpath:mail/templates/")))
                .thenReturn(bodyResource);

        assertThatThrownBy(() -> sut.renderBody("template", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing mail template variable");
    }

    @Test
    @DisplayName("テンプレートファイル読み込みに失敗したとき IllegalStateException をスローする")
    void renderBody_resourceNotFound_throwsIllegalStateException() throws IOException {
        Resource brokenResource = mock(Resource.class);
        when(brokenResource.getInputStream()).thenThrow(new IOException("not found"));
        when(resourceLoader.getResource(startsWith("classpath:mail/templates/")))
                .thenReturn(brokenResource);

        assertThatThrownBy(() -> sut.renderBody("non-existent", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load mail template");
    }

    // --- ヘルパ ---

    private Resource mockResource(String content) throws IOException {
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(stream);
        return resource;
    }
}
