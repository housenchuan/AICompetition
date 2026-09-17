package com.aicompetition.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自然语言统计 text-to-SQL 的安全校验单测：
 * 只允许单条只读 SELECT、仅白名单三张业务表、禁注释/禁写操作关键字、无 LIMIT 时强制包一层 LIMIT 200。
 */
class NlSqlServiceTest {

    @Test
    void 普通SELECT_通过校验() {
        String sql = NlSqlService.sanitize("SELECT count(*) AS 总数 FROM customer_risk_his");
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("LIMIT 200"));
    }

    @Test
    void 已带LIMIT_保持不变() {
        String sql = NlSqlService.sanitize("SELECT * FROM policy_applications LIMIT 10");
        assertEquals("SELECT * FROM policy_applications LIMIT 10", sql);
    }

    @Test
    void 小写select与join白名单表_通过() {
        String sql = NlSqlService.sanitize(
                "select pa.product_type, count(*) from policy_applications pa "
                        + "join underwriting_decisions ud on ud.application_id = pa.profile_id group by 1");
        assertTrue(sql.toLowerCase().startsWith("select"));
        assertFalse(sql.contains("_nlq"), "已有 group by 无 limit 应包一层，但不应破坏语义");
    }

    @Test
    void markdown围栏与结尾分号_自动剥离() {
        String sql = NlSqlService.sanitize("```sql\nSELECT 1 FROM customer_risk_his;\n```");
        // 围栏与分号剥离后，无 LIMIT 会自动包一层
        assertTrue(sql.contains("SELECT 1 FROM customer_risk_his"));
        assertTrue(sql.startsWith("SELECT * FROM ("));
        assertTrue(sql.endsWith("_nlq LIMIT 200"));
    }

    @Test
    void 多语句_拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> NlSqlService.sanitize("SELECT 1 FROM customer_risk_his; DELETE FROM customer_risk_his"));
    }

    @Test
    void 写操作_拒绝() {
        assertThrows(IllegalArgumentException.class, () -> NlSqlService.sanitize("DELETE FROM customer_risk_his"));
        assertThrows(IllegalArgumentException.class,
                () -> NlSqlService.sanitize("UPDATE policy_applications SET status = '已通过'"));
        assertThrows(IllegalArgumentException.class, () -> NlSqlService.sanitize("DROP TABLE customer_risk_his"));
    }

    @Test
    void 非白名单表_拒绝() {
        assertThrows(IllegalArgumentException.class, () -> NlSqlService.sanitize("SELECT * FROM pg_tables"));
        assertThrows(IllegalArgumentException.class, () -> NlSqlService.sanitize("SELECT * FROM information_schema.tables"));
    }

    @Test
    void 注释_拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> NlSqlService.sanitize("SELECT 1 FROM customer_risk_his -- and 1=1"));
        assertThrows(IllegalArgumentException.class,
                () -> NlSqlService.sanitize("/* c */ SELECT 1 FROM customer_risk_his"));
    }

    @Test
    void 非SELECT开头_拒绝() {
        assertThrows(IllegalArgumentException.class, () -> NlSqlService.sanitize("WITH t AS (SELECT 1) SELECT * FROM t"));
    }

    @Test
    void 白名单列名不受禁用关键字误伤() {
        // created_at 含 "create" 子串但不是独立单词，不应误拒
        String sql = NlSqlService.sanitize("SELECT created_at FROM customer_risk_his WHERE created_at >= '2025-06-01'");
        assertNotNull(sql);
    }
}
