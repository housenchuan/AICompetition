package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 风险计分规则引擎单测：重点覆盖两条新增「直接拒保」规则（酒精成瘾 / 近6个月拒保记录），
 * 因为真实样本数据里不含触发场景，只能靠构造输入验证。
 */
class RiskScoreEngineTest {

    private RiskScoreEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        RuleService ruleService = new RuleService(new ObjectMapper());
        ruleService.load();
        engine = new RiskScoreEngine(ruleService);
    }

    private UnderwritingDecision base() {
        UnderwritingDecision d = new UnderwritingDecision();
        d.setAge(30);
        d.setBmi(new BigDecimal("22.0"));
        d.setBloodPressure("正常");
        d.setSmokingStatus("否");
        d.setDrinkingStatus("否");
        d.setPersonalMedicalHistory("无");
        d.setFamilyMedicalHistory("无");
        d.setOccupation("公司职员");
        return d;
    }

    @Test
    void 饮酒含瘾字_判酒精成瘾拒保() {
        UnderwritingDecision d = base();
        d.setDrinkingStatus("酒精成瘾");
        ScoreResult r = engine.compute(d);
        assertTrue(r.isRejected());
        assertEquals("拒保体", r.getRiskLevel());
        assertEquals(0, r.getTotalScore());
        assertTrue(r.getKeyFactors().stream().anyMatch(f -> f.contains("酒精成瘾")));
    }

    @Test
    void 饮酒受控词表正常值_不误判成瘾() {
        for (String v : new String[]{"是", "否", "偶尔"}) {
            UnderwritingDecision d = base();
            d.setDrinkingStatus(v);
            ScoreResult r = engine.compute(d);
            assertFalse(r.isRejected(), "饮酒=" + v + " 不应判成瘾拒保");
        }
    }

    @Test
    void 近6个月内曾拒保_直接拒保() {
        UnderwritingDecision d = base();
        ScoreResult r = engine.compute(d, true);
        assertTrue(r.isRejected());
        assertEquals("拒保体", r.getRiskLevel());
        assertEquals(0, r.getTotalScore());
        assertTrue(r.getKeyFactors().stream().anyMatch(f -> f.contains("拒保记录")));
    }

    @Test
    void 无历史拒保标志_健康体正常算分() {
        ScoreResult r = engine.compute(base(), false);
        assertFalse(r.isRejected());
        assertNotEquals("拒保体", r.getRiskLevel());
    }
}
