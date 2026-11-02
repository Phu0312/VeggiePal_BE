package com.veggiepal.ai.quota;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Calendar used for every AI quota period, independent of the host time zone. */
public final class QuotaCalendar {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private QuotaCalendar() {}

    public static LocalDate today() {
        return dateAt(Instant.now());
    }

    public static LocalDateTime localNow() {
        return Instant.now().atZone(ZONE).toLocalDateTime();
    }

    /** For values written with {@link #localNow()}: they are Vietnam wall-clock time, not JVM-zone time. */
    public static Instant toInstant(LocalDateTime localTime) {
        return localTime == null ? null : localTime.atZone(ZONE).toInstant();
    }

    static LocalDate dateAt(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }
}
