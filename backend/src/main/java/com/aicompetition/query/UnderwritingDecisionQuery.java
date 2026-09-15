package com.aicompetition.query;

/**
 * 核保决策结果查询参数（分页 + 等值筛选）。
 */
public class UnderwritingDecisionQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String decisionId;
    private String applicationId;
    private String customerId;
    private String gender;
    private String occupation;
    private String riskLevel;
    private String underwritingResult;
    private Boolean hasSocialInsurance;
    private String smokingStatus;
    private String drinkingStatus;
    private String createdBy;

    /** 创建时间 / 更新时间范围（yyyy-MM-dd，按当天起止匹配）。 */
    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getUnderwritingResult() { return underwritingResult; }
    public void setUnderwritingResult(String underwritingResult) { this.underwritingResult = underwritingResult; }

    public Boolean getHasSocialInsurance() { return hasSocialInsurance; }
    public void setHasSocialInsurance(Boolean hasSocialInsurance) { this.hasSocialInsurance = hasSocialInsurance; }

    public String getSmokingStatus() { return smokingStatus; }
    public void setSmokingStatus(String smokingStatus) { this.smokingStatus = smokingStatus; }

    public String getDrinkingStatus() { return drinkingStatus; }
    public void setDrinkingStatus(String drinkingStatus) { this.drinkingStatus = drinkingStatus; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(String createdFrom) { this.createdFrom = createdFrom; }

    public String getCreatedTo() { return createdTo; }
    public void setCreatedTo(String createdTo) { this.createdTo = createdTo; }

    public String getUpdatedFrom() { return updatedFrom; }
    public void setUpdatedFrom(String updatedFrom) { this.updatedFrom = updatedFrom; }

    public String getUpdatedTo() { return updatedTo; }
    public void setUpdatedTo(String updatedTo) { this.updatedTo = updatedTo; }
}
