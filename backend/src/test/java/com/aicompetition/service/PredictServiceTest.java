package com.aicompetition.service;

import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.PolicyApplicationMapper;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预测服务单测：验证方案A「近6个月内曾有拒保记录」的跨表查询接线。
 * 用真实规则引擎 + mock mapper；useLlm 未经 Spring 注入默认 false，故不触达 LLM。
 */
class PredictServiceTest {

    private UnderwritingDecisionMapper decisionMapper;
    private PolicyApplicationMapper policyApplicationMapper;
    private PredictService service;

    @BeforeEach
    void setUp() throws Exception {
        RuleService ruleService = new RuleService(new ObjectMapper());
        ruleService.load();
        RiskScoreEngine engine = new RiskScoreEngine(ruleService);
        decisionMapper = mock(UnderwritingDecisionMapper.class);
        policyApplicationMapper = mock(PolicyApplicationMapper.class);
        AiService aiService = mock(AiService.class);
        AdjustmentService adjustmentService = mock(AdjustmentService.class);
        service = new PredictService(engine, decisionMapper, policyApplicationMapper, ruleService, aiService, adjustmentService);
    }

    private UnderwritingDecision healthyDecision() {
        UnderwritingDecision d = new UnderwritingDecision();
        d.setDecisionId("D999");
        d.setApplicationId("A999");
        d.setCustomerId("C999");
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
    void 近6个月内有拒保记录_当前单直接拒保() {
        UnderwritingDecision d = healthyDecision();
        when(decisionMapper.selectById("D999")).thenReturn(d);
        PolicyApplication cur = new PolicyApplication();
        cur.setApplicationDate(LocalDate.of(2024, 9, 1));
        when(policyApplicationMapper.selectById("A999")).thenReturn(cur);
        when(policyApplicationMapper.countRecentRejections(eq("C999"), eq("已拒保"),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 9, 1)), eq("A999"))).thenReturn(1);

        service.predictOne("D999");

        assertEquals("拒保体", d.getRiskLevel());
        assertEquals(0, d.getRiskScore());
        verify(decisionMapper).updatePrediction(d);
    }

    @Test
    void 无历史拒保_健康体不拒保() {
        UnderwritingDecision d = healthyDecision();
        when(decisionMapper.selectById("D999")).thenReturn(d);
        PolicyApplication cur = new PolicyApplication();
        cur.setApplicationDate(LocalDate.of(2024, 9, 1));
        when(policyApplicationMapper.selectById("A999")).thenReturn(cur);
        when(policyApplicationMapper.countRecentRejections(any(), any(), any(), any(), any())).thenReturn(0);

        service.predictOne("D999");

        assertNotEquals("拒保体", d.getRiskLevel());
    }

    @Test
    void 创建时间为空_预测时补为当前时间() {
        UnderwritingDecision d = healthyDecision();
        d.setCreatedAt(null);
        when(decisionMapper.selectById("D999")).thenReturn(d);
        when(policyApplicationMapper.selectById("A999")).thenReturn(new PolicyApplication());
        when(policyApplicationMapper.countRecentRejections(any(), any(), any(), any(), any())).thenReturn(0);

        service.predictOne("D999");

        assertNotNull(d.getCreatedAt());
    }

    @Test
    void 创建时间已有_预测时保持不变() {
        UnderwritingDecision d = healthyDecision();
        java.time.LocalDateTime existing = java.time.LocalDateTime.of(2024, 1, 1, 10, 0);
        d.setCreatedAt(existing);
        when(decisionMapper.selectById("D999")).thenReturn(d);
        when(policyApplicationMapper.selectById("A999")).thenReturn(new PolicyApplication());
        when(policyApplicationMapper.countRecentRejections(any(), any(), any(), any(), any())).thenReturn(0);

        service.predictOne("D999");

        assertEquals(existing, d.getCreatedAt());
    }
}
