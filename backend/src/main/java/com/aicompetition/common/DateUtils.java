package com.aicompetition.common;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期解析工具：把 yyyy-MM-dd 字符串转成范围边界。
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    public static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDate.parse(s.trim(), DATE);
    }

    /** 当天 00:00:00 作为范围下界。 */
    public static LocalDateTime startOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atStartOfDay();
    }

    /** 当天 23:59:59 作为范围上界。 */
    public static LocalDateTime endOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atTime(LocalTime.of(23, 59, 59));
    }
}
