package com.aicompetition.query;

import java.util.List;

/**
 * 数据汇总聚合查询参数。
 * entity 与 groupBy 均走白名单校验，避免任意字段/裸 SQL。
 */
public class AggregateQuery {

    /** 数据源：policy_applications / customer_risk_his / underwriting_decisions */
    private String entity;

    /** 分组维度（1~2 个，白名单字段名），如 productType / status / riskLevel / month 等 */
    private List<String> groupBy;

    /** 时间范围（yyyy-MM-dd，按各表自然日期过滤：申请=application_date，画像/决策=created_at） */
    private String dateFrom;
    private String dateTo;

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public List<String> getGroupBy() { return groupBy; }
    public void setGroupBy(List<String> groupBy) { this.groupBy = groupBy; }

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }

    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
}
