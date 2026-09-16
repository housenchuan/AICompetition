package com.aicompetition.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackItem {

    private String id;          // F5000, F5001, ...
    private String type;        // 问题反馈/功能建议/误判申诉/规则优化/投诉/表扬
    private String source;      // 核保员/代理人/客户/内部质检/系统巡检
    private String priority;    // 高/中/低
    private String relatedNo;   // 关联核保号（可选）
    private String title;
    private String description;
    private String contact;
    private String status;      // 待处理/处理中/已解决/已驳回/已关闭
    private String handler;
    private Integer satisfaction; // 1-5
    private String createdAt;   // yyyy-MM-dd HH:mm:ss
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getRelatedNo() { return relatedNo; }
    public void setRelatedNo(String relatedNo) { this.relatedNo = relatedNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }

    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
