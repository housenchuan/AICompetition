package com.aicompetition.service;

import com.aicompetition.common.DateUtils;
import com.aicompetition.entity.CustomerRiskHis;
import com.aicompetition.entity.PolicyApplication;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.CustomerRiskHisMapper;
import com.aicompetition.mapper.PolicyApplicationMapper;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.aicompetition.query.AggregateQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * 数据汇总统计：按时间/产品/状态/风险等级维度聚合（数据量小，服务端内存聚合）。
 */
@Service
public class StatService {

    private final PolicyApplicationMapper applicationMapper;
    private final UnderwritingDecisionMapper decisionMapper;
    private final CustomerRiskHisMapper customerRiskHisMapper;

    public StatService(PolicyApplicationMapper applicationMapper, UnderwritingDecisionMapper decisionMapper,
                       CustomerRiskHisMapper customerRiskHisMapper) {
        this.applicationMapper = applicationMapper;
        this.decisionMapper = decisionMapper;
        this.customerRiskHisMapper = customerRiskHisMapper;
    }

    public Map<String, Object> overview(String dateFrom, String dateTo) {
        List<PolicyApplication> apps = applicationMapper.selectList(new PolicyApplication(), dateFrom, dateTo,
                null, null, null, null);
        List<UnderwritingDecision> decs = decisionMapper.selectList(new UnderwritingDecision(), null, null, null, null);

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

    // ===================== 通用维度聚合（白名单） =====================

    /**
     * 按 entity + groupBy（1~2 维，白名单）聚合计数。单维返回分布列表，双维返回透视表。
     */
    public Map<String, Object> aggregate(AggregateQuery q) {
        String entity = q.getEntity();
        List<String> dims = q.getGroupBy();
        if (dims == null || dims.isEmpty()) throw new IllegalArgumentException("groupBy 不能为空");
        if (dims.size() > 4) throw new IllegalArgumentException("最多支持四个分组维度");

        List<Object> rows = new ArrayList<>();
        Map<String, Function<Object, String>> extractors = new LinkedHashMap<>();

        if ("policy_applications".equals(entity)) {
            // 统一按创建时间(created_at)过滤；日期参数为 String，VARCHAR 列字符串比较
            String from = DateUtils.startOfDay(q.getDateFrom());
            String to   = DateUtils.endOfDay(q.getDateTo());
            for (PolicyApplication a : applicationMapper.selectList(new PolicyApplication(), null, null, from, to, null, null)) rows.add(a);
            extractors.put("productType", o -> ((PolicyApplication) o).getProductType());
            extractors.put("status", o -> ((PolicyApplication) o).getStatus());
            extractors.put("paymentFrequency", o -> ((PolicyApplication) o).getPaymentFrequency());
            extractors.put("createdBy", o -> ((PolicyApplication) o).getCreatedBy());
            extractors.put("customerId", o -> ((PolicyApplication) o).getCustomerId());
            // createdAt 为 "yyyy-MM-dd HH:mm:ss"，取前7位得 "yyyy-MM"
            extractors.put("month", o -> { String d = ((PolicyApplication) o).getCreatedAt(); return d != null && d.length() >= 7 ? d.substring(0, 7) : null; });
        } else if ("customer_risk_his".equals(entity)) {
            String from = DateUtils.startOfDay(q.getDateFrom());
            String to   = DateUtils.endOfDay(q.getDateTo());
            for (CustomerRiskHis c : customerRiskHisMapper.selectList(new com.aicompetition.query.CustomerRiskHisQuery(), from, to, null, null)) rows.add(c);
            extractors.put("gender", o -> ((CustomerRiskHis) o).getGender());
            extractors.put("occupation", o -> ((CustomerRiskHis) o).getOccupation());
            extractors.put("smokingStatus", o -> ((CustomerRiskHis) o).getSmokingStatus());
            extractors.put("drinkingStatus", o -> ((CustomerRiskHis) o).getDrinkingStatus());
            extractors.put("hasSocialInsurance", o -> boolLabel(((CustomerRiskHis) o).getHasSocialInsurance()));
            extractors.put("target", o -> claimLabel(((CustomerRiskHis) o).getTarget()));
            extractors.put("customerId", o -> ((CustomerRiskHis) o).getCustomerId());
            extractors.put("month", o -> { String d = ((CustomerRiskHis) o).getCreatedAt(); return d != null && d.length() >= 7 ? d.substring(0, 7) : null; });
        } else if ("underwriting_decisions".equals(entity)) {
            String from = DateUtils.startOfDay(q.getDateFrom());
            String to   = DateUtils.endOfDay(q.getDateTo());
            for (UnderwritingDecision d : decisionMapper.selectList(new UnderwritingDecision(), from, to, null, null)) rows.add(d);
            extractors.put("riskLevel", o -> ((UnderwritingDecision) o).getRiskLevel());
            extractors.put("underwritingResult", o -> ((UnderwritingDecision) o).getUnderwritingResult());
            extractors.put("gender", o -> ((UnderwritingDecision) o).getGender());
            extractors.put("occupation", o -> ((UnderwritingDecision) o).getOccupation());
            extractors.put("smokingStatus", o -> ((UnderwritingDecision) o).getSmokingStatus());
            extractors.put("drinkingStatus", o -> ((UnderwritingDecision) o).getDrinkingStatus());
            extractors.put("hasSocialInsurance", o -> boolLabel(((UnderwritingDecision) o).getHasSocialInsurance()));
            extractors.put("customerId", o -> ((UnderwritingDecision) o).getCustomerId());
            extractors.put("month", o -> { String d = ((UnderwritingDecision) o).getCreatedAt(); return d != null && d.length() >= 7 ? d.substring(0, 7) : null; });
        } else {
            throw new IllegalArgumentException("不支持的数据源: " + entity);
        }

        // 解析并校验所有维度（白名单）
        List<Function<Object, String>> fns = new ArrayList<>();
        for (String d : dims) fns.add(requireDim(extractors, d));
        Function<Object, String> f1 = fns.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entity", entity);
        result.put("dims", dims);

        if (dims.size() == 1) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Object row : rows) {
                String k = f1.apply(row);
                if (k == null || k.isEmpty()) continue;
                counts.merge(k, 1L, Long::sum);
            }
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            List<Map<String, Object>> list = new ArrayList<>();
            // 时间维度(创建年月)按时间正序，其余维度按数量倒序
            boolean byMonth = "month".equals(dims.get(0));
            java.util.Comparator<Map.Entry<String, Long>> cmp = byMonth
                    ? Map.Entry.comparingByKey()
                    : (a, b) -> Long.compare(b.getValue(), a.getValue());
            counts.entrySet().stream()
                    .sorted(cmp)
                    .forEach(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("key", e.getKey());
                        item.put("count", e.getValue());
                        item.put("percent", total == 0 ? 0 : Math.round(e.getValue() * 1000.0 / total) / 10.0);
                        list.add(item);
                    });
            result.put("rows", list);
            result.put("total", total);
        } else if (dims.size() == 2) {
            Function<Object, String> f2 = fns.get(1);
            Map<String, Map<String, Long>> pivot = new java.util.TreeMap<>();
            Map<String, Long> colTotals = new java.util.TreeMap<>();
            long total = 0;
            for (Object row : rows) {
                String r = f1.apply(row), c = f2.apply(row);
                if (r == null || r.isEmpty() || c == null || c.isEmpty()) continue;
                pivot.computeIfAbsent(r, k -> new java.util.TreeMap<>()).merge(c, 1L, Long::sum);
                colTotals.merge(c, 1L, Long::sum);
                total++;
            }
            List<String> rowKeys = new ArrayList<>(pivot.keySet());
            List<String> colKeys = new ArrayList<>(colTotals.keySet());
            List<List<Long>> matrix = new ArrayList<>();
            List<Long> rowTotals = new ArrayList<>();
            for (String r : rowKeys) {
                List<Long> line = new ArrayList<>();
                long rt = 0;
                for (String c : colKeys) { long v = pivot.get(r).getOrDefault(c, 0L); line.add(v); rt += v; }
                matrix.add(line);
                rowTotals.add(rt);
            }
            Map<String, Object> pv = new LinkedHashMap<>();
            pv.put("rowKeys", rowKeys);
            pv.put("colKeys", colKeys);
            pv.put("matrix", matrix);
            pv.put("rowTotals", rowTotals);
            pv.put("colTotals", colKeys.stream().map(colTotals::get).toList());
            pv.put("total", total);
            result.put("pivot", pv);
        } else {
            // 3~4 维：扁平分组表，每行 = 维度组合 + 数量 + 占比
            Map<List<String>, Long> combo = new LinkedHashMap<>();
            for (Object row : rows) {
                List<String> key = new ArrayList<>(fns.size());
                boolean skip = false;
                for (Function<Object, String> fn : fns) {
                    String v = fn.apply(row);
                    if (v == null || v.isEmpty()) { skip = true; break; }
                    key.add(v);
                }
                if (skip) continue;
                combo.merge(key, 1L, Long::sum);
            }
            long total = combo.values().stream().mapToLong(Long::longValue).sum();
            List<Map<String, Object>> groups = new ArrayList<>();
            combo.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .forEach(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("keys", e.getKey());
                        item.put("count", e.getValue());
                        item.put("percent", total == 0 ? 0 : Math.round(e.getValue() * 1000.0 / total) / 10.0);
                        groups.add(item);
                    });
            result.put("groups", groups);
            result.put("total", total);
        }
        return result;
    }

    private Function<Object, String> requireDim(Map<String, Function<Object, String>> extractors, String dim) {
        Function<Object, String> f = extractors.get(dim);
        if (f == null) throw new IllegalArgumentException("不支持的分组维度: " + dim);
        return f;
    }

    private String boolLabel(Boolean b) { return b == null ? null : (b ? "是" : "否"); }

    private String claimLabel(Integer t) { return t == null ? null : (t == 1 ? "有理赔" : "无理赔"); }

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
