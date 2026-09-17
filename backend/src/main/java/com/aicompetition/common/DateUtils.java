package com.aicompetition.common;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具：把 yyyy-MM-dd 字符串转成范围边界（字符串形式，兼容 VARCHAR 日期列）。
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {}

    /** 解析 yyyy-MM-dd 字符串为 LocalDate（内部使用，供日期算术）。 */
    public static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDate.parse(s.trim(), DATE);
    }

    /** 当天 00:00:00 字符串，用于 VARCHAR 日期列范围过滤下界。 */
    public static String startOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atStartOfDay().format(DT);
    }

    /** 当天 23:59:59 字符串，用于 VARCHAR 日期列范围过滤上界。 */
    public static String endOfDay(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atTime(23, 59, 59).format(DT);
    }

    /** 当前时间的 yyyy-MM-dd HH:mm:ss 字符串，用于写入 VARCHAR 时间列。 */
    public static String nowStr() {
        return LocalDateTime.now().format(DT);
    }

    /** LocalDate → yyyy-MM-dd 字符串，用于 PredictService 日期算术结果传参。 */
    public static String formatDate(LocalDate d) {
        return d == null ? null : d.format(DATE);
    }
}
