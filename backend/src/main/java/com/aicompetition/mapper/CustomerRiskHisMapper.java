package com.aicompetition.mapper;

import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.query.CustomerRiskHisQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 历史客户风险画像分析表 Mapper。
 * 列表查询配合 PageHelper.startPage(...) 实现分页；创建/更新时间支持范围筛选。
 */
public interface CustomerRiskHisMapper {

    int insert(CustomerRiskHis entity);

    int updateById(CustomerRiskHis entity);

    int deleteById(@Param("profileId") String profileId);

    CustomerRiskHis selectById(@Param("profileId") String profileId);

    /**
     * 条件查询。q 中的非空字段作等值过滤；时间参数作范围过滤（可为空）。
     */
    List<CustomerRiskHis> selectList(@Param("q") CustomerRiskHisQuery q,
                                     @Param("createdFrom") String createdFrom,
                                     @Param("createdTo") String createdTo,
                                     @Param("updatedFrom") String updatedFrom,
                                     @Param("updatedTo") String updatedTo);
}
