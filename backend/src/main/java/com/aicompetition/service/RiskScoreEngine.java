package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 风险计分规则引擎（骨架，确定性计算）。
 *
 * 说明：BMI/血压/烟酒/个人病史/年龄 为赛题给定固定规则，此处按规则精确计算；
 * 职业/家族病史/风险等级区间读取自知识库（rules/risk-rules.json），
 * 属 V2 待历史数据挖掘校准的部分。文本病史的模糊语义归类由 LLM 层补充（见预测数据流）。
 */
@Service
public class RiskScoreEngine {

    private final RuleService ruleService;

    public RiskScoreEngine(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    public ScoreResult compute(UnderwritingDecision in) {
        ScoreResult r = new ScoreResult();

        // 直接拒保：个人病史命中重度异常疾病
        String personal = safe(in.getPersonalMedicalHistory());
        for (JsonNode rule : ruleService.section("personalHistory")) {
            if (rule.path("reject").asBoolean(false) && containsAny(personal, rule.get("diseases"))) {
                r.setRejected(true);
                r.addFactor("个人病史重度异常（直接拒保）");
                r.setRiskLevel("拒保体");
                r.setUnderwritingResult("拒保");
                r.setPremiumAdjustment(BigDecimal.ONE);
                r.setTotalScore(0);
                return r;
            }
        }

        int total = 0;
        total += scoreAge(in.getAge(), r);
        total += scoreBmi(in.getBmi(), r);
        total += scoreBloodPressure(in.getBloodPressure(), r);
        total += scoreSmoking(in.getSmokingStatus(), r);
        total += scoreDrinking(in.getDrinkingStatus(), r);
        total += scorePersonalHistory(personal, r);
        total += scoreOccupation(safe(in.getOccupation()), r);
        total += scoreFamilyHistory(safe(in.getFamilyMedicalHistory()), r);

        r.setTotalScore(total);
        classify(total, r);
        return r;
    }

    private int scoreAge(Integer age, ScoreResult r) {
        if (age == null) return 0;
        for (JsonNode n : ruleService.section("age")) {
            if (age >= n.get("min").asInt() && age < n.get("max").asInt()) {
                int s = n.get("score").asInt();
                r.addBreakdown("年龄(" + n.get("label").asText() + ")", s);
                if (s > 0) r.addFactor("年龄" + n.get("label").asText());
                return s;
            }
        }
        return 0;
    }

    private int scoreBmi(BigDecimal bmi, ScoreResult r) {
        if (bmi == null) return 0;
        double v = bmi.doubleValue();
        for (JsonNode n : ruleService.section("bmi")) {
            if (v >= n.get("min").asDouble() && v < n.get("max").asDouble()) {
                int s = n.get("score").asInt();
                r.addBreakdown("BMI(" + n.get("label").asText() + ")", s);
                if (s > 0) r.addFactor("BMI" + n.get("label").asText());
                return s;
            }
        }
        return 0;
    }

    private int scoreBloodPressure(String bp, ScoreResult r) {
        // 血压为分类标签（正常/正常高值/临界高血压/轻度高血压/中度高血压）：按 label 精确匹配
        String label = safe(bp);
        if (label.isEmpty()) return 0;
        for (JsonNode n : ruleService.section("bloodPressure")) {
            if (label.equals(n.get("label").asText())) {
                int s = n.get("score").asInt();
                r.addBreakdown("血压(" + label + ")", s);
                if (s > 0) r.addFactor(label);
                return s;
            }
        }
        return 0;
    }

    private int scoreSmoking(String status, ScoreResult r) {
        return matchStatus("smoking", "吸烟", status, r);
    }

    private int scoreDrinking(String status, ScoreResult r) {
        return matchStatus("drinking", "饮酒", status, r);
    }

    private int matchStatus(String section, String dim, String status, ScoreResult r) {
        String s = safe(status);
        if (s.isEmpty()) return 0;
        // 规则按「否定/0分项在前」排列，命中即返回，避免"不吸烟"包含"吸烟"的子串误判。
        for (JsonNode n : ruleService.section(section)) {
            for (JsonNode kw : n.get("keywords")) {
                if (s.contains(kw.asText())) {
                    int sc = n.get("score").asInt();
                    if (sc > 0) {
                        r.addBreakdown(dim + "(" + n.get("status").asText() + ")", sc);
                        r.addFactor(n.get("status").asText());
                    }
                    return sc;
                }
            }
        }
        return 0;
    }

    private int scorePersonalHistory(String personal, ScoreResult r) {
        if (personal.isEmpty()) return 0;
        int total = 0;
        for (JsonNode rule : ruleService.section("personalHistory")) {
            if (rule.path("reject").asBoolean(false)) continue;
            if (containsAny(personal, rule.get("diseases"))) {
                int s = rule.get("score").asInt();
                total += s;
                r.addFactor("个人病史·" + rule.get("category").asText() + "(+" + s + ")");
            }
        }
        if (total > 0) r.addBreakdown("个人病史", total);
        return total;
    }

    private int scoreOccupation(String occupation, ScoreResult r) {
        if (occupation.isEmpty()) return 0;
        for (JsonNode n : ruleService.section("occupation")) {
            for (JsonNode occ : n.get("occupations")) {
                if (occupation.contains(occ.asText())) {
                    int s = n.get("score").asInt();
                    r.addBreakdown("职业(" + n.get("level").asText() + ")", s);
                    if (s > 0) r.addFactor("职业风险" + n.get("level").asText());
                    return s;
                }
            }
        }
        return 0;
    }

    private int scoreFamilyHistory(String family, ScoreResult r) {
        if (family.isEmpty()) return 0;
        for (JsonNode n : ruleService.section("familyHistory")) {
            if (containsAny(family, n.get("diseases"))) {
                int s = n.get("score").asInt();
                r.addBreakdown("家族病史(" + n.get("level").asText() + ")", s);
                if (s > 0) r.addFactor("家族病史" + n.get("level").asText());
                return s;
            }
        }
        return 0;
    }

    private void classify(int total, ScoreResult r) {
        for (JsonNode n : ruleService.section("levels")) {
            int min = n.get("minScore").asInt();
            int max = n.get("maxScore").asInt();
            if (min < 0) continue;
            if (total >= min && total <= max) {
                r.setRiskLevel(n.get("level").asText());
                r.setUnderwritingResult(n.get("result").asText());
                r.setPremiumAdjustment(new BigDecimal(n.get("premiumAdjustment").asText()));
                return;
            }
        }
        // 超出最高区间：按最高风险体处理
        JsonNode top = ruleService.section("levels").get(3);
        r.setRiskLevel(top.get("level").asText());
        r.setUnderwritingResult(top.get("result").asText());
        r.setPremiumAdjustment(new BigDecimal(top.get("premiumAdjustment").asText()));
    }

    private boolean containsAny(String text, JsonNode diseases) {
        if (text == null || text.isEmpty() || diseases == null) return false;
        for (JsonNode d : diseases) {
            if (text.contains(d.asText())) return true;
        }
        return false;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
