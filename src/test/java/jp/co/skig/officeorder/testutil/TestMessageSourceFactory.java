package jp.co.skig.officeorder.testutil;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 単体テストで本番用 messages.properties を読み込むための MessageSource 生成ヘルパ。
 *
 * <p>サービス単体テストでも画面向けメッセージ定義を本番と同じ束から取得し、
 * 文言管理の差異でテストがすり抜けないようにする。
 */
public final class TestMessageSourceFactory {

    private TestMessageSourceFactory() {
    }

    /**
     * クラスパス上の messages.properties を読む MessageSource を返す。
     *
     * @return テスト用 MessageSource
     */
    public static MessageSource create() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
