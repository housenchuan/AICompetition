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

    @Test
    void BMI图片边界值_严格按规则表计分() {
        assertBmiScore("18.4", 10);
        assertBmiScore("18.5", 0);
        assertBmiScore("24.9", 0);
        assertBmiScore("25.0", 5);
        assertBmiScore("28.0", 5);
        assertBmiScore("28.1", 15);
        assertBmiScore("31.9", 15);
        assertBmiScore("32.0", 30);
    }

    @Test
    void BMI两位小数_直接按原值判断不四舍五入() {
        assertBmiScore("18.45", 10);
        assertBmiScore("24.89", 0);
        assertBmiScore("25.01", 5);
        assertBmiScore("28.09", 0);
        assertBmiScore("28.11", 15);
        assertBmiScore("31.95", 15);
    }

    @Test
    void 个人病史命中多个规则项_只取最高分() {
        UnderwritingDecision d = base();
        d.setPersonalMedicalHistory("胃炎、颈椎病、高血压");

        ScoreResult r = engine.compute(d);

        assertEquals(25, r.getTotalScore());
        assertEquals(25, r.getBreakdown().get("个人病史"));
        assertEquals(1, r.getKeyFactors().stream().filter(f -> f.startsWith("个人病史·")).count());
    }

    @Test
    void 个人病史同一规则项命中多个疾病_只计一次() {
        UnderwritingDecision d = base();
        d.setPersonalMedicalHistory("胃炎、胆结石、甲状腺结节");

        ScoreResult r = engine.compute(d);

        assertEquals(10, r.getTotalScore());
        assertEquals(10, r.getBreakdown().get("个人病史"));
    }

    @Test
    void 个人病史命中直接拒保疾病_优先拒保不参与最高分比较() {
        UnderwritingDecision d = base();
        d.setPersonalMedicalHistory("高血压、恶性肿瘤");

        ScoreResult r = engine.compute(d);

        assertTrue(r.isRejected());
        assertEquals(0, r.getTotalScore());
        assertEquals("拒保体", r.getRiskLevel());
    }

    @Test
    void 家族病史命中多个规则项_只取最高分() {
        UnderwritingDecision d = base();
        d.setFamilyMedicalHistory("高血压、恶性肿瘤");

        ScoreResult r = engine.compute(d);

        assertEquals(15, r.getTotalScore());
        assertEquals(15, r.getBreakdown().get("家族病史(高风险)"));
    }

    @Test
    void 家族病史同一规则项命中多个疾病_只计一次() {
        UnderwritingDecision d = base();
        d.setFamilyMedicalHistory("高血压、糖尿病、心脏病");

        ScoreResult r = engine.compute(d);

        assertEquals(5, r.getTotalScore());
        assertEquals(5, r.getBreakdown().get("家族病史(中风险)"));
    }

    private void assertBmiScore(String bmi, int expectedScore) {
        UnderwritingDecision d = base();
        d.setBmi(new BigDecimal(bmi));
        assertEquals(expectedScore, engine.compute(d).getTotalScore(), "BMI=" + bmi);
    }
}
