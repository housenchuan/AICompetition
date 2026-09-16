package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.entity.FeedbackItem;
import com.aicompetition.service.FeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/list")
    public Result<Map<String, Object>> list(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        int page = body.containsKey("page") ? ((Number) body.get("page")).intValue() : 1;
        int size = body.containsKey("size") ? ((Number) body.get("size")).intValue() : 10;
        String type = (String) body.get("type");
        String status = (String) body.get("status");
        String priority = (String) body.get("priority");
        String keyword = (String) body.get("keyword");
        return Result.ok(feedbackService.list(page, size, type, status, priority, keyword));
    }

    @PostMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(feedbackService.stats());
    }

    @PostMapping("/create")
    public Result<FeedbackItem> create(@RequestBody FeedbackItem item) {
        return Result.ok(feedbackService.create(item));
    }

    @PostMapping("/update/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody FeedbackItem patch) {
        boolean ok = feedbackService.update(id, patch);
        return ok ? Result.ok() : Result.fail("未找到对应反馈工单");
    }
}
