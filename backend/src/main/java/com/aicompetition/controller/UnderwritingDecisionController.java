package com.aicompetition.controller;

import com.aicompetition.common.PageResult;
import com.aicompetition.common.Result;
import com.aicompetition.dto.AdjustRequest;
import com.aicompetition.dto.ReviewRequest;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.query.UnderwritingDecisionQuery;
import com.aicompetition.service.AdjustmentService;
import com.aicompetition.service.UnderwritingDecisionService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

/**
 * 核保决策画像分析结果表接口（统一 POST）。
 */
@RestController
@RequestMapping("/api/decisions")
public class UnderwritingDecisionController {

    private final UnderwritingDecisionService service;
    private final AdjustmentService adjustmentService;

    public UnderwritingDecisionController(UnderwritingDecisionService service, AdjustmentService adjustmentService) {
        this.service = service;
        this.adjustmentService = adjustmentService;
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

    // ===== 人工修整 / 分级审批 / 审计留痕（文件存储，不改表结构）=====

    /** 提交人工修整：核保专员→待审批；核保主管→免审直接生效。 */
    @PostMapping("/adjust/{decisionId}")
    public Result<ObjectNode> adjust(@PathVariable String decisionId, @RequestBody AdjustRequest req) {
        try {
            return Result.ok(adjustmentService.submit(decisionId, req));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 审批人工修整：仅核保主管。通过→覆写生效；驳回→仅留痕。 */
    @PostMapping("/adjust/{decisionId}/review")
    public Result<ObjectNode> review(@PathVariable String decisionId, @RequestBody ReviewRequest req) {
        try {
            return Result.ok(adjustmentService.review(decisionId, req));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 查询某决策的审计留痕 + AI 预测备份（供详情弹窗对比展示）。 */
    @PostMapping("/adjust/{decisionId}/audit")
    public Result<ObjectNode> audit(@PathVariable String decisionId) {
        return Result.ok(adjustmentService.getAudit(decisionId));
    }
}
