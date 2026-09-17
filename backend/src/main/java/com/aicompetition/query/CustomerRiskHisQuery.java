package com.aicompetition.query;

/**
 * 历史客户风险画像查询参数（分页 + 等值筛选 + 时间范围，日期格式 yyyy-MM-dd）。
 */
public class CustomerRiskHisQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String profileId;
    private String customerId;
    private String gender;
    private String occupation;
    private String smokingStatus;
    private String drinkingStatus;
    private Boolean hasSocialInsurance;
    private Integer target;

    /** 血压情况过滤：按分类标签等值匹配（正常/正常高值/临界高血压/轻度高血压/中度高血压） */
    private String bloodPressure;

    private Integer ageMin;
    private Integer ageMax;
    private Integer incomeMin;
    private Integer incomeMax;
    private Double bmiMin;
    private Double bmiMax;
    private Integer scoreMin;
    private Integer scoreMax;

    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

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

    public Integer getAgeMin() { return ageMin; }
    public void setAgeMin(Integer ageMin) { this.ageMin = ageMin; }
    public Integer getAgeMax() { return ageMax; }
    public void setAgeMax(Integer ageMax) { this.ageMax = ageMax; }
    public Integer getIncomeMin() { return incomeMin; }
    public void setIncomeMin(Integer incomeMin) { this.incomeMin = incomeMin; }
    public Integer getIncomeMax() { return incomeMax; }
    public void setIncomeMax(Integer incomeMax) { this.incomeMax = incomeMax; }
    public Double getBmiMin() { return bmiMin; }
    public void setBmiMin(Double bmiMin) { this.bmiMin = bmiMin; }
    public Double getBmiMax() { return bmiMax; }
    public void setBmiMax(Double bmiMax) { this.bmiMax = bmiMax; }
    public Integer getScoreMin() { return scoreMin; }
    public void setScoreMin(Integer scoreMin) { this.scoreMin = scoreMin; }
    public Integer getScoreMax() { return scoreMax; }
    public void setScoreMax(Integer scoreMax) { this.scoreMax = scoreMax; }

    public String getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(String createdFrom) { this.createdFrom = createdFrom; }

    public String getCreatedTo() { return createdTo; }
    public void setCreatedTo(String createdTo) { this.createdTo = createdTo; }

    public String getUpdatedFrom() { return updatedFrom; }
    public void setUpdatedFrom(String updatedFrom) { this.updatedFrom = updatedFrom; }

    public String getUpdatedTo() { return updatedTo; }
    public void setUpdatedTo(String updatedTo) { this.updatedTo = updatedTo; }
}
