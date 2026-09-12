package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.dto.IntentResult;
import com.aicompetition.service.NlService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 自然语言指令接口：解析用户口令为结构化意图（QUERY/PREDICT/AGGREGATE）。
 * 执行路由（查询/预测/统计）将在后续接入。
 */
@RestController
@RequestMapping("/api/nl")
public class NlController {

    private final NlService nlService;

    public NlController(NlService nlService) {
        this.nlService = nlService;
    }

    @PostMapping("/parse")
    public Result<IntentResult> parse(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return Result.fail("text 不能为空");
        }
        return Result.ok(nlService.parse(text));
    }
}
