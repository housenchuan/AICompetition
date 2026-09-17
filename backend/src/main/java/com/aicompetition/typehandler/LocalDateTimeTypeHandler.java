package com.aicompetition.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * 兼容 VARCHAR 存储的 LocalDateTime TypeHandler。
 * 内网比赛 DB 的时间列类型为 VARCHAR，JDBC 无法直接映射 LocalDateTime，需手动解析。
 */
@MappedTypes(LocalDateTime.class)
@MappedJdbcTypes(value = {JdbcType.VARCHAR, JdbcType.TIMESTAMP}, includeNullJdbcType = true)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    /** 容忍不同精度：yyyy-MM-dd HH:mm:ss[.SSSSSS] */
    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        if (jdbcType == JdbcType.VARCHAR) {
            ps.setString(i, parameter.format(OUT));
        } else {
            ps.setTimestamp(i, Timestamp.valueOf(parameter));
        }
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String v = value.trim();
            // 仅日期时（VARCHAR 存成 "2025-02-26"），补上时间部分
            if (v.length() == 10) v = v + " 00:00:00";
            return LocalDateTime.parse(v, FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
