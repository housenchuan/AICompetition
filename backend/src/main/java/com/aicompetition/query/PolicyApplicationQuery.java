package com.aicompetition.query;

/**
 * 投保申请查询参数（分页 + 等值筛选 + 申请日期范围，日期格式 yyyy-MM-dd）。
 */
public class PolicyApplicationQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String applicationId;
    private String customerId;
    private String productType;
    private String productName;
    private String paymentFrequency;
    private String status;
    private String createdBy;

    /** 关联查询用：按核保决策的风险等级筛选。 */
    private String riskLevel;

    private String dateFrom;
    private String dateTo;

    /** 创建时间 / 更新时间范围（yyyy-MM-dd，按当天起止匹配）。 */
    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }

    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }

    public String getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(String createdFrom) { this.createdFrom = createdFrom; }

    public String getCreatedTo() { return createdTo; }
    public void setCreatedTo(String createdTo) { this.createdTo = createdTo; }

    public String getUpdatedFrom() { return updatedFrom; }
    public void setUpdatedFrom(String updatedFrom) { this.updatedFrom = updatedFrom; }

    public String getUpdatedTo() { return updatedTo; }
    public void setUpdatedTo(String updatedTo) { this.updatedTo = updatedTo; }
}
