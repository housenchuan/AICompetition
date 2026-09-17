package com.aicompetition.mapper;

import com.aicompetition.dto.ApplicationDecisionVO;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.query.PolicyApplicationQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PolicyApplicationMapper {

    int insert(PolicyApplication entity);

    int updateById(PolicyApplication entity);

    int deleteById(@Param("profileId") String profileId);

    PolicyApplication selectById(@Param("profileId") String profileId);

    List<PolicyApplication> selectList(@Param("q") PolicyApplication q,
                                       @Param("dateFrom") LocalDate dateFrom,
                                       @Param("dateTo") LocalDate dateTo,
                                       @Param("createdFrom") LocalDateTime createdFrom,
                                       @Param("createdTo") LocalDateTime createdTo,
                                       @Param("updatedFrom") LocalDateTime updatedFrom,
                                       @Param("updatedTo") LocalDateTime updatedTo);

    List<ApplicationDecisionVO> selectListWithDecision(@Param("q") PolicyApplicationQuery q,
                                                       @Param("dateFrom") LocalDate dateFrom,
                                                       @Param("dateTo") LocalDate dateTo);

    int countRecentRejections(@Param("customerId") String customerId,
                              @Param("status") String status,
                              @Param("dateFrom") LocalDate dateFrom,
                              @Param("dateTo") LocalDate dateTo,
                              @Param("excludeProfileId") String excludeProfileId);
}
