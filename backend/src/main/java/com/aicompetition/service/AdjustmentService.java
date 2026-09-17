package com.aicompetition.service;

import com.aicompetition.dto.AdjustRequest;
import com.aicompetition.dto.ReviewRequest;
import com.aicompetition.entity.UnderwritingDecision;
import com.aicompetition.mapper.UnderwritingDecisionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 人工修整 / 分级审批 / AI 预测备份 —— 文件存储（data/adjustments.json），不改数据库表结构。
 *
 * 权限：核保专员提交（进入待审批）；核保主管提交（免审直接生效）或审批（通过/驳回）；管理员只读。
 * 审批通过后覆写值写回决策表现有列（risk_score/risk_level/underwriting_result/premium_adjustment）。
 * AI 每次预测的结果快照存 aiBaseline，作为 AI 数据备份，永久保留供审核对比。
 */
@Service
public class AdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(AdjustmentService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String ROLE_UNDERWRITER = "核保专员";
    public static final String ROLE_SUPERVISOR = "核保主管";

    public static final String ST_PENDING = "待审批";
    public static final String ST_APPROVED = "已通过";
    public static final String ST_EFFECTIVE = "已生效"; // 核保主管免审直接生效
    public static final String ST_REJECTED = "已驳回";

    private final ObjectMapper om = new ObjectMapper();
    private final File file = new File("data/adjustments.json");
    private final UnderwritingDecisionMapper decisionMapper;
    private ObjectNode root;

    public AdjustmentService(UnderwritingDecisionMapper decisionMapper) {
        this.decisionMapper = decisionMapper;
        load();
    }

    private synchronized void load() {
        try {
            if (file.exists()) {
                root = (ObjectNode) om.readTree(file);
            }
        } catch (Exception e) {
            log.warn("读取 adjustments.json 失败，重建空库：{}", e.getMessage());
        }
        if (root == null) root = om.createObjectNode();
        if (!root.has("aiBaseline")) root.putObject("aiBaseline");
        if (!root.has("adjustments")) root.putObject("adjustments");
    }

    private synchronized void save() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            om.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            log.error("写入 adjustments.json 失败：{}", e.getMessage());
            throw new IllegalStateException("修整数据保存失败");
        }
    }

    private ObjectNode snapshot(UnderwritingDecision d) {
        ObjectNode n = om.createObjectNode();
        n.put("riskScore", d.getRiskScore());
        n.put("riskLevel", d.getRiskLevel());
        n.put("underwritingResult", d.getUnderwritingResult());
        n.put("premiumAdjustment", d.getPremiumAdjustment());
        n.put("keyFactors", d.getKeyFactors());
        return n;
    }

    /** AI 预测成功后备份该决策的 AI 结果（每次覆盖为最近一次），供审核对比、永久保留。 */
    public synchronized void saveAiBaseline(UnderwritingDecision d) {
        if (d == null || d.getDecisionId() == null) return;
        ((ObjectNode) root.get("aiBaseline")).set(d.getDecisionId(), snapshot(d));
        save();
    }

    /** 提交人工修整。核保专员→待审批；核保主管→免审直接生效。 */
    public synchronized ObjectNode submit(String decisionId, AdjustRequest req) {
        String role = req.getRole();
        if (!ROLE_UNDERWRITER.equals(role) && !ROLE_SUPERVISOR.equals(role)) {
            throw new IllegalArgumentException("仅核保专员或核保主管可发起人工修整");
        }
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("修整原因必填");
        }
        UnderwritingDecision d = decisionMapper.selectById(decisionId);
        if (d == null) throw new IllegalArgumentException("核保决策记录不存在：" + decisionId);

        ArrayNode list = listOf(decisionId);
        if (hasPending(list)) throw new IllegalArgumentException("该决策已有待审批的修整，请先完成审批");

        // AI 预测备份（优先取 aiBaseline，无则用当前决策值兜底）
        JsonNode ai = root.get("aiBaseline").get(decisionId);
        ObjectNode aiPrediction = ai != null ? ai.deepCopy() : snapshot(d);

        // 人工覆写值：拒保/延期 → 加费归零（系数 1.00）
        BigDecimal premium = req.getPremiumAdjustment();
        if (isZeroPremiumResult(req.getUnderwritingResult())) premium = BigDecimal.ONE;
        ObjectNode humanValue = om.createObjectNode();
        humanValue.put("riskScore", req.getRiskScore());
        humanValue.put("riskLevel", req.getRiskLevel());
        humanValue.put("underwritingResult", req.getUnderwritingResult());
        humanValue.put("premiumAdjustment", premium);

        boolean supervisor = ROLE_SUPERVISOR.equals(role);
        String now = LocalDateTime.now().format(TS);

        ObjectNode rec = om.createObjectNode();
        rec.put("adjustId", UUID.randomUUID().toString().replace("-", ""));
        rec.put("decisionId", decisionId);
        rec.set("aiPrediction", aiPrediction);
        rec.set("humanValue", humanValue);
        rec.put("reason", req.getReason().trim());
        rec.put("submitRole", role);
        rec.put("submitAt", now);
        rec.put("level", 2);
        rec.put("approverRole", ROLE_SUPERVISOR);
        rec.put("status", supervisor ? ST_EFFECTIVE : ST_PENDING);
        rec.putNull("reviewRole");
        rec.putNull("reviewAt");
        rec.putNull("reviewComment");
        list.add(rec);

        // 核保主管提交：免审核，立即覆写生效
        if (supervisor) applyOverride(decisionId, humanValue);

        save();
        return rec;
    }

    /** 审批：仅核保主管。通过→覆写生效；驳回→仅留痕。 */
    public synchronized ObjectNode review(String decisionId, ReviewRequest req) {
        if (!ROLE_SUPERVISOR.equals(req.getRole())) {
            throw new IllegalArgumentException("仅核保主管可审批");
        }
        ArrayNode list = listOf(decisionId);
        ObjectNode pending = null;
        for (JsonNode n : list) {
            if (ST_PENDING.equals(n.path("status").asText())) pending = (ObjectNode) n;
        }
        if (pending == null) throw new IllegalArgumentException("没有待审批的修整");

        boolean pass = Boolean.TRUE.equals(req.getPass());
        pending.put("status", pass ? ST_APPROVED : ST_REJECTED);
        pending.put("reviewRole", req.getRole());
        pending.put("reviewAt", LocalDateTime.now().format(TS));
        pending.put("reviewComment", req.getComment() == null ? "" : req.getComment().trim());

        if (pass) applyOverride(decisionId, (ObjectNode) pending.get("humanValue"));

        save();
        return pending;
    }

    /** 覆写值写回决策表现有列（只更新4字段+updated_at，保留 key_factors）。 */
    private void applyOverride(String decisionId, ObjectNode human) {
        UnderwritingDecision u = new UnderwritingDecision();
        u.setDecisionId(decisionId);
        if (human.hasNonNull("riskScore")) u.setRiskScore(human.get("riskScore").asInt());
        if (human.hasNonNull("riskLevel")) u.setRiskLevel(human.get("riskLevel").asText());
        if (human.hasNonNull("underwritingResult")) u.setUnderwritingResult(human.get("underwritingResult").asText());
        if (human.hasNonNull("premiumAdjustment")) u.setPremiumAdjustment(human.get("premiumAdjustment").decimalValue());
        u.setUpdatedAt(LocalDateTime.now().format(TS));
        decisionMapper.updateById(u);
    }

    /** 列表标签用：待审批 / 已生效 / 已驳回 / null（取最新一条记录）。 */
    public String latestStatusTag(String decisionId) {
        JsonNode list = root.path("adjustments").path(decisionId);
        if (!list.isArray() || list.isEmpty()) return null;
        String s = list.get(list.size() - 1).path("status").asText();
        if (ST_PENDING.equals(s)) return ST_PENDING;
        if (ST_APPROVED.equals(s) || ST_EFFECTIVE.equals(s)) return ST_EFFECTIVE;
        if (ST_REJECTED.equals(s)) return ST_REJECTED;
        return null;
    }

    /** 审计留痕 + AI 备份（供详情弹窗）。 */
    public ObjectNode getAudit(String decisionId) {
        ObjectNode out = om.createObjectNode();
        out.set("aiBaseline", root.path("aiBaseline").path(decisionId).isMissingNode()
                ? null : root.get("aiBaseline").get(decisionId));
        out.set("records", listOf(decisionId));
        return out;
    }

    private ArrayNode listOf(String decisionId) {
        ObjectNode adj = (ObjectNode) root.get("adjustments");
        if (!adj.has(decisionId) || !adj.get(decisionId).isArray()) adj.putArray(decisionId);
        return (ArrayNode) adj.get(decisionId);
    }

    private boolean hasPending(ArrayNode list) {
        for (JsonNode n : list) if (ST_PENDING.equals(n.path("status").asText())) return true;
        return false;
    }

    private boolean isZeroPremiumResult(String result) {
        if (result == null) return false;
        return result.contains("拒保") || result.contains("延期");
    }
}
