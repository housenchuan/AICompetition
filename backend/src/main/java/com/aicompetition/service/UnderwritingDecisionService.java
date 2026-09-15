package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.common.PageResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.aicompetition.query.UnderwritingDecisionQuery;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UnderwritingDecisionService {

    private final UnderwritingDecisionMapper mapper;

    public UnderwritingDecisionService(UnderwritingDecisionMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<UnderwritingDecision> page(UnderwritingDecisionQuery query) {
        UnderwritingDecision q = new UnderwritingDecision();
        q.setApplicationId(query.getApplicationId());
        q.setCustomerId(query.getCustomerId());
        q.setGender(query.getGender());
        q.setOccupation(query.getOccupation());
        q.setRiskLevel(query.getRiskLevel());
        q.setUnderwritingResult(query.getUnderwritingResult());
        q.setDecisionId(query.getDecisionId());
        q.setHasSocialInsurance(query.getHasSocialInsurance());
        q.setSmokingStatus(query.getSmokingStatus());
        q.setDrinkingStatus(query.getDrinkingStatus());
        q.setCreatedBy(query.getCreatedBy());

        LocalDateTime createdFrom = DateUtils.startOfDay(query.getCreatedFrom());
        LocalDateTime createdTo = DateUtils.endOfDay(query.getCreatedTo());
        LocalDateTime updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        LocalDateTime updatedTo = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<UnderwritingDecision> list = mapper.selectList(q, createdFrom, createdTo, updatedFrom, updatedTo);
        return PageResult.of(list);
    }

    public UnderwritingDecision getById(String decisionId) {
        return mapper.selectById(decisionId);
    }

    public UnderwritingDecision getByApplicationId(String applicationId) {
        return mapper.selectByApplicationId(applicationId);
    }

    public UnderwritingDecision create(UnderwritingDecision entity) {
        if (entity.getDecisionId() == null || entity.getDecisionId().isEmpty()) {
            entity.setDecisionId(UUID.randomUUID().toString().replace("-", ""));
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (entity.getCreatedBy() == null) entity.setCreatedBy("人工");
        mapper.insert(entity);
        return entity;
    }

    public UnderwritingDecision update(UnderwritingDecision entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return mapper.selectById(entity.getDecisionId());
    }

    public int delete(String decisionId) {
        return mapper.deleteById(decisionId);
    }
}
