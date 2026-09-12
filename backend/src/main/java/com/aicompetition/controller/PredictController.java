package com.aicompetition.controller;

import com.aicompetition.common.Result;
import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.service.PredictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 核保预测接口：试算 / 单条 / 批量（统一 POST）。
 */
@RestController
@RequestMapping("/api/predict")
public class PredictController {

    private final PredictService predictService;

    public PredictController(PredictService predictService) {
        this.predictService = predictService;
    }

    /** 试算：传入画像，返回评分结果，不落库。 */
    @PostMapping("/preview")
    public Result<ScoreResult> preview(@RequestBody UnderwritingDecision input) {
        return Result.ok(predictService.preview(input));
    }

    /** 单条预测并回写。 */
    @PostMapping("/single/{decisionId}")
    public Result<UnderwritingDecision> single(@PathVariable String decisionId) {
        return Result.ok(predictService.predictOne(decisionId));
    }

    /** 批量预测：body { "ids": ["D001","D002"] }。 */
    @PostMapping("/batch")
    public Result<List<UnderwritingDecision>> batch(@RequestBody Map<String, List<String>> body) {
        return Result.ok(predictService.predictBatch(body.get("ids")));
    }
}
