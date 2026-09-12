package com.aicompetition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自然语言指令解析结果（意图 + 结构化参数）。
 * 由 LLM 解析生成，后端据此路由到白名单业务方法（查询 / 预测 / 统计）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntentResult {

    /** QUERY 查询 / PREDICT 预测 / AGGREGATE 统计 / UNKNOWN 无法识别 */
    private String intent;

    /** 目标表：customer_risk_his / policy_applications / underwriting_decisions */
    private String entity;

    /** 预测/查询目标范围：single 单条 / batch 多条 / all 全部 / by_date 按日期 */
    private String target;

    /** 统计指标，如 pass_rate、risk_level_distribution、count */
    private List<String> metrics;

    /** 过滤条件：customerId / customerName / timeRange{start,end} / date / status / productType 等 */
    private Map<String, Object> filters = new HashMap<>();

    /** 无法识别或需澄清时的说明 */
    private String note;

    /** LLM 原始返回（便于调试） */
    private String raw;

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public List<String> getMetrics() { return metrics; }
    public void setMetrics(List<String> metrics) { this.metrics = metrics; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}
