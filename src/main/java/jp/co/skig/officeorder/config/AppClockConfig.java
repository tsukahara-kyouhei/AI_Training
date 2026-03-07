package jp.co.skig.officeorder.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * アプリ標準Clockを定義する設定。
 */
@Configuration
public class AppClockConfig {

    /** 独自アプリ設定。 */
    private final AppProperties appProperties;

    /**
     * Clock設定を生成する。
     *
     * @param appProperties 独自アプリ設定
     */
    public AppClockConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * アプリ標準Clockを生成する。
     *
     * @return システムClock
     */
    @Bean
    public Clock appClock() {
        return Clock.system(ZoneId.of(appProperties.getTimeZone()));
    }
}
