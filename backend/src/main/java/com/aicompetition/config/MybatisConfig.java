package com.aicompetition.config;

import com.aicompetition.typehandler.LocalDateTimeTypeHandler;
import com.aicompetition.typehandler.LocalDateTypeHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 强制替换 MyBatis 内置 LocalDate/LocalDateTime TypeHandler。
 *
 * 内网比赛数据库的日期列为 VARCHAR 类型，而 MyBatis 内置 Handler 调用
 * rs.getObject(col, LocalDate.class)，PostgreSQL JDBC 驱动对 VARCHAR 列
 * 拒绝此调用。通过 ConfigurationCustomizer 在 SqlSessionFactory 初始化
 * 最后一步覆盖注册，保证优先级高于内置 Handler 和 type-handlers-package 扫描。
 */
@org.springframework.context.annotation.Configuration
public class MybatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisTypeHandlerCustomizer() {
        return (Configuration configuration) -> {
            TypeHandlerRegistry reg = configuration.getTypeHandlerRegistry();

            LocalDateTypeHandler ldh = new LocalDateTypeHandler();
            reg.register(LocalDate.class, JdbcType.VARCHAR, ldh);
            reg.register(LocalDate.class, JdbcType.DATE, ldh);
            reg.register(LocalDate.class, JdbcType.CHAR, ldh);
            // null jdbcType = 自动映射兜底（覆盖内置 LocalDateTypeHandler）
            reg.register(LocalDate.class, (JdbcType) null, ldh);

            LocalDateTimeTypeHandler ldth = new LocalDateTimeTypeHandler();
            reg.register(LocalDateTime.class, JdbcType.VARCHAR, ldth);
            reg.register(LocalDateTime.class, JdbcType.TIMESTAMP, ldth);
            reg.register(LocalDateTime.class, JdbcType.CHAR, ldth);
            // null jdbcType = 自动映射兜底（覆盖内置 LocalDateTimeTypeHandler）
            reg.register(LocalDateTime.class, (JdbcType) null, ldth);
        };
    }
}
