package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 置信度评分与智能路由单测：验证「越可信分越高、越可路由自动通过」的确定性逻辑。
 * 用真实规则引擎（读 classpath 规则），不落库依赖。
 */
class ConfidenceServiceTest {

    private ConfidenceService svc;

    @BeforeEach
    void setUp() throws Exception {
        RuleService ruleService = new RuleService(new ObjectMapper());
        ruleService.load();
        // 隔离测试写盘：删除可能存在的旧文件
        new File("data/confidence.json").delete();
        svc = new ConfidenceService(ruleService);
    }

    private UnderwritingDecision decision(String id) {
        UnderwritingDecision d = new UnderwritingDecision();
        d.setDecisionId(id);
        d.setAge(30);
        d.setBmi(new BigDecimal("22.0"));
        d.setBloodPressure("正常");
        return d;
    }

    @Test
    void 健康标准体_高置信度_自动通过() {
        ScoreResult r = new ScoreResult();
        r.setTotalScore(20);
        r.setRiskLevel("标准体");
        r.setUnderwritingResult("标保（标准费率承保）");
        r.setPremiumAdjustment(BigDecimal.ONE);

        ObjectNode res = svc.evaluate(decision("T1"), r);
        assertTrue(res.get("confidence").asInt() >= ConfidenceService.TH_AUTO, "健康标准体应为高置信度");
        assertEquals(ConfidenceService.P_AUTO, res.get("priority").asText());
    }

    @Test
    void 拒保结论_置信度下降_不自动通过() {
        ScoreResult r = new ScoreResult();
        r.setRejected(true);
        r.setTotalScore(0);
        r.setRiskLevel("拒保体");
        r.setUnderwritingResult("拒保");
        r.setPremiumAdjustment(BigDecimal.ONE);

        ObjectNode res = svc.evaluate(decision("T2"), r);
        assertTrue(res.get("confidence").asInt() < ConfidenceService.TH_AUTO, "拒保高风险决策应扣分、不自动放行");
        assertNotEquals(ConfidenceService.P_AUTO, res.get("priority").asText());
        assertTrue(res.get("signals").size() >= 1, "应记录可解释扣分信号");
    }

    @Test
    void 大幅加费叠加临界分_路由高优人工() {
        ScoreResult r = new ScoreResult();
        r.setTotalScore(85); // 高风险体下界，临界
        r.setRiskLevel("高风险体");
        r.setUnderwritingResult("延期承保并加费50%以上");
        r.setPremiumAdjustment(new BigDecimal("1.60"));
        r.addBreakdown("个人病史", 30);

        ObjectNode res = svc.evaluate(decision("T3"), r);
        assertEquals(ConfidenceService.P_HIGH, res.get("priority").asText());
    }

    @Test
    void 优先级路由阈值() {
        assertEquals(ConfidenceService.P_AUTO, ConfidenceService.priorityOf(90));
        assertEquals(ConfidenceService.P_NORMAL, ConfidenceService.priorityOf(75));
        assertEquals(ConfidenceService.P_HIGH, ConfidenceService.priorityOf(50));
    }
}
