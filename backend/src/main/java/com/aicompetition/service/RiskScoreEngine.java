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

    /** 便捷入口：无历史拒保上下文（如 preview 试算）。 */
    public ScoreResult compute(UnderwritingDecision in) {
        return compute(in, false);
    }

    /**
     * @param hasRecentRejection 近6个月内该客户是否曾有拒保记录（跨表事实，由 PredictService 查得后传入，
     *                           引擎不做 I/O，仅按事实做确定性判定）。
     */
    public ScoreResult compute(UnderwritingDecision in, boolean hasRecentRejection) {
        ScoreResult r = new ScoreResult();

        // 直接拒保①：近6个月内曾有拒保记录
        JsonNode recentReject = ruleService.section("recentRejectionReject");
        if (hasRecentRejection && recentReject != null && recentReject.path("reject").asBoolean(false)) {
            return reject(r, recentReject.path("factor").asText("近6个月内曾有拒保记录（直接拒保）"));
        }

        // 直接拒保②：饮酒状况含「瘾」字（酒精成瘾）；受控词表正常值 是/否/偶尔 不含「瘾」，无误判
        JsonNode drinkingReject = ruleService.section("drinkingReject");
        if (drinkingReject != null && drinkingReject.path("reject").asBoolean(false)
                && safe(in.getDrinkingStatus()).contains(drinkingReject.path("keyword").asText("瘾"))) {
            return reject(r, drinkingReject.path("factor").asText("酒精成瘾（直接拒保）"));
        }

        // 直接拒保③：个人病史命中重度异常疾病
        String personal = safe(in.getPersonalMedicalHistory());
        for (JsonNode rule : ruleService.section("personalHistory")) {
            if (rule.path("reject").asBoolean(false) && containsAny(personal, rule.get("diseases"))) {
                return reject(r, "个人病史重度异常（直接拒保）");
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

    /** 统一构造「直接拒保」结果：拒保体、总分0、加费系数1、附拒保因子。 */
    private ScoreResult reject(ScoreResult r, String factor) {
        r.setRejected(true);
        r.addFactor(factor);
        r.setRiskLevel("拒保体");
        r.setUnderwritingResult("拒保");
        r.setPremiumAdjustment(BigDecimal.ONE);
        r.setTotalScore(0);
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
        // 直接按数据库保存的 BMI 原值（最多两位小数）进行区间判断，不做四舍五入。
        // 边界重叠（18.5、32.0）时取 min 更大的档位，
        // 即 18.5 归正常范围、32.0 归严重肥胖。
        BigDecimal v = bmi;
        JsonNode matched = null;
        for (JsonNode n : ruleService.section("bmi")) {
            BigDecimal min = n.get("min").decimalValue();
            BigDecimal max = n.get("max").decimalValue();
            if (v.compareTo(min) >= 0 && v.compareTo(max) <= 0
                    && (matched == null || min.compareTo(matched.get("min").decimalValue()) > 0)) {
                matched = n;
            }
        }
        if (matched == null) return 0;
        int s = matched.get("score").asInt();
        r.addBreakdown("BMI(" + matched.get("label").asText() + ")", s);
        if (s > 0) r.addFactor("BMI" + matched.get("label").asText());
        return s;
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
        // 个人病史命中多个规则项时，仅取最高风险分；同一规则项包含多个疾病关键词，
        // containsAny 只返回一次命中，因此不会在规则项内重复计分。
        JsonNode matched = null;
        for (JsonNode rule : ruleService.section("personalHistory")) {
            if (rule.path("reject").asBoolean(false)) continue;
            if (containsAny(personal, rule.get("diseases"))) {
                if (matched == null || rule.get("score").asInt() > matched.get("score").asInt()) {
                    matched = rule;
                }
            }
        }
        if (matched == null) return 0;
        int score = matched.get("score").asInt();
        r.addBreakdown("个人病史", score);
        r.addFactor("个人病史·" + matched.get("category").asText() + "(+" + score + ")");
        return score;
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
        // 家族病史命中多个规则项时，只取最高风险分；同一规则项内多个疾病关键词只算一次。
        JsonNode matched = null;
        for (JsonNode n : ruleService.section("familyHistory")) {
            if (containsAny(family, n.get("diseases"))) {
                if (matched == null || n.get("score").asInt() > matched.get("score").asInt()) {
                    matched = n;
                }
            }
        }
        if (matched == null) return 0;
        int score = matched.get("score").asInt();
        r.addBreakdown("家族病史(" + matched.get("level").asText() + ")", score);
        if (score > 0) r.addFactor("家族病史" + matched.get("level").asText());
        return score;
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
        // 正常配置下每个可达总分都能命中某一档（各维度加分均为5的倍数，区间已全覆盖）。
        // 若走到这里，说明 levels 区间配置有漏档：快速失败暴露问题，而非悄悄误判等级。
        throw new IllegalStateException("总分 " + total + " 未命中任何风险等级区间，请检查 risk-rules.json 的 levels 配置");
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
