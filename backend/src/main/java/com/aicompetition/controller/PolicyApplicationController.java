package com.aicompetition.controller;

import com.aicompetition.common.PageResult;
import com.aicompetition.common.Result;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.query.PolicyApplicationQuery;
import com.aicompetition.service.PolicyApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 投保申请记录表接口（统一 POST）。
 */
@RestController
@RequestMapping("/api/applications")
public class PolicyApplicationController {

    private final PolicyApplicationService service;

    public PolicyApplicationController(PolicyApplicationService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<PolicyApplication>> page(@RequestBody(required = false) PolicyApplicationQuery query) {
        return Result.ok(service.page(query == null ? new PolicyApplicationQuery() : query));
    }

    @PostMapping("/detail/{applicationId}")
    public Result<PolicyApplication> detail(@PathVariable String applicationId) {
        return Result.ok(service.getById(applicationId));
    }

    /** 投保申请 + 核保决策结果关联查询。 */
    @PostMapping("/with-decision/{applicationId}")
    public Result<Map<String, Object>> withDecision(@PathVariable String applicationId) {
        return Result.ok(service.getWithDecision(applicationId));
    }

    @PostMapping("/create")
    public Result<PolicyApplication> create(@RequestBody PolicyApplication entity) {
        return Result.ok(service.create(entity));
    }

    @PostMapping("/update/{applicationId}")
    public Result<PolicyApplication> update(@PathVariable String applicationId, @RequestBody PolicyApplication entity) {
        entity.setApplicationId(applicationId);
        return Result.ok(service.update(entity));
    }

    @PostMapping("/delete/{applicationId}")
    public Result<Integer> delete(@PathVariable String applicationId) {
        return Result.ok(service.delete(applicationId));
    }
}
