package com.aicompetition.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投保申请 + 核保决策结果 关联查询视图对象（列表用）。
 * 申请字段来自 policy_applications，AI 决策字段来自关联的 underwriting_decisions（LEFT JOIN，可能为空）。
 */
public class ApplicationDecisionVO {

    // ===== 投保申请字段 =====
    private String applicationId;
    private String customerId;
    private String productType;
    private String productName;
    private BigDecimal coverageAmount;
    private BigDecimal premium;
    private String paymentFrequency;
    private String insurancePeriod;
    private Integer waitingPeriod;
    private String beneficiaryRelationship;
    private LocalDate applicationDate;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 关联的核保决策字段（可能为 null）=====
    private String decisionId;
    private Integer riskScore;
    private String riskLevel;
    private String underwritingResult;
    private BigDecimal premiumAdjustment;
    private String keyFactors;

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

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

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }

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
}
