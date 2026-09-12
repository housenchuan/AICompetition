package com.aicompetition.controller;

import com.aicompetition.common.PageResult;
import com.aicompetition.common.Result;
import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.query.CustomerRiskHisQuery;
import com.aicompetition.service.CustomerRiskHisService;
import org.springframework.web.bind.annotation.*;

/**
 * 历史客户风险画像分析表接口（统一 POST）。
 */
@RestController
@RequestMapping("/api/customer-risk")
public class CustomerRiskHisController {

    private final CustomerRiskHisService service;

    public CustomerRiskHisController(CustomerRiskHisService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<CustomerRiskHis>> page(@RequestBody(required = false) CustomerRiskHisQuery query) {
        return Result.ok(service.page(query == null ? new CustomerRiskHisQuery() : query));
    }

    @PostMapping("/detail/{profileId}")
    public Result<CustomerRiskHis> detail(@PathVariable String profileId) {
        return Result.ok(service.getById(profileId));
    }

    @PostMapping("/create")
    public Result<CustomerRiskHis> create(@RequestBody CustomerRiskHis entity) {
        return Result.ok(service.create(entity));
    }

    @PostMapping("/update/{profileId}")
    public Result<CustomerRiskHis> update(@PathVariable String profileId, @RequestBody CustomerRiskHis entity) {
        entity.setProfileId(profileId);
        return Result.ok(service.update(entity));
    }

    @PostMapping("/delete/{profileId}")
    public Result<Integer> delete(@PathVariable String profileId) {
        return Result.ok(service.delete(profileId));
    }
}
