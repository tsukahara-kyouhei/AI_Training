package jp.co.skig.officeorder.common;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

/**
 * アプリ共通の現在時刻取得窓口。
 *
 * <p>{@link Clock} を直接参照せず、このクラス経由で取得することで
 * テスト時に時刻差し替えしやすくする。
 */
@Component
public class AppTimeProvider {

    /** アプリ標準時刻の基準Clock。 */
    private final Clock appClock;

    /**
     * 共通時刻プロバイダを生成する。
     *
     * @param appClock アプリ標準Clock
     */
    public AppTimeProvider(Clock appClock) {
        this.appClock = appClock;
    }

    /**
     * 現在日時をオフセット付きで返す。
     *
     * @return 現在日時
     */
    public OffsetDateTime nowOffsetDateTime() {
        return OffsetDateTime.now(appClock);
    }

    /**
     * アプリ標準のタイムゾーンを返す。
     *
     * @return タイムゾーン
     */
    public ZoneId zoneId() {
        return appClock.getZone();
    }
}
