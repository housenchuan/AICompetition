package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.common.PageResult;
import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.mapper.CustomerRiskHisMapper;
import com.aicompetition.query.CustomerRiskHisQuery;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerRiskHisService {

    private final CustomerRiskHisMapper mapper;

    public CustomerRiskHisService(CustomerRiskHisMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<CustomerRiskHis> page(CustomerRiskHisQuery query) {
        String createdFrom = DateUtils.startOfDay(query.getCreatedFrom());
        String createdTo   = DateUtils.endOfDay(query.getCreatedTo());
        String updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        String updatedTo   = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<CustomerRiskHis> list = mapper.selectList(query, createdFrom, createdTo, updatedFrom, updatedTo);
        return PageResult.of(list);
    }

    public CustomerRiskHis getById(String profileId) {
        return mapper.selectById(profileId);
    }

    public CustomerRiskHis create(CustomerRiskHis entity) {
        if (entity.getProfileId() == null || entity.getProfileId().isEmpty()) {
            entity.setProfileId(UUID.randomUUID().toString().replace("-", ""));
        }
        String now = DateUtils.nowStr();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return entity;
    }

    public CustomerRiskHis update(CustomerRiskHis entity) {
        entity.setUpdatedAt(DateUtils.nowStr());
        mapper.updateById(entity);
        return mapper.selectById(entity.getProfileId());
    }

    public int delete(String profileId) {
        return mapper.deleteById(profileId);
    }
}
