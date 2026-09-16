package com.aicompetition.mapper;

import com.aicompetition.dto.ApplicationDecisionVO;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.query.PolicyApplicationQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投保申请记录表 Mapper。
 */
public interface PolicyApplicationMapper {

    int insert(PolicyApplication entity);

    int updateById(PolicyApplication entity);

    int deleteById(@Param("profileId") String profileId);

    PolicyApplication selectById(@Param("profileId") String profileId);

    /**
     * 条件查询。q 中的非空字段作等值过滤；申请日期支持范围过滤（可为空）。
     */
    List<PolicyApplication> selectList(@Param("q") PolicyApplication q,
                                       @Param("dateFrom") LocalDate dateFrom,
                                       @Param("dateTo") LocalDate dateTo,
                                       @Param("createdFrom") LocalDateTime createdFrom,
                                       @Param("createdTo") LocalDateTime createdTo,
                                       @Param("updatedFrom") LocalDateTime updatedFrom,
                                       @Param("updatedTo") LocalDateTime updatedTo);

    /**
     * 投保申请与核保决策结果关联分页查询（LEFT JOIN，决策字段可能为空）。
     * 支持申请侧筛选 + 决策风险等级筛选 + 申请日期范围。
     */
    List<ApplicationDecisionVO> selectListWithDecision(@Param("q") PolicyApplicationQuery q,
                                                       @Param("dateFrom") LocalDate dateFrom,
                                                       @Param("dateTo") LocalDate dateTo);

    /**
     * 统计某投保人在 [dateFrom, dateTo) 内、状态为 status 的其它申请数（排除 excludeProfileId 当前申请）。
     * 用于「近N个月内曾有拒保记录 → 直接拒保」判定。
     */
    int countRecentRejections(@Param("customerId") String customerId,
                              @Param("status") String status,
                              @Param("dateFrom") LocalDate dateFrom,
                              @Param("dateTo") LocalDate dateTo,
                              @Param("excludeProfileId") String excludeProfileId);
}
