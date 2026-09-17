package com.aicompetition.entity;

import java.math.BigDecimal;

/**
 * 核保决策画像分析结果表 underwriting_decisions
 * 其中 riskScore/riskLevel/underwritingResult/premiumAdjustment/keyFactors 为 AI 生成字段。
 * createdAt/updatedAt 对应数据库 VARCHAR 列，用 String 存储。
 */
public class UnderwritingDecision {

    private String decisionId;
    private String applicationId;
    private String customerId;
    private Integer age;
    private String gender;
    private String occupation;
    private BigDecimal annualIncome;
    private Boolean hasSocialInsurance;
    private String smokingStatus;
    private String drinkingStatus;
    private String familyMedicalHistory;
    private String personalMedicalHistory;
    private BigDecimal bmi;
    private String bloodPressure;
    private Integer riskScore;
    private String riskLevel;
    private String underwritingResult;
    private BigDecimal premiumAdjustment;
    private String keyFactors;
    private String createdBy;
    private String createdAt;   // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"
    private String updatedAt;   // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"

    /** 非持久化：人工修整状态标签（待审批/已生效/已驳回），由 AdjustmentService 合并填充，不入库。 */
    private String adjustStatus;

    public String getAdjustStatus() { return adjustStatus; }
    public void setAdjustStatus(String adjustStatus) { this.adjustStatus = adjustStatus; }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }

    public Boolean getHasSocialInsurance() { return hasSocialInsurance; }
    public void setHasSocialInsurance(Boolean hasSocialInsurance) { this.hasSocialInsurance = hasSocialInsurance; }

    public String getSmokingStatus() { return smokingStatus; }
    public void setSmokingStatus(String smokingStatus) { this.smokingStatus = smokingStatus; }

    public String getDrinkingStatus() { return drinkingStatus; }
    public void setDrinkingStatus(String drinkingStatus) { this.drinkingStatus = drinkingStatus; }

    public String getFamilyMedicalHistory() { return familyMedicalHistory; }
    public void setFamilyMedicalHistory(String familyMedicalHistory) { this.familyMedicalHistory = familyMedicalHistory; }

    public String getPersonalMedicalHistory() { return personalMedicalHistory; }
    public void setPersonalMedicalHistory(String personalMedicalHistory) { this.personalMedicalHistory = personalMedicalHistory; }

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getUnderwritingResult() { return underwritingResult; }
    public void setUnderwritingResult(String underwritingResult) { this.underwritingResult = underwritingResult; }

    public BigDecimal getPremiumAdjustment() { return premiumAdjustment; }
    public void setPremiumAdjustment(BigDecimal premiumAdjustment) { this.premiumAdjustment = premiumAdjustment; }

    public String getKeyFactors() { return keyFactors; }
    public void setKeyFactors(String keyFactors) { this.keyFactors = keyFactors; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
