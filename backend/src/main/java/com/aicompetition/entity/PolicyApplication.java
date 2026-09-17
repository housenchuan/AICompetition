package com.aicompetition.entity;

import java.math.BigDecimal;

/**
 * 投保申请记录表 policy_applications
 * 日期字段（applicationDate/createdAt/updatedAt）对应数据库 VARCHAR 列，用 String 存储。
 */
public class PolicyApplication {

    private String profileId;
    private String customerId;
    private String productType;
    private String productName;
    private BigDecimal coverageAmount;
    private BigDecimal premium;
    private String paymentFrequency;
    private String insurancePeriod;
    private Integer waitingPeriod;
    private String beneficiaryRelationship;
    private String applicationDate;   // VARCHAR(20) "yyyy-MM-dd"
    private String status;
    private String createdBy;
    private String createdAt;         // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"
    private String updatedAt;         // VARCHAR(30) "yyyy-MM-dd HH:mm:ss"

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }

    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }

    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }

    public String getInsurancePeriod() { return insurancePeriod; }
    public void setInsurancePeriod(String insurancePeriod) { this.insurancePeriod = insurancePeriod; }

    public Integer getWaitingPeriod() { return waitingPeriod; }
    public void setWaitingPeriod(Integer waitingPeriod) { this.waitingPeriod = waitingPeriod; }

    public String getBeneficiaryRelationship() { return beneficiaryRelationship; }
    public void setBeneficiaryRelationship(String beneficiaryRelationship) { this.beneficiaryRelationship = beneficiaryRelationship; }

    public String getApplicationDate() { return applicationDate; }
    public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
