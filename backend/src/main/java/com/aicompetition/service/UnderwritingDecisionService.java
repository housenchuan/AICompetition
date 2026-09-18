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
    private final AdjustmentService adjustmentService;
    private final ConfidenceService confidenceService;

    public UnderwritingDecisionService(UnderwritingDecisionMapper mapper, AdjustmentService adjustmentService,
                                       ConfidenceService confidenceService) {
        this.mapper = mapper;
        this.adjustmentService = adjustmentService;
        this.confidenceService = confidenceService;
    }

    /** 合并文件存储的透传标签：人工修整状态 + AI 置信度/审核优先级。 */
    private void mergeTags(UnderwritingDecision d) {
        if (d == null || d.getDecisionId() == null) return;
        d.setAdjustStatus(adjustmentService.latestStatusTag(d.getDecisionId()));
        d.setConfidence(confidenceService.confidenceOf(d.getDecisionId()));
        d.setReviewPriority(confidenceService.priorityTag(d.getDecisionId()));
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
        LocalDateTime createdTo   = DateUtils.endOfDay(query.getCreatedTo());
        LocalDateTime updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        LocalDateTime updatedTo   = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<UnderwritingDecision> list = mapper.selectList(q, createdFrom, createdTo, updatedFrom, updatedTo);
        PageResult<UnderwritingDecision> pr = PageResult.of(list);
        // 合并文件存储标签（人工修整状态 + AI 置信度/审核优先级），非 DB 列
        for (UnderwritingDecision d : list) {
            mergeTags(d);
        }
        return pr;
    }

    public UnderwritingDecision getById(String decisionId) {
        UnderwritingDecision d = mapper.selectById(decisionId);
        mergeTags(d);
        return d;
    }

    public UnderwritingDecision getByApplicationId(String applicationId) {
        return mapper.selectByApplicationId(applicationId);
    }

    public UnderwritingDecision create(UnderwritingDecision entity) {
        if (entity.getDecisionId() == null || entity.getDecisionId().isEmpty()) {
            entity.setDecisionId(UUID.randomUUID().toString().replace("-", ""));
        }
        LocalDateTime now = DateUtils.now();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (entity.getCreatedBy() == null) entity.setCreatedBy("人工");
        mapper.insert(entity);
        return entity;
    }

    public UnderwritingDecision update(UnderwritingDecision entity) {
        UnderwritingDecision existing = mapper.selectById(entity.getDecisionId());
        if (existing == null || !"人工".equals(existing.getCreatedBy())) {
            throw new IllegalArgumentException("系统生成的记录不允许手动编辑");
        }
        entity.setUpdatedAt(DateUtils.now());
        mapper.updateById(entity);
        return mapper.selectById(entity.getDecisionId());
    }

    public int delete(String decisionId) {
        return mapper.deleteById(decisionId);
    }
}
