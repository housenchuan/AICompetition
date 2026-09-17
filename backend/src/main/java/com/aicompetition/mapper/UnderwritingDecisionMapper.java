package com.aicompetition.mapper;

import com.aicompetition.entity.UnderwritingDecision;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UnderwritingDecisionMapper {

    int insert(UnderwritingDecision entity);

    int updateById(UnderwritingDecision entity);

    int deleteById(@Param("decisionId") String decisionId);

    UnderwritingDecision selectById(@Param("decisionId") String decisionId);

    UnderwritingDecision selectByApplicationId(@Param("applicationId") String applicationId);

    List<UnderwritingDecision> selectList(@Param("q") UnderwritingDecision q,
                                         @Param("createdFrom") LocalDateTime createdFrom,
                                         @Param("createdTo") LocalDateTime createdTo,
                                         @Param("updatedFrom") LocalDateTime updatedFrom,
                                         @Param("updatedTo") LocalDateTime updatedTo);

    int updatePrediction(UnderwritingDecision entity);
}
