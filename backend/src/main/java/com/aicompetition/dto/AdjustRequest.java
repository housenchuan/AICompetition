package com.aicompetition.dto;

import java.math.BigDecimal;

/**
 * 人工修整提交请求：覆写核保结论/风险等级/加费系数/风险评分 + 必填原因 + 提交人角色。
 */
public class AdjustRequest {

    private Integer riskScore;
    private String riskLevel;
    private String underwritingResult;
    /** 加费系数，如加费60%记为 1.60；拒保/延期自动归零为 1.00。 */
    private BigDecimal premiumAdjustment;
    /** 关键风险因子（AI 语义生成，人工可修整覆写；未改则保留原值）。 */
    private String keyFactors;
    private String reason;
    /** 提交人角色：核保专员 / 核保主管。 */
    private String role;

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getKeyFactors() { return keyFactors; }
    public void setKeyFactors(String keyFactors) { this.keyFactors = keyFactors; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getUnderwritingResult() { return underwritingResult; }
    public void setUnderwritingResult(String underwritingResult) { this.underwritingResult = underwritingResult; }

    public BigDecimal getPremiumAdjustment() { return premiumAdjustment; }
    public void setPremiumAdjustment(BigDecimal premiumAdjustment) { this.premiumAdjustment = premiumAdjustment; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
