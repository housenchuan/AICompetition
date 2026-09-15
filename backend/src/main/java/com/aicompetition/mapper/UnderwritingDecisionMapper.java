package com.aicompetition.mapper;

import com.aicompetition.entity.UnderwritingDecision;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 核保决策画像分析结果表 Mapper。
 */
public interface UnderwritingDecisionMapper {

    int insert(UnderwritingDecision entity);

    int updateById(UnderwritingDecision entity);

    int deleteById(@Param("decisionId") String decisionId);

    UnderwritingDecision selectById(@Param("decisionId") String decisionId);

    /** 按投保申请编号查询（申请与核保决策关联查询）。 */
    UnderwritingDecision selectByApplicationId(@Param("applicationId") String applicationId);

    /** 条件查询。q 中的非空字段作等值过滤。 */
    List<UnderwritingDecision> selectList(@Param("q") UnderwritingDecision q,
                                         @Param("createdFrom") LocalDateTime createdFrom,
                                         @Param("createdTo") LocalDateTime createdTo,
                                         @Param("updatedFrom") LocalDateTime updatedFrom,
                                         @Param("updatedTo") LocalDateTime updatedTo);

    /** 预测后仅回写 AI 生成字段（评分/等级/结论/加费比例/关键因子）。 */
    int updatePrediction(UnderwritingDecision entity);
}
