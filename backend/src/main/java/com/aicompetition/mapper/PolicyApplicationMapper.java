package com.aicompetition.mapper;

import com.aicompetition.entity.PolicyApplication;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 投保申请记录表 Mapper。
 */
public interface PolicyApplicationMapper {

    int insert(PolicyApplication entity);

    int updateById(PolicyApplication entity);

    int deleteById(@Param("applicationId") String applicationId);

    PolicyApplication selectById(@Param("applicationId") String applicationId);

    /**
     * 条件查询。q 中的非空字段作等值过滤；申请日期支持范围过滤（可为空）。
     */
    List<PolicyApplication> selectList(@Param("q") PolicyApplication q,
                                       @Param("dateFrom") LocalDate dateFrom,
                                       @Param("dateTo") LocalDate dateTo);
}
