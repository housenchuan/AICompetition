package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.entity.ai.ChatMessage;
import com.aicompetition.entity.ai.ChatResponse;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 核保预测服务：规则引擎给出确定性评分/等级/结论/加费，
 * 关键风险因子文本可由 LLM 增强（ai.predict.use-llm=true 且内网可达时），否则用规则拆解兜底。
 */
@Service
public class PredictService {

    private static final Logger log = LoggerFactory.getLogger(PredictService.class);

    private final RiskScoreEngine engine;
    private final UnderwritingDecisionMapper decisionMapper;
    private final AiService aiService;

    @Value("${ai.predict.use-llm:true}")
    private boolean useLlm;

    public PredictService(RiskScoreEngine engine, UnderwritingDecisionMapper decisionMapper, AiService aiService) {
        this.engine = engine;
        this.decisionMapper = decisionMapper;
        this.aiService = aiService;
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
        ScoreResult r = engine.compute(d);
        d.setRiskScore(r.getTotalScore());
        d.setRiskLevel(r.getRiskLevel());
        d.setUnderwritingResult(r.getUnderwritingResult());
        d.setPremiumAdjustment(r.getPremiumAdjustment());
        d.setKeyFactors(buildFactors(r, d));
        d.setUpdatedAt(LocalDateTime.now());
        decisionMapper.updatePrediction(d);
        return decisionMapper.selectById(decisionId);
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
        if (!useLlm) {
            return ruleFactors;
        }
        try {
            return llmFactors(r, d, ruleFactors);
        } catch (Exception e) {
            log.warn("LLM 关键因子生成失败，降级为规则拆解：{}", e.getMessage());
            return ruleFactors;
        }
    }

    private String llmFactors(ScoreResult r, UnderwritingDecision d, String ruleFactors) {
        String sys = "你是资深保险核保专家。根据投保人画像与规则评分结果，用一句话（不超过 60 字）概括关键风险因子与核保建议，直接给结论，不要解释过程。";
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
