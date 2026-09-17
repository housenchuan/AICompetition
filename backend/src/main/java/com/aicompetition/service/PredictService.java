package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.entity.ai.ChatMessage;
import com.aicompetition.entity.ai.ChatResponse;
import com.aicompetition.mapper.PolicyApplicationMapper;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aicompetition.common.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 核保预测服务：规则引擎给出确定性评分/等级/结论/加费，
 * 关键风险因子文本可由 LLM 增强（ai.predict.use-llm=true 且内网可达时），否则用规则拆解兜底。
 */
@Service
public class PredictService {

    private static final Logger log = LoggerFactory.getLogger(PredictService.class);

    private final RiskScoreEngine engine;
    private final UnderwritingDecisionMapper decisionMapper;
    private final PolicyApplicationMapper policyApplicationMapper;
    private final RuleService ruleService;
    private final AiService aiService;
    private final AdjustmentService adjustmentService;

    @Value("${ai.predict.use-llm:true}")
    private boolean useLlm;

    public PredictService(RiskScoreEngine engine, UnderwritingDecisionMapper decisionMapper,
                          PolicyApplicationMapper policyApplicationMapper, RuleService ruleService,
                          AiService aiService, AdjustmentService adjustmentService) {
        this.engine = engine;
        this.decisionMapper = decisionMapper;
        this.policyApplicationMapper = policyApplicationMapper;
        this.ruleService = ruleService;
        this.aiService = aiService;
        this.adjustmentService = adjustmentService;
    }

    /** 仅试算，不落库。 */
    public ScoreResult preview(UnderwritingDecision input) {
        return engine.compute(input);
    }

    /** 单条预测并回写 AI 生成字段。 */
    public UnderwritingDecision predictOne(String decisionId) {
        UnderwritingDecision d = decisionMapper.selectById(decisionId);
        if (d == null) {
            throw new IllegalArgumentException("核保决策记录不存在：" + decisionId);
        }
        ScoreResult r = engine.compute(d, hasRecentRejection(d));
        d.setRiskScore(r.getTotalScore());
        d.setRiskLevel(r.getRiskLevel());
        d.setUnderwritingResult(r.getUnderwritingResult());
        d.setPremiumAdjustment(r.getPremiumAdjustment());
        d.setKeyFactors(buildFactors(r, d));
        // 创建时间为空（预测前未落库）则补为当前时间；已有则保持原值
        if (d.getCreatedAt() == null) {
            d.setCreatedAt(DateUtils.now());
        }
        d.setUpdatedAt(DateUtils.now());
        decisionMapper.updatePrediction(d);
        UnderwritingDecision saved = decisionMapper.selectById(decisionId);
        // 备份本次 AI 预测结果（供审核对比、永久保留，不受后续人工覆写影响）
        adjustmentService.saveAiBaseline(saved);
        return saved;
    }

    /**
     * 方案A：同一投保人在当前申请日前 N 个月内是否有 status='已拒保' 的其它申请（排除当前申请）。
     * 参照日取当前申请的 application_date（绝不用 now()，数据为历史数据）；取不到参照日则跳过（视为无）。
     */
    private boolean hasRecentRejection(UnderwritingDecision d) {
        if (d.getApplicationId() == null || d.getCustomerId() == null) return false;
        JsonNode cfg = ruleService.section("recentRejectionReject");
        if (cfg == null || !cfg.path("reject").asBoolean(false)) return false;
        PolicyApplication cur = policyApplicationMapper.selectById(d.getApplicationId());
        if (cur == null || cur.getApplicationDate() == null) return false;
        LocalDate refDate = cur.getApplicationDate();
        LocalDate from = refDate.minusMonths(cfg.path("months").asInt(6));
        int cnt = policyApplicationMapper.countRecentRejections(
                d.getCustomerId(), cfg.path("status").asText("已拒保"),
                from, refDate, d.getApplicationId());
        return cnt > 0;
    }

    /** 批量预测。 */
    public List<UnderwritingDecision> predictBatch(List<String> decisionIds) {
        List<UnderwritingDecision> results = new ArrayList<>();
        if (decisionIds == null) return results;
        for (String id : decisionIds) {
            try {
                results.add(predictOne(id));
            } catch (Exception e) {
                log.warn("预测失败 decisionId={}: {}", id, e.getMessage());
            }
        }
        return results;
    }

    private String buildFactors(ScoreResult r, UnderwritingDecision d) {
        String ruleFactors = r.getKeyFactors().isEmpty() ? "无显著风险因素" : String.join("；", r.getKeyFactors());
        String summary = ruleFactors;
        if (useLlm) {
            try {
                summary = llmFactors(r, d, ruleFactors);
            } catch (Exception e) {
                log.warn("LLM 关键因子生成失败，降级为规则拆解：{}", e.getMessage());
            }
        }
        String breakdown = buildBreakdown(r, d);
        return breakdown.isEmpty() ? summary : breakdown + " ｜ " + summary;
    }

    /** 各维度加分明细：只列分值>0 的维度，按分值降序，BMI 带原始值。拒保时不列。 */
    private String buildBreakdown(ScoreResult r, UnderwritingDecision d) {
        if (r.isRejected()) return "";
        List<Map.Entry<String, Integer>> items = new ArrayList<>();
        for (Map.Entry<String, Integer> e : r.getBreakdown().entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) items.add(e);
        }
        items.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : items) {
            if (sb.length() > 0) sb.append("、");
            sb.append(dimLabel(e.getKey(), d)).append("(+").append(e.getValue()).append(")");
        }
        return sb.toString();
    }

    /** 把 breakdown 的 key 归一成简洁维度名；BMI 带原始值。 */
    private String dimLabel(String key, UnderwritingDecision d) {
        if (key.startsWith("BMI")) {
            String v = d.getBmi() == null ? "" : d.getBmi().stripTrailingZeros().toPlainString();
            return "BMI" + v;
        }
        if (key.startsWith("血压")) return "血压";
        if (key.startsWith("吸烟")) return "吸烟";
        if (key.startsWith("饮酒")) return "饮酒";
        if (key.startsWith("年龄")) return "年龄";
        if (key.startsWith("职业")) return "职业";
        if (key.startsWith("家族")) return "家族史";
        if (key.startsWith("个人病史")) return "病史";
        return key;
    }

    private String llmFactors(ScoreResult r, UnderwritingDecision d, String ruleFactors) {
        String sys = "你是资深保险核保专家。根据投保人画像与规则评分结果，用一句话概括关键风险因子与核保建议，直接给结论，不要解释过程。";
        String user = String.format(
                "画像：年龄%s，性别%s，职业%s，BMI%s，血压%s，吸烟%s，饮酒%s，个人病史[%s]，家族病史[%s]。" +
                        "规则评分：总分%d，等级%s，结论%s。规则命中因子：%s。",
                d.getAge(), d.getGender(), d.getOccupation(), d.getBmi(), d.getBloodPressure(),
                d.getSmokingStatus(), d.getDrinkingStatus(), d.getPersonalMedicalHistory(), d.getFamilyMedicalHistory(),
                r.getTotalScore(), r.getRiskLevel(), r.getUnderwritingResult(), ruleFactors);
        List<ChatMessage> messages = List.of(ChatMessage.system(sys), ChatMessage.user(user));
        ChatResponse resp = aiService.chat(messages, 0.3, 256).block(java.time.Duration.ofSeconds(20));
        if (resp != null && resp.getChoices() != null && !resp.getChoices().isEmpty()) {
            String content = resp.getChoices().get(0).getMessage().getContent();
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return ruleFactors;
    }
}
