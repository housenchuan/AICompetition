package com.aicompetition.service;

import com.aicompetition.dto.ScoreResult;
import com.aicompetition.entity.UnderwritingDecision;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创新点③：AI 置信度评分与智能路由 —— 文件存储（data/confidence.json），不改数据库表结构。
 *
 * 置信度（0-100）是对「本条 AI 自动核保结论是否可安全自动放行」的量化：
 * 从满分 100 出发，对需要人工关注的信号扣分（高风险结论 / 大幅加费 / 病史语义不确定 / 临界分 / 关键字段缺失），
 * 得分越高越可信。据此路由审核优先级：≥85 自动通过、70-84 普通审核、<70 高优人工。
 *
 * 全部为确定性计算，且每个扣分项都记录 signals，供创新点④「可解释审计轨迹」展示。
 */
@Service
public class ConfidenceService {

    private static final Logger log = LoggerFactory.getLogger(ConfidenceService.class);

    public static final String P_AUTO = "自动通过";
    public static final String P_NORMAL = "普通审核";
    public static final String P_HIGH = "高优人工";

    public static final int TH_AUTO = 85;
    public static final int TH_NORMAL = 70;

    private final ObjectMapper om = new ObjectMapper();
    private final File file = new File("data/confidence.json");
    private final RuleService ruleService;
    private ObjectNode root;

    public ConfidenceService(RuleService ruleService) {
        this.ruleService = ruleService;
        load();
    }

    private synchronized void load() {
        try {
            if (file.exists()) root = (ObjectNode) om.readTree(file);
        } catch (Exception e) {
            log.warn("读取 confidence.json 失败，重建空库：{}", e.getMessage());
        }
        if (root == null) root = om.createObjectNode();
    }

    private synchronized void save() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            om.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            log.error("写入 confidence.json 失败：{}", e.getMessage());
        }
    }

    /** 优先级路由：得分 → 审核队列。 */
    public static String priorityOf(int confidence) {
        if (confidence >= TH_AUTO) return P_AUTO;
        if (confidence >= TH_NORMAL) return P_NORMAL;
        return P_HIGH;
    }

    /**
     * 评估某条决策的置信度并持久化。纯确定性计算，返回 {confidence, priority, signals[]}。
     * 由 PredictService 在评分后调用；signals 每项含 {factor, delta}。
     */
    public synchronized ObjectNode evaluate(UnderwritingDecision d, ScoreResult r) {
        ArrayNode signals = om.createArrayNode();
        int score = 100;

        String result = safe(r.getUnderwritingResult());
        if (result.contains("拒保")) {
            score -= 25;
            signals.add(signal("结论为拒保，高风险决策建议人工复核", -25));
        } else if (result.contains("延期")) {
            score -= 20;
            signals.add(signal("结论为延期承保，需人工确认观察期", -20));
        }

        BigDecimal prem = r.getPremiumAdjustment();
        if (prem != null && !r.isRejected()) {
            double up = prem.doubleValue() - 1.0; // 加费比例
            if (up > 0.5) {
                score -= 15;
                signals.add(signal("加费幅度 > 50%，费率影响大", -15));
            } else if (up > 0.2) {
                score -= 8;
                signals.add(signal("加费幅度 > 20%", -8));
            }
        }

        Map<String, Integer> bd = r.getBreakdown() == null ? new LinkedHashMap<>() : r.getBreakdown();
        boolean hasPersonalHistory = bd.keySet().stream().anyMatch(k -> k.startsWith("个人病史"))
                || (d != null && safe(d.getPersonalMedicalHistory()).length() > 0
                    && !safe(d.getPersonalMedicalHistory()).equals("无"));
        if (hasPersonalHistory && !r.isRejected()) {
            score -= 12;
            signals.add(signal("含个人病史，文本语义判断存在不确定性", -12));
        }
        if (bd.keySet().stream().anyMatch(k -> k.startsWith("家族"))) {
            score -= 5;
            signals.add(signal("含家族病史风险因素", -5));
        }

        if (!r.isRejected() && isBorderline(r.getTotalScore())) {
            score -= 10;
            signals.add(signal("总分接近风险等级分界，判定临界", -10));
        }

        if (d != null) {
            int miss = 0;
            if (d.getBmi() == null) miss++;
            if (safe(d.getBloodPressure()).isEmpty()) miss++;
            if (d.getAge() == null) miss++;
            if (miss > 0) {
                int delta = Math.min(miss * 5, 10);
                score -= delta;
                signals.add(signal("关键字段缺失 " + miss + " 项，信息不完整", -delta));
            }
        }

        int confidence = Math.max(0, Math.min(100, score));
        String priority = priorityOf(confidence);

        ObjectNode node = om.createObjectNode();
        node.put("confidence", confidence);
        node.put("priority", priority);
        node.set("signals", signals);

        if (d != null && d.getDecisionId() != null) {
            root.set(d.getDecisionId(), node);
            save();
        }
        return node;
    }

    /** 读取已存置信度结果（供列表合并 / 审计展示），无则 null。 */
    public JsonNode get(String decisionId) {
        JsonNode n = root.get(decisionId);
        return n == null || n.isNull() ? null : n;
    }

    public Integer confidenceOf(String decisionId) {
        JsonNode n = get(decisionId);
        return n == null ? null : n.path("confidence").asInt();
    }

    public String priorityTag(String decisionId) {
        JsonNode n = get(decisionId);
        return n == null ? null : n.path("priority").asText(null);
    }

    /** 智能路由分布统计（供看板/路演数据）：三档各自条数 + 自动通过率。 */
    public synchronized ObjectNode priorityStats() {
        int auto = 0, normal = 0, high = 0;
        for (JsonNode n : root) {
            String p = n.path("priority").asText("");
            if (P_AUTO.equals(p)) auto++;
            else if (P_NORMAL.equals(p)) normal++;
            else if (P_HIGH.equals(p)) high++;
        }
        int total = auto + normal + high;
        ObjectNode out = om.createObjectNode();
        out.put("total", total);
        out.put(P_AUTO, auto);
        out.put(P_NORMAL, normal);
        out.put(P_HIGH, high);
        out.put("autoPassRate", total == 0 ? 0.0 : Math.round(auto * 1000.0 / total) / 10.0);
        return out;
    }

    /** 临界判定：总分落在任一风险等级区间边界 ±5 分内。 */
    private boolean isBorderline(int total) {
        JsonNode levels = ruleService.section("levels");
        if (levels == null) return false;
        for (JsonNode n : levels) {
            int min = n.path("minScore").asInt(-1);
            int max = n.path("maxScore").asInt(-1);
            if (min < 0) continue;
            if (Math.abs(total - min) <= 5 || Math.abs(total - max) <= 5) return true;
        }
        return false;
    }

    private ObjectNode signal(String factor, int delta) {
        ObjectNode n = om.createObjectNode();
        n.put("factor", factor);
        n.put("delta", delta);
        return n;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
