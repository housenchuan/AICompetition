package com.aicompetition.controller;

import com.aicompetition.common.PageResult;
import com.aicompetition.common.Result;
import com.aicompetition.dto.ApplicationDecisionVO;
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

    /** 投保申请 + 核保决策结果 关联分页查询。 */
    @PostMapping("/page-joined")
    public Result<PageResult<ApplicationDecisionVO>> pageJoined(@RequestBody(required = false) PolicyApplicationQuery query) {
        return Result.ok(service.pageWithDecision(query == null ? new PolicyApplicationQuery() : query));
    }

    @PostMapping("/detail/{profileId}")
    public Result<PolicyApplication> detail(@PathVariable String profileId) {
        return Result.ok(service.getById(profileId));
    }

    /** 投保申请 + 核保决策结果关联查询。 */
    @PostMapping("/with-decision/{profileId}")
    public Result<Map<String, Object>> withDecision(@PathVariable String profileId) {
        return Result.ok(service.getWithDecision(profileId));
    }

    @PostMapping("/create")
    public Result<PolicyApplication> create(@RequestBody PolicyApplication entity) {
        return Result.ok(service.create(entity));
    }

    @PostMapping("/update/{profileId}")
    public Result<PolicyApplication> update(@PathVariable String profileId, @RequestBody PolicyApplication entity) {
        entity.setProfileId(profileId);
        return Result.ok(service.update(entity));
    }

    @PostMapping("/delete/{profileId}")
    public Result<Integer> delete(@PathVariable String profileId) {
        return Result.ok(service.delete(profileId));
    }
}
