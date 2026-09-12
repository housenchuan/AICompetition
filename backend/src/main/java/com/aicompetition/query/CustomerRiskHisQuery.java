package com.aicompetition.query;

/**
 * 历史客户风险画像查询参数（分页 + 等值筛选 + 时间范围，日期格式 yyyy-MM-dd）。
 */
public class CustomerRiskHisQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String customerId;
    private String gender;
    private String occupation;
    private String smokingStatus;
    private String drinkingStatus;
    private Boolean hasSocialInsurance;
    private Integer target;

    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getSmokingStatus() { return smokingStatus; }
    public void setSmokingStatus(String smokingStatus) { this.smokingStatus = smokingStatus; }

    public String getDrinkingStatus() { return drinkingStatus; }
    public void setDrinkingStatus(String drinkingStatus) { this.drinkingStatus = drinkingStatus; }

    public Boolean getHasSocialInsurance() { return hasSocialInsurance; }
    public void setHasSocialInsurance(Boolean hasSocialInsurance) { this.hasSocialInsurance = hasSocialInsurance; }

    public Integer getTarget() { return target; }
    public void setTarget(Integer target) { this.target = target; }

    public String getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(String createdFrom) { this.createdFrom = createdFrom; }

    public String getCreatedTo() { return createdTo; }
    public void setCreatedTo(String createdTo) { this.createdTo = createdTo; }

    public String getUpdatedFrom() { return updatedFrom; }
    public void setUpdatedFrom(String updatedFrom) { this.updatedFrom = updatedFrom; }

    public String getUpdatedTo() { return updatedTo; }
    public void setUpdatedTo(String updatedTo) { this.updatedTo = updatedTo; }
}
