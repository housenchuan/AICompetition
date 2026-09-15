package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.query.AggregateQuery;
import com.aicompetition.service.StatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据汇总统计接口（统一 POST）。
 */
@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    /** 概览统计。body 可选 { "dateFrom":"yyyy-MM-dd", "dateTo":"yyyy-MM-dd" }（按申请日期过滤）。 */
    @PostMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestBody(required = false) Map<String, String> body) {
        String from = body == null ? null : body.get("dateFrom");
        String to = body == null ? null : body.get("dateTo");
        return Result.ok(statService.overview(from, to));
    }

    /** 按维度聚合。body: { entity, groupBy:[dim1,dim2?], dateFrom, dateTo }。单维返回 rows，双维返回 pivot。 */
    @PostMapping("/aggregate")
    public Result<Map<String, Object>> aggregate(@RequestBody AggregateQuery query) {
        try {
            return Result.ok(statService.aggregate(query));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
