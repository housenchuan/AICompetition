package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.service.RuleSuggestionService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 创新点⑤：反馈驱动的规则优化闭环接口（文件存储）。
 */
@RestController
@RequestMapping("/api/rule-suggestions")
public class RuleSuggestionController {

    private final RuleSuggestionService service;

    public RuleSuggestionController(RuleSuggestionService service) {
        this.service = service;
    }

    /** 由反馈一键转规则优化建议。body: {title, content, suggestedChange}（可空，缺省取反馈内容）。 */
    @PostMapping("/from-feedback/{feedbackId}")
    public Result<ObjectNode> fromFeedback(@PathVariable String feedbackId,
                                           @RequestBody(required = false) Map<String, String> body) {
        try {
            Map<String, String> b = body == null ? Map.of() : body;
            return Result.ok(service.fromFeedback(feedbackId, b.get("title"), b.get("content"), b.get("suggestedChange")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 列表（可按 status 过滤）。 */
    @PostMapping("/list")
    public Result<ArrayNode> list(@RequestBody(required = false) Map<String, String> body) {
        String status = body == null ? null : body.get("status");
        return Result.ok(service.listAll(status));
    }

    /** 确认：采纳/驳回。body: {adopt: true/false, reviewer}。 */
    @PostMapping("/{id}/review")
    public Result<ObjectNode> review(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            boolean adopt = Boolean.TRUE.equals(body.get("adopt"));
            String reviewer = body.get("reviewer") == null ? null : String.valueOf(body.get("reviewer"));
            return Result.ok(service.review(id, adopt, reviewer));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 闭环成效统计。 */
    @PostMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(service.stats());
    }

    /** 已转过规则建议的反馈 id 列表（前端据此隐藏「转规则建议」按钮）。 */
    @PostMapping("/feedback-ids")
    public Result<java.util.List<String>> feedbackIds() {
        return Result.ok(service.convertedFeedbackIds());
    }
}
