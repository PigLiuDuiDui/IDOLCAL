package com.example.bim.api.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 活动时间换算：官方时区缩写（KST / JST / CST…）→ IANA 时区 → UTC 绝对时刻。
 * 与前端 src/utils/time.js 的 TZ_ALIASES 保持一致（粉丝习惯的固定缩写，非夏令时语义）。
 * 数据库只存 date/time/timezone（官方原始数据，前端展示用）+ start_at_utc（后端计算用）。
 */
public final class EventTimes {

    /** 官方时区缩写 → IANA 时区（与前端 TZ_ALIASES 一致） */
    private static final java.util.Map<String, String> TZ_ALIASES = java.util.Map.of(
            "KST", "Asia/Seoul",
            "JST", "Asia/Tokyo",
            "CST", "Asia/Shanghai",
            "ICT", "Asia/Bangkok",
            "PHT", "Asia/Manila",
            "SGT", "Asia/Singapore",
            "WIB", "Asia/Jakarta",
            "HKT", "Asia/Hong_Kong",
            "IST", "Asia/Kolkata");

    private EventTimes() {
    }

    /** 事件 timezone 缩写（或 IANA 名）→ ZoneId；未知时默认 Asia/Seoul */
    public static ZoneId zoneOf(String timezone) {
        String alias = timezone == null || timezone.isBlank() ? "KST" : timezone;
        String iana = TZ_ALIASES.getOrDefault(alias, alias);
        try {
            return ZoneId.of(iana);
        } catch (Exception e) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    /**
     * 计算活动开始时刻的 UTC epoch millis。
     * time 为空返回 null（全天活动无确定时刻）；time='00:00' 按官方时区当天 0 点计算。
     */
    public static Long startAtUtc(String date, String time, String timezone) {
        if (date == null || !date.matches("^\\d{4}-\\d{2}-\\d{2}$")) return null;
        if (time == null || !time.matches("^\\d{2}:\\d{2}$")) return null;
        try {
            String[] d = date.split("-");
            String[] t = time.split(":");
            LocalDateTime ldt = LocalDateTime.of(
                    Integer.parseInt(d[0]), Integer.parseInt(d[1]), Integer.parseInt(d[2]),
                    Integer.parseInt(t[0]), Integer.parseInt(t[1]));
            return ZonedDateTime.of(ldt, zoneOf(timezone)).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }
}
