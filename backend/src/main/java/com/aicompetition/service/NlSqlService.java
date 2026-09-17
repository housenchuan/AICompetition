package com.aicompetition.service;

import com.aicompetition.entity.ai.ChatMessage;
import com.aicompetition.entity.ai.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自然语言统计（text-to-SQL）：把表结构 + 用户自然语言统计问题传给爱码 LLM 生成
 * PostgreSQL SELECT 语句，经安全校验（单条只读、表白名单、禁关键字、强制 LIMIT）后
 * 通过 JdbcTemplate 执行并返回结构化结果。
 */
@Service
public class NlSqlService {

    private static final Logger log = LoggerFactory.getLogger(NlSqlService.class);

    /** 只允许查询这三张业务表。 */
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "customer_risk_his", "policy_applications", "underwriting_decisions");

    /** 禁止出现的关键字（写操作/DDL/危险语句）。 */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|create|truncate|grant|revoke|copy|merge|call|do|set|vacuum|analyze|comment|reindex|listen|notify|lock)\\b");

    /** 提取 FROM/JOIN 后的表名。 */
    private static final Pattern TABLE_REF = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+([a-z_][a-z0-9_]*)");

    private static final int MAX_ROWS = 200;

    private final AiService aiService;
    private final JdbcTemplate jdbcTemplate;

    public NlSqlService(AiService aiService, DataSource dataSource) {
        this.aiService = aiService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(15);
        this.jdbcTemplate.setMaxRows(MAX_ROWS + 1);
    }

    /** 自然语言统计问题 → SQL → 安全校验 → 执行 → 结构化结果。 */
    public Map<String, Object> stats(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("统计问题不能为空");
        }
        String sql = generateSql(question.trim());
        sql = sanitize(sql);
        log.info("NL 统计 SQL：{}", sql);

        List<Map<String, Object>> raw = jdbcTemplate.queryForList(sql);
        boolean truncated = raw.size() > MAX_ROWS;
        if (truncated) {
            raw = new ArrayList<>(raw.subList(0, MAX_ROWS));
        }

        List<String> columns = new ArrayList<>();
        if (!raw.isEmpty()) {
            raw.get(0).keySet().forEach(columns::add);
        }
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String c : columns) {
                row.put(c, formatValue(r.get(c)));
            }
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question.trim());
        result.put("sql", sql);
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("rowCount", rows.size());
        result.put("truncated", truncated);
        return result;
    }

    // ===================== LLM 生成 SQL =====================

    private String generateSql(String question) {
        String sys = "你是 PostgreSQL 数据分析专家。根据表结构把用户的中文统计问题转成一条 SELECT 语句，"
                + "只输出 SQL 本身，不要 markdown，不要分号结尾，不要解释。";
        String user = """
                数据库为 PostgreSQL，三张业务表（只读）：
                1. customer_risk_his 历史客户风险画像表：profile_id 画像唯一标识, customer_id 投保人编号, age 年龄, gender 性别(男/女), occupation 职业, annual_income 年收入, has_social_insurance 是否有社保(boolean), smoking_status 吸烟状况(是/否/已戒烟), drinking_status 饮酒状况(是/否/偶尔), family_medical_history 家族病史(如 无/无特殊病史/高血压/糖尿病/脑血管疾病/心脏病/恶性肿瘤 或顿号组合), personal_medical_history 个人病史(如 无/颈椎病/脂肪肝/高血压/糖尿病/甲状腺结节/胃炎/胆结石/乙肝病毒携带/腰椎间盘突出 或组合), bmi 体重指数, blood_pressure 血压(正常/正常高值/临界高血压/轻度高血压/中度高血压), created_at/updated_at 时间戳, target 是否理赔(1有理赔/0无理赔), score_v1 第一版风险评分。
                2. policy_applications 投保申请记录表：profile_id 投保申请唯一标识, customer_id 投保人编号, product_type 产品类型(寿险/医疗险/意外险/重疾险/年金险), product_name 产品名称, coverage_amount 保额, premium 保费, payment_frequency 缴费频率(年缴/半年缴/季缴/月缴), insurance_period 保障期限, waiting_period 等待期天数, beneficiary_relationship 与受益人关系, application_date 申请日期(date), status 申请状态(待核保/核保中/已通过/已拒保/已撤单), created_by 创建人(系统/人工), created_at/updated_at 时间戳。
                3. underwriting_decisions 核保决策画像分析结果表：decision_id 决策唯一标识, application_id 投保申请编号, customer_id 投保人编号, age/gender/occupation 等画像字段同表1, risk_score 风险评分, risk_level 风险等级(标准体/次标体A级/次标体B级/高风险体/拒保体，未预测时为 NULL), underwriting_result 核保结论(标保/加费/除外/延期/拒保，未预测时为 NULL), premium_adjustment 加费比例, key_factors 关键风险因子, created_by 创建人, created_at/updated_at 时间戳。

                用户统计问题："%s"

                生成要求：
                - 只查上述三张表；需要关联时用 JOIN（投保申请 pa 关联核保决策 ud：pa.profile_id = ud.application_id）；
                - 统计类问题用聚合函数（count/sum/avg/max/min），比率用 count(*) FILTER (WHERE ...) * 100.0 / count(*) 之类表达，列别名用中文；
                - 时间口径默认用 created_at（申请/画像/决策的创建时间）；"2025年6月" 即 created_at >= '2025-06-01' AND created_at < '2025-07-01'；申请业务日期可用 application_date；
                - "通过率"指 status='已通过' 的占比；"风险分布"按 risk_level 分组；"已通过核保的人员数据"为明细查询；
                - 结果加 ORDER BY，明细类查询加 LIMIT 200；
                - 输出一条以 SELECT 开头的 SQL（不要使用 WITH/CTE），不要分号，不要解释。""".formatted(question);

        // 推理类模型（如 DeepSeek）会先输出思考过程再给答案，max_tokens 需留足余量
        ChatResponse resp = aiService.chat(List.of(ChatMessage.system(sys), ChatMessage.user(user)), 0.0, 4096)
                .block(java.time.Duration.ofSeconds(60));
        String content = resp != null && resp.getChoices() != null && !resp.getChoices().isEmpty()
                ? resp.getChoices().get(0).getMessage().getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 未返回 SQL");
        }
        return content;
    }

    // ===================== 安全校验 =====================

    /** 校验并归一化 LLM 生成的 SQL：单条只读、表白名单、禁关键字、强制 LIMIT。（纯函数，静态便于单测） */
    static String sanitize(String raw) {
        String sql = raw.trim();
        // 去掉 markdown 代码块
        if (sql.startsWith("```")) {
            int nl = sql.indexOf('\n');
            if (nl >= 0) sql = sql.substring(nl + 1);
            sql = sql.replaceAll("```\\s*$", "").trim();
        }
        // 去掉结尾分号
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            throw new IllegalArgumentException("只允许单条查询语句");
        }
        if (sql.contains("--") || sql.contains("/*")) {
            throw new IllegalArgumentException("SQL 中不允许包含注释");
        }
        String head = sql.toLowerCase();
        if (!head.startsWith("select")) {
            throw new IllegalArgumentException("仅支持 SELECT 查询");
        }
        Matcher forbidden = FORBIDDEN.matcher(sql);
        if (forbidden.find()) {
            throw new IllegalArgumentException("SQL 包含不允许的关键字：" + forbidden.group());
        }
        Matcher tables = TABLE_REF.matcher(sql);
        boolean hasTable = false;
        while (tables.find()) {
            hasTable = true;
            String t = tables.group(1).toLowerCase();
            if (!ALLOWED_TABLES.contains(t)) {
                throw new IllegalArgumentException("不允许查询表：" + t);
            }
        }
        if (!hasTable) {
            throw new IllegalArgumentException("SQL 未引用任何业务表");
        }
        // 强制行数上限：无 LIMIT 则包一层
        if (!Pattern.compile("(?i)\\blimit\\b").matcher(sql).find()) {
            sql = "SELECT * FROM (\n" + sql + "\n) _nlq LIMIT " + MAX_ROWS;
        }
        return sql;
    }

    // ===================== 结果值格式化 =====================

    private String formatValue(Object v) {
        if (v == null) return "";
        if (v instanceof Boolean) return (Boolean) v ? "是" : "否";
        if (v instanceof Timestamp) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp) v);
        }
        if (v instanceof java.sql.Date) {
            return v.toString();
        }
        if (v instanceof java.util.Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.util.Date) v);
        }
        if (v instanceof BigDecimal) {
            return ((BigDecimal) v).stripTrailingZeros().toPlainString();
        }
        if (v instanceof Double || v instanceof Float) {
            return String.valueOf(Math.round(((Number) v).doubleValue() * 100.0) / 100.0);
        }
        return String.valueOf(v);
    }
}
