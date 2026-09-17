package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.dto.IntentResult;
import com.aicompetition.service.NlService;
import com.aicompetition.service.NlSqlService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 自然语言指令接口：
 * /parse 意图解析（查询/预测/统计路由用）；
 * /stats 自然语言统计（text-to-SQL：表结构+问题传给爱码 LLM 生成 SELECT，安全校验后执行）。
 */
@RestController
@RequestMapping("/api/nl")
public class NlController {

    private final NlService nlService;
    private final NlSqlService nlSqlService;

    public NlController(NlService nlService, NlSqlService nlSqlService) {
        this.nlService = nlService;
        this.nlSqlService = nlSqlService;
    }

    @PostMapping("/parse")
    public Result<IntentResult> parse(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return Result.fail("text 不能为空");
        }
        return Result.ok(nlService.parse(text));
    }

    /**
     * 自然语言统计（text-to-SQL）：表结构与统计问题传给爱码 LLM 生成 SELECT，
     * 程序安全校验（单条只读、表白名单、禁关键字、强制 LIMIT 200）后执行并返回列+行结果。
     */
    @PostMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return Result.fail("text 不能为空");
        }
        try {
            return Result.ok(nlSqlService.stats(text));
        } catch (Exception e) {
            return Result.fail("自然语言统计失败：" + e.getMessage());
        }
    }
}
