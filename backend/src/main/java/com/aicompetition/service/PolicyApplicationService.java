package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.common.PageResult;
import com.aicompetition.dto.ApplicationDecisionVO;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.PolicyApplicationMapper;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.aicompetition.query.PolicyApplicationQuery;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PolicyApplicationService {

    private final PolicyApplicationMapper mapper;
    private final UnderwritingDecisionMapper decisionMapper;

    public PolicyApplicationService(PolicyApplicationMapper mapper, UnderwritingDecisionMapper decisionMapper) {
        this.mapper = mapper;
        this.decisionMapper = decisionMapper;
    }

    public PageResult<PolicyApplication> page(PolicyApplicationQuery query) {
        PolicyApplication q = new PolicyApplication();
        q.setProfileId(query.getProfileId());
        q.setCustomerId(query.getCustomerId());
        q.setProductType(query.getProductType());
        q.setProductName(query.getProductName());
        q.setPaymentFrequency(query.getPaymentFrequency());
        q.setStatus(query.getStatus());
        q.setCreatedBy(query.getCreatedBy());

        // 日期范围参数直接用 String（VARCHAR 列做字符串比较，ISO 格式可正确排序）
        String dateFrom    = query.getDateFrom();
        String dateTo      = query.getDateTo();
        String createdFrom = DateUtils.startOfDay(query.getCreatedFrom());
        String createdTo   = DateUtils.endOfDay(query.getCreatedTo());
        String updatedFrom = DateUtils.startOfDay(query.getUpdatedFrom());
        String updatedTo   = DateUtils.endOfDay(query.getUpdatedTo());

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<PolicyApplication> list = mapper.selectList(q, dateFrom, dateTo,
                createdFrom, createdTo, updatedFrom, updatedTo);
        return PageResult.of(list);
    }

    public PolicyApplication getById(String profileId) {
        return mapper.selectById(profileId);
    }

    /** 投保申请 + 核保决策结果 关联分页查询。 */
    public PageResult<ApplicationDecisionVO> pageWithDecision(PolicyApplicationQuery query) {
        String dateFrom = query.getDateFrom();
        String dateTo   = query.getDateTo();
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<ApplicationDecisionVO> list = mapper.selectListWithDecision(query, dateFrom, dateTo);
        return PageResult.of(list);
    }

    /** 投保申请与核保决策结果关联查询。 */
    public Map<String, Object> getWithDecision(String profileId) {
        PolicyApplication application = mapper.selectById(profileId);
        UnderwritingDecision decision = decisionMapper.selectByApplicationId(profileId);
        Map<String, Object> result = new HashMap<>();
        result.put("application", application);
        result.put("decision", decision);
        return result;
    }

    public PolicyApplication create(PolicyApplication entity) {
        if (entity.getProfileId() == null || entity.getProfileId().isEmpty()) {
            entity.setProfileId(UUID.randomUUID().toString().replace("-", ""));
        }
        String now = DateUtils.nowStr();
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (entity.getCreatedBy() == null) entity.setCreatedBy("人工");
        if (entity.getStatus() == null) entity.setStatus("待核保");
        mapper.insert(entity);
        return entity;
    }

    public PolicyApplication update(PolicyApplication entity) {
        PolicyApplication existing = mapper.selectById(entity.getProfileId());
        if (existing == null || !"人工".equals(existing.getCreatedBy())) {
            throw new IllegalArgumentException("系统生成的记录不允许手动编辑");
        }
        entity.setUpdatedAt(DateUtils.nowStr());
        mapper.updateById(entity);
        return mapper.selectById(entity.getProfileId());
    }

    public int delete(String profileId) {
        return mapper.deleteById(profileId);
    }
}
