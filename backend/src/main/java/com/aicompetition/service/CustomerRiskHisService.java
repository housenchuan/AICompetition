package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.common.PageResult;
import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.mapper.CustomerRiskHisMapper;
import com.aicompetition.query.CustomerRiskHisQuery;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerRiskHisService {

    private final CustomerRiskHisMapper mapper;

    public CustomerRiskHisService(CustomerRiskHisMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<CustomerRiskHis> page(CustomerRiskHisQuery query) {
        CustomerRiskHis q = new CustomerRiskHis();
        q.setProfileId(query.getProfileId());
        q.setCustomerId(query.getCustomerId());
        q.setGender(query.getGender());
        q.setOccupation(query.getOccupation());
        q.setSmokingStatus(query.getSmokingStatus());
        q.setDrinkingStatus(query.getDrinkingStatus());
        q.setHasSocialInsurance(query.getHasSocialInsurance());
        q.setTarget(query.getTarget());

        LocalDateTime createdFrom = DateUtils.startOfDay(query.getCreatedFrom());
        LocalDateTime createdTo = DateUtils.endOfDay(query.getCreatedTo());
        LocalDateTime updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        LocalDateTime updatedTo = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<CustomerRiskHis> list = mapper.selectList(q, createdFrom, createdTo, updatedFrom, updatedTo,
                query.getBpSysMin(), query.getBpSysMax());
        return PageResult.of(list);
    }

    public CustomerRiskHis getById(String profileId) {
        return mapper.selectById(profileId);
    }

    public CustomerRiskHis create(CustomerRiskHis entity) {
        if (entity.getProfileId() == null || entity.getProfileId().isEmpty()) {
            entity.setProfileId(UUID.randomUUID().toString().replace("-", ""));
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return entity;
    }

    public CustomerRiskHis update(CustomerRiskHis entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return mapper.selectById(entity.getProfileId());
    }

    public int delete(String profileId) {
        return mapper.deleteById(profileId);
    }
}
