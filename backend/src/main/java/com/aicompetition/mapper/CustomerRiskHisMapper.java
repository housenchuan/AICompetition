package com.aicompetition.mapper;

import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.query.CustomerRiskHisQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerRiskHisMapper {

    int insert(CustomerRiskHis entity);

    int updateById(CustomerRiskHis entity);

    int deleteById(@Param("profileId") String profileId);

    CustomerRiskHis selectById(@Param("profileId") String profileId);

    List<CustomerRiskHis> selectList(@Param("q") CustomerRiskHisQuery q,
                                     @Param("createdFrom") LocalDateTime createdFrom,
                                     @Param("createdTo") LocalDateTime createdTo,
                                     @Param("updatedFrom") LocalDateTime updatedFrom,
                                     @Param("updatedTo") LocalDateTime updatedTo);
}
