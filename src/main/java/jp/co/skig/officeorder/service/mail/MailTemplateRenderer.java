package jp.co.skig.officeorder.service.mail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * メール件名・本文テンプレートを読み込み、差し込み項目を展開するレンダラ。
 *
 * <p>
 * 画面向けメッセージと混在させず、メール文面を専用リソースへ分離して管理するために使う。
 * テンプレート内のプレースホルダは {@code {placeholder_name}} 形式とする。
 */
@Component
public class MailTemplateRenderer {

    /** プレースホルダ置換対象の正規表現。 */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");
    /** 件名定義ファイルの配置先。 */
    private static final String SUBJECTS_RESOURCE = "classpath:mail/subjects.properties";
    /** 本文テンプレート配置ディレクトリ。 */
    private static final String BODY_TEMPLATE_PREFIX = "classpath:mail/templates/";

    /** クラスパス上のテンプレート読込に使う ResourceLoader。 */
    private final ResourceLoader resourceLoader;

    /**
     * メールテンプレートレンダラを生成する。
     *
     * @param resourceLoader クラスパスリソース読込に使う ResourceLoader
     */
    public MailTemplateRenderer(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 件名テンプレートを読み込み、差し込み項目を展開する。
     *
     * @param templateKey 件名テンプレートキー
     * @param variables   差し込み項目
     * @return 展開済み件名
     */
    public String renderSubject(String templateKey, Map<String, String> variables) {
        Properties subjects = loadSubjectTemplates();
        String template = subjects.getProperty(templateKey);
        if (template == null) {
            throw new IllegalStateException("Unknown mail subject template key: " + templateKey);
        }
        return renderTemplate(template, variables, "subject:" + templateKey);
    }

    /**
     * 本文テンプレートを読み込み、差し込み項目を展開する。
     *
     * @param templateName 本文テンプレート名（拡張子除く）
     * @param variables    差し込み項目
     * @return 展開済み本文
     */
    public String renderBody(String templateName, Map<String, String> variables) {
        String resourceLocation = BODY_TEMPLATE_PREFIX + templateName + ".txt";
        String template = readTextResource(resourceLocation);
        return renderTemplate(template, variables, "body:" + templateName);
    }

    /**
     * 件名テンプレート定義を読み込む。
     *
     * @return 件名テンプレート定義
     */
    private Properties loadSubjectTemplates() {
        Properties properties = new Properties();
        Resource resource = resourceLoader.getResource(SUBJECTS_RESOURCE);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load mail subject templates: " + SUBJECTS_RESOURCE, ex);
        }
    }

    /**
     * テンプレート文字列へ差し込み項目を展開する。
     *
     * @param template   テンプレート本文
     * @param variables  差し込み項目
     * @param templateId エラー時に識別しやすくするためのテンプレートID
     * @return 展開済み文字列
     */
    private String renderTemplate(String template, Map<String, String> variables, String templateId) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!variables.containsKey(placeholder)) {
                throw new IllegalStateException("Missing mail template variable: " + templateId + " -> " + placeholder);
            }
            String value = variables.get(placeholder);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /**
     * 指定リソースのテキストを UTF-8 で読み込む。
     *
     * @param resourceLocation 読込対象リソース
     * @return 読み込んだテキスト
     */
    private String readTextResource(String resourceLocation) {
        Resource resource = resourceLoader.getResource(resourceLocation);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[1024];
            int readSize;
            while ((readSize = reader.read(buffer)) != -1) {
                text.append(buffer, 0, readSize);
            }
            return text.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load mail template: " + resourceLocation, ex);
        }
    }
}
