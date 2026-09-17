package com.aicompetition.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 兼容 VARCHAR 存储的 LocalDate TypeHandler。
 * 内网比赛 DB 的日期列类型为 VARCHAR，JDBC 无法直接映射 LocalDate，需手动解析。
 */
@MappedTypes(LocalDate.class)
@MappedJdbcTypes(value = {JdbcType.VARCHAR, JdbcType.DATE}, includeNullJdbcType = true)
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.format(FMT));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private LocalDate parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            // 兼容 "2025-02-26" 和 "2025-02-26 00:00:00" 两种格式
            return LocalDate.parse(value.trim().substring(0, 10), FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
