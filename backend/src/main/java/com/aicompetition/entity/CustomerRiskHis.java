package com.aicompetition.entity;

import java.math.BigDecimal;

/**
 * 历史客户风险画像分析表 customer_risk_his
 * createdAt/updatedAt 对应数据库 VARCHAR 列，用 String 存储。
 */
public class CustomerRiskHis {

    private String profileId;
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
    private String createdAt;   // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"
    private String updatedAt;   // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"
    private Integer target;
    private Integer scoreV1;

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

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

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Integer getTarget() { return target; }
    public void setTarget(Integer target) { this.target = target; }

    public Integer getScoreV1() { return scoreV1; }
    public void setScoreV1(Integer scoreV1) { this.scoreV1 = scoreV1; }
}
