package com.aicompetition.common;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具：把 yyyy-MM-dd 字符串转成 LocalDate / LocalDateTime，用于 timestamp/date 列。
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    /** 解析 yyyy-MM-dd 字符串为 LocalDate。 */
    public static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDate.parse(s.trim(), DATE);
    }

    /** 当天 00:00:00，用于 timestamp 列范围过滤下界。 */
    public static LocalDateTime startOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atStartOfDay();
    }

    /** 当天 23:59:59，用于 timestamp 列范围过滤上界。 */
    public static LocalDateTime endOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atTime(23, 59, 59);
    }

    /** 当前时间，用于写入 timestamp 列。 */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /** LocalDate → yyyy-MM-dd 字符串，用于 PredictService 日期算术结果传参。 */
    public static String formatDate(LocalDate d) {
        return d == null ? null : d.format(DATE);
    }
}
