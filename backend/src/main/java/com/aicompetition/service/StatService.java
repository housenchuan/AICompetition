package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.PolicyApplicationMapper;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 数据汇总统计：按时间/产品/状态/风险等级维度聚合（数据量小，服务端内存聚合）。
 */
@Service
public class StatService {

    private final PolicyApplicationMapper applicationMapper;
    private final UnderwritingDecisionMapper decisionMapper;

    public StatService(PolicyApplicationMapper applicationMapper, UnderwritingDecisionMapper decisionMapper) {
        this.applicationMapper = applicationMapper;
        this.decisionMapper = decisionMapper;
    }

    public Map<String, Object> overview(String dateFrom, String dateTo) {
        LocalDate from = DateUtils.parseDate(dateFrom);
        LocalDate to = DateUtils.parseDate(dateTo);

        List<PolicyApplication> apps = applicationMapper.selectList(new PolicyApplication(), from, to);
        List<UnderwritingDecision> decs = decisionMapper.selectList(new UnderwritingDecision());

        int appTotal = apps.size();
        long passed = apps.stream().filter(a -> "已通过".equals(a.getStatus())).count();
        long rejected = apps.stream().filter(a -> "已拒保".equals(a.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationTotal", appTotal);
        result.put("decisionTotal", decs.size());
        result.put("passedCount", passed);
        result.put("rejectedCount", rejected);
        result.put("passRate", appTotal == 0 ? 0 : Math.round(passed * 1000.0 / appTotal) / 10.0);

        // 状态分布
        result.put("statusDist", toList(countBy(apps, PolicyApplication::getStatus)));
        // 产品类型分布
        result.put("productDist", toList(countBy(apps, PolicyApplication::getProductType)));
        // 风险等级分布（仅已预测）
        long predicted = decs.stream().filter(d -> d.getRiskLevel() != null && !d.getRiskLevel().isEmpty()).count();
        result.put("predictedCount", predicted);
        result.put("riskLevelDist", toList(countBy(decs, UnderwritingDecision::getRiskLevel)));

        // 平均风险评分（已预测）
        result.put("avgRiskScore", decs.stream()
                .filter(d -> d.getRiskScore() != null)
                .mapToInt(UnderwritingDecision::getRiskScore)
                .average().stream().map(v -> Math.round(v * 10) / 10.0).findFirst().orElse(0));

        return result;
    }

    private <T> Map<String, Long> countBy(List<T> list, java.util.function.Function<T, String> key) {
        Map<String, Long> m = new TreeMap<>();
        for (T t : list) {
            String k = key.apply(t);
            if (k == null || k.isEmpty()) continue;
            m.merge(k, 1L, Long::sum);
        }
        return m;
    }

    private List<Map<String, Object>> toList(Map<String, Long> counts) {
        List<Map<String, Object>> list = new ArrayList<>();
        counts.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", k);
            item.put("value", v);
            list.add(item);
        });
        return list;
    }
}
