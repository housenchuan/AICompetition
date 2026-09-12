package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.service.RiskScoreEngine;
import com.aicompetition.service.RuleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

/**
 * 风险计分规则接口：规则展示（2.1.x）+ 规则引擎试算（骨架）。
 */
@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;
    private final RiskScoreEngine engine;

    public RuleController(RuleService ruleService, RiskScoreEngine engine) {
        this.ruleService = ruleService;
        this.engine = engine;
    }

    /** 完整风险计分规则（供前端规则展示页）。 */
    @PostMapping("/list")
    public Result<JsonNode> rules() {
        return Result.ok(ruleService.getRules());
    }

    /** 规则引擎试算（仅确定性计算，不含 LLM 语义），用于演示与自测。 */
    @PostMapping("/try-score")
    public Result<ScoreResult> tryScore(@RequestBody UnderwritingDecision input) {
        return Result.ok(engine.compute(input));
    }
}
