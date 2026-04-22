package jp.co.skig.officeorder.common;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AppTimeProvider} の単体テスト。
 */
class AppTimeProviderTest {

    // ---- AT-01: nowOffsetDateTime は固定Clockの時刻を返す ----
    @Test
    void at01_nowOffsetDateTime_returnsClockTime() {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-15T03:00:00Z"), ZoneOffset.ofHours(9));
        AppTimeProvider sut = new AppTimeProvider(fixed);

        var result = sut.nowOffsetDateTime();

        // JST(+09:00) で 2026-01-15T12:00:00
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(12);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    // ---- AT-02: zoneId は Clockのゾーンを返す ----
    @Test
    void at02_zoneId_returnsClockZone() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Clock fixed = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), zone);
        AppTimeProvider sut = new AppTimeProvider(fixed);

        ZoneId result = sut.zoneId();

        assertThat(result).isEqualTo(zone);
    }

    // ---- AT-03: UTC Clockの場合 nowOffsetDateTime はオフセット +00:00 ----
    @Test
    void at03_utcClock_offsetIsZero() {
        Clock fixed = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
        AppTimeProvider sut = new AppTimeProvider(fixed);

        var result = sut.nowOffsetDateTime();

        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.getHour()).isEqualTo(10);
    }
}
