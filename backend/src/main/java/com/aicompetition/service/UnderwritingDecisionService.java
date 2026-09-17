package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.common.PageResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.aicompetition.query.UnderwritingDecisionQuery;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UnderwritingDecisionService {

    private final UnderwritingDecisionMapper mapper;
    private final AdjustmentService adjustmentService;

    public UnderwritingDecisionService(UnderwritingDecisionMapper mapper, AdjustmentService adjustmentService) {
        this.mapper = mapper;
        this.adjustmentService = adjustmentService;
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

        String createdFrom = DateUtils.startOfDay(query.getCreatedFrom());
        String createdTo   = DateUtils.endOfDay(query.getCreatedTo());
        String updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        String updatedTo   = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<UnderwritingDecision> list = mapper.selectList(q, createdFrom, createdTo, updatedFrom, updatedTo);
        PageResult<UnderwritingDecision> pr = PageResult.of(list);
        // 合并人工修整状态标签（来自文件存储，非 DB 列）
        for (UnderwritingDecision d : list) {
            d.setAdjustStatus(adjustmentService.latestStatusTag(d.getDecisionId()));
        }
        return pr;
    }

    public UnderwritingDecision getById(String decisionId) {
        UnderwritingDecision d = mapper.selectById(decisionId);
        if (d != null) d.setAdjustStatus(adjustmentService.latestStatusTag(decisionId));
        return d;
    }

    public UnderwritingDecision getByApplicationId(String applicationId) {
        return mapper.selectByApplicationId(applicationId);
    }

    public UnderwritingDecision create(UnderwritingDecision entity) {
        if (entity.getDecisionId() == null || entity.getDecisionId().isEmpty()) {
            entity.setDecisionId(UUID.randomUUID().toString().replace("-", ""));
        }
        String now = DateUtils.nowStr();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (entity.getCreatedBy() == null) entity.setCreatedBy("人工");
        mapper.insert(entity);
        return entity;
    }

    public UnderwritingDecision update(UnderwritingDecision entity) {
        entity.setUpdatedAt(DateUtils.nowStr());
        mapper.updateById(entity);
        return mapper.selectById(entity.getDecisionId());
    }

    public int delete(String decisionId) {
        return mapper.deleteById(decisionId);
    }
}
