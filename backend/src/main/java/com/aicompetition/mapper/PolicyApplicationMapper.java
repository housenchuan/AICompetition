package com.aicompetition.mapper;

import com.aicompetition.dto.ApplicationDecisionVO;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.query.PolicyApplicationQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 投保申请记录表 Mapper。
 * 日期参数均为 String（对应 VARCHAR 列），格式：
 *   dateFrom/dateTo        "yyyy-MM-dd"
 *   createdFrom/createdTo/updatedFrom/updatedTo  "yyyy-MM-dd HH:mm:ss"
 */
public interface PolicyApplicationMapper {

    int insert(PolicyApplication entity);

    int updateById(PolicyApplication entity);

    int deleteById(@Param("profileId") String profileId);

    PolicyApplication selectById(@Param("profileId") String profileId);

    /**
     * 条件查询。q 中的非空字段作等值过滤；申请日期/创建时间/更新时间支持范围过滤。
     */
    List<PolicyApplication> selectList(@Param("q") PolicyApplication q,
                                       @Param("dateFrom") String dateFrom,
                                       @Param("dateTo") String dateTo,
                                       @Param("createdFrom") String createdFrom,
                                       @Param("createdTo") String createdTo,
                                       @Param("updatedFrom") String updatedFrom,
                                       @Param("updatedTo") String updatedTo);

    /**
     * 投保申请与核保决策结果关联分页查询（LEFT JOIN，决策字段可能为空）。
     */
    List<ApplicationDecisionVO> selectListWithDecision(@Param("q") PolicyApplicationQuery q,
                                                       @Param("dateFrom") String dateFrom,
                                                       @Param("dateTo") String dateTo);

    /**
     * 统计某投保人在 [dateFrom, dateTo] 内、状态为 status 的其它申请数（排除 excludeProfileId）。
     * 用于「近N个月内曾有拒保记录 → 直接拒保」判定。
     */
    int countRecentRejections(@Param("customerId") String customerId,
                              @Param("status") String status,
                              @Param("dateFrom") String dateFrom,
                              @Param("dateTo") String dateTo,
                              @Param("excludeProfileId") String excludeProfileId);
}
