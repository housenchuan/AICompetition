package com.aicompetition.controller;

import com.aicompetition.common.PageResult;
import com.aicompetition.common.Result;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.query.UnderwritingDecisionQuery;
import com.aicompetition.service.UnderwritingDecisionService;
import org.springframework.web.bind.annotation.*;

/**
 * 核保决策画像分析结果表接口（统一 POST）。
 */
@RestController
@RequestMapping("/api/decisions")
public class UnderwritingDecisionController {

    private final UnderwritingDecisionService service;

    public UnderwritingDecisionController(UnderwritingDecisionService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<UnderwritingDecision>> page(@RequestBody(required = false) UnderwritingDecisionQuery query) {
        return Result.ok(service.page(query == null ? new UnderwritingDecisionQuery() : query));
    }

    @PostMapping("/detail/{decisionId}")
    public Result<UnderwritingDecision> detail(@PathVariable String decisionId) {
        return Result.ok(service.getById(decisionId));
    }

    @PostMapping("/by-application/{applicationId}")
    public Result<UnderwritingDecision> byApplication(@PathVariable String applicationId) {
        return Result.ok(service.getByApplicationId(applicationId));
    }

    @PostMapping("/create")
    public Result<UnderwritingDecision> create(@RequestBody UnderwritingDecision entity) {
        return Result.ok(service.create(entity));
    }

    @PostMapping("/update/{decisionId}")
    public Result<UnderwritingDecision> update(@PathVariable String decisionId, @RequestBody UnderwritingDecision entity) {
        entity.setDecisionId(decisionId);
        return Result.ok(service.update(entity));
    }

    @PostMapping("/delete/{decisionId}")
    public Result<Integer> delete(@PathVariable String decisionId) {
        return Result.ok(service.delete(decisionId));
    }
}
