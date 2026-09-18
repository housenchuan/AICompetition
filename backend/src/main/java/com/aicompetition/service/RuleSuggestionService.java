package com.aicompetition.service;

import com.aicompetition.entity.FeedbackItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创新点⑤：反馈驱动的规则优化闭环 —— 文件存储（data/rule_suggestions.json），不改数据库表结构。
 *
 * 「误判申诉 / 规则优化」类反馈可一键沉淀为规则优化建议，经内部质检 / 管理员确认后采纳。
 * 闭环：反馈 → 规则建议（待确认）→ 人工确认（已采纳/已驳回）→ 反哺规则知识库。
 *
 * 说明：建议态为「人工确认才生效」，不自动改写线上 risk-rules.json —— 保险场景规则变更必须留人工闸口，
 * 与创新点④审计留痕一致。全自动规则自学习列入后期版本规划。
 */
@Service
public class RuleSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(RuleSuggestionService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String ST_PENDING = "待确认";
    public static final String ST_ADOPTED = "已采纳";
    public static final String ST_REJECTED = "已驳回";

    private final ObjectMapper om = new ObjectMapper();
    private final File file = new File("data/rule_suggestions.json");
    private final FeedbackService feedbackService;
    private ArrayNode list;
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public RuleSuggestionService(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
        load();
    }

    private synchronized void load() {
        try {
            if (file.exists()) list = (ArrayNode) om.readTree(file);
        } catch (Exception e) {
            log.warn("读取 rule_suggestions.json 失败，重建空库：{}", e.getMessage());
        }
        if (list == null) list = om.createArrayNode();
        int max = 0;
        for (JsonNode n : list) {
            try { max = Math.max(max, Integer.parseInt(n.path("id").asText("RS0").substring(2))); }
            catch (Exception ignore) { }
        }
        idCounter.set(max);
    }

    private synchronized void save() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            om.writerWithDefaultPrettyPrinter().writeValue(file, list);
        } catch (Exception e) {
            log.error("写入 rule_suggestions.json 失败：{}", e.getMessage());
            throw new IllegalStateException("规则建议保存失败");
        }
    }

    /** 由一条反馈（误判申诉/规则优化）生成规则优化建议。 */
    public synchronized ObjectNode fromFeedback(String feedbackId, String title, String content, String suggestedChange) {
        FeedbackItem fb = feedbackService.get(feedbackId);
        if (fb == null) throw new IllegalArgumentException("反馈不存在：" + feedbackId);

        String now = LocalDateTime.now().format(TS);
        ObjectNode rec = om.createObjectNode();
        rec.put("id", "RS" + String.format("%04d", idCounter.incrementAndGet()));
        rec.put("fromFeedbackId", feedbackId);
        rec.put("sourceType", fb.getType());
        rec.put("relatedNo", fb.getRelatedNo() == null ? "" : fb.getRelatedNo());
        rec.put("title", blankTo(title, fb.getTitle()));
        rec.put("content", blankTo(content, fb.getDescription()));
        rec.put("suggestedChange", suggestedChange == null ? "" : suggestedChange.trim());
        rec.put("status", ST_PENDING);
        rec.putNull("reviewer");
        rec.putNull("reviewedAt");
        rec.put("createdAt", now);
        rec.put("updatedAt", now);
        list.add(rec);
        save();
        return rec;
    }

    /** 确认建议：采纳/驳回（人工闸口，不自动改线上规则）。 */
    public synchronized ObjectNode review(String id, boolean adopt, String reviewer) {
        for (JsonNode n : list) {
            if (id.equals(n.path("id").asText())) {
                ObjectNode rec = (ObjectNode) n;
                rec.put("status", adopt ? ST_ADOPTED : ST_REJECTED);
                rec.put("reviewer", reviewer == null ? "" : reviewer);
                rec.put("reviewedAt", LocalDateTime.now().format(TS));
                rec.put("updatedAt", LocalDateTime.now().format(TS));
                save();
                return rec;
            }
        }
        throw new IllegalArgumentException("规则建议不存在：" + id);
    }

    /** 已生成过规则建议的反馈 id 列表（供前端隐藏「转规则建议」按钮，避免重复转）。 */
    public synchronized java.util.List<String> convertedFeedbackIds() {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (JsonNode n : list) {
            String fid = n.path("fromFeedbackId").asText(null);
            if (fid != null && !fid.isEmpty()) set.add(fid);
        }
        return new java.util.ArrayList<>(set);
    }

    /** 列表（可按状态过滤，最新在前）。 */
    public synchronized ArrayNode listAll(String status) {
        ArrayNode out = om.createArrayNode();
        for (int i = list.size() - 1; i >= 0; i--) {
            JsonNode n = list.get(i);
            if (status == null || status.isEmpty() || status.equals(n.path("status").asText())) out.add(n);
        }
        return out;
    }

    /** 统计：各状态条数 + 采纳率（供闭环成效展示）。 */
    public synchronized Map<String, Object> stats() {
        int pending = 0, adopted = 0, rejected = 0;
        for (JsonNode n : list) {
            String s = n.path("status").asText("");
            if (ST_PENDING.equals(s)) pending++;
            else if (ST_ADOPTED.equals(s)) adopted++;
            else if (ST_REJECTED.equals(s)) rejected++;
        }
        int reviewed = adopted + rejected;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", list.size());
        out.put(ST_PENDING, pending);
        out.put(ST_ADOPTED, adopted);
        out.put(ST_REJECTED, rejected);
        out.put("adoptRate", reviewed == 0 ? 0.0 : Math.round(adopted * 1000.0 / reviewed) / 10.0);
        return out;
    }

    private String blankTo(String v, String fallback) {
        if (v != null && !v.trim().isEmpty()) return v.trim();
        return fallback == null ? "" : fallback;
    }
}
