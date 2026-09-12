package com.aicompetition.service;

import com.aicompetition.dto.IntentResult;
import com.aicompetition.entity.ai.ChatMessage;
import com.aicompetition.entity.ai.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自然语言指令解析：优先 LLM（内网可达时），否则用本地规则兜底（关键词 + 日期正则），
 * 保证离线也能解析常见话术。解析结果仅为结构化意图，不生成 SQL。
 */
@Service
public class NlService {

    private static final Logger log = LoggerFactory.getLogger(NlService.class);

    private static final String SYSTEM_PROMPT = """
            你是保险核保系统的自然语言指令解析器。把用户的中文指令解析为严格 JSON，用于后端路由，不要生成 SQL，不要解释。
            表：customer_risk_his 历史客户画像；policy_applications 投保申请(application_date/status/product_type)；underwriting_decisions 核保决策(risk_level/underwriting_result)。
            intent 取值：QUERY 查询 / PREDICT 预测 / AGGREGATE 统计 / UNKNOWN。
            输出 JSON：{"intent","entity","target":"single|batch|all|by_date","metrics":["pass_rate","risk_level_distribution","count"],"filters":{"customerName","customerId","date":"yyyy-MM-dd","timeRange":{"start":"yyyy-MM-dd","end":"yyyy-MM-dd"},"status","productType"}}。
            只输出 JSON，不要 ```。相对/某月时间换算为绝对 timeRange。人名填 filters.customerName。
            """;

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${ai.nl.use-llm:false}")
    private boolean useLlm;

    public NlService(AiService aiService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public IntentResult parse(String text) {
        if (useLlm) {
            try {
                IntentResult r = llmParse(text);
                if (r != null && r.getIntent() != null && !"UNKNOWN".equals(r.getIntent())) {
                    return r;
                }
            } catch (Exception e) {
                log.warn("LLM 解析失败，改用本地规则：{}", e.getMessage());
            }
        }
        return heuristicParse(text);
    }

    // ================= LLM 解析 =================
    private IntentResult llmParse(String text) throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        addFewShots(messages);
        messages.add(ChatMessage.user(text));
        ChatResponse resp = aiService.chat(messages, 0.0, 1024).block();
        String content = resp != null && resp.getChoices() != null && !resp.getChoices().isEmpty()
                ? resp.getChoices().get(0).getMessage().getContent() : null;
        if (content == null || content.isBlank()) return null;
        String json = extractJson(content);
        IntentResult result = objectMapper.readValue(json, IntentResult.class);
        result.setRaw(content);
        result.setNote("LLM 解析");
        return result;
    }

    private void addFewShots(List<ChatMessage> m) {
        m.add(ChatMessage.user("帮我查2025年6月的所有投保申请数据"));
        m.add(ChatMessage.assistant("{\"intent\":\"QUERY\",\"entity\":\"policy_applications\",\"target\":\"all\",\"filters\":{\"timeRange\":{\"start\":\"2025-06-01\",\"end\":\"2025-06-30\"}}}"));
        m.add(ChatMessage.user("帮我预测2025年6月25日张三的投保风险等级"));
        m.add(ChatMessage.assistant("{\"intent\":\"PREDICT\",\"entity\":\"underwriting_decisions\",\"target\":\"single\",\"filters\":{\"customerName\":\"张三\",\"date\":\"2025-06-25\"}}"));
        m.add(ChatMessage.user("请预测2025年6月所有客户的核保决定"));
        m.add(ChatMessage.assistant("{\"intent\":\"PREDICT\",\"entity\":\"underwriting_decisions\",\"target\":\"all\",\"filters\":{\"timeRange\":{\"start\":\"2025-06-01\",\"end\":\"2025-06-30\"}}}"));
        m.add(ChatMessage.user("帮我统计2025年3月到6月的核保通过率与风险分布"));
        m.add(ChatMessage.assistant("{\"intent\":\"AGGREGATE\",\"entity\":\"underwriting_decisions\",\"metrics\":[\"pass_rate\",\"risk_level_distribution\"],\"filters\":{\"timeRange\":{\"start\":\"2025-03-01\",\"end\":\"2025-06-30\"}}}"));
    }

    private String extractJson(String content) {
        String s = content.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }

    // ================= 本地规则解析（兜底） =================
    private static final Pattern DAY = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern RANGE = Pattern.compile("(\\d{4})年(\\d{1,2})月.*?[到至\\-~](\\d{1,2})月");
    private static final Pattern MONTH = Pattern.compile("(\\d{4})年(\\d{1,2})月");
    private static final Pattern CUST = Pattern.compile("[Cc]\\d{2,}");

    private IntentResult heuristicParse(String text) {
        IntentResult r = new IntentResult();
        r.setNote("本地规则解析");
        r.setRaw(text);
        Map<String, Object> filters = new HashMap<>();

        // 意图
        String intent;
        if (containsAny(text, "统计", "汇总", "通过率", "分布", "占比", "多少", "数量")) {
            intent = "AGGREGATE";
        } else if (containsAny(text, "预测", "核保决策", "核保建议", "风险等级", "评级", "定级", "核保决定")) {
            intent = "PREDICT";
        } else if (containsAny(text, "查", "列表", "查询", "看", "显示", "展示")) {
            intent = "QUERY";
        } else {
            intent = "QUERY";
        }
        r.setIntent(intent);

        // 实体
        String entity;
        if (containsAny(text, "投保申请", "申请")) entity = "policy_applications";
        else if (containsAny(text, "核保", "决策", "风险", "理赔")) entity = "underwriting_decisions";
        else if (containsAny(text, "画像", "客户", "历史")) entity = "customer_risk_his";
        else entity = intent.equals("QUERY") ? "policy_applications" : "underwriting_decisions";
        r.setEntity(entity);

        // 时间
        Matcher day = DAY.matcher(text);
        Matcher range = RANGE.matcher(text);
        Matcher month = MONTH.matcher(text);
        if (day.find()) {
            String d = String.format("%s-%02d-%02d",
                    day.group(1), Integer.parseInt(day.group(2)), Integer.parseInt(day.group(3)));
            filters.put("date", d);
        } else if (range.find()) {
            int y = Integer.parseInt(range.group(1));
            int m1 = Integer.parseInt(range.group(2));
            int m2 = Integer.parseInt(range.group(3));
            filters.put("timeRange", monthRange(y, m1, y, m2));
        } else if (month.find()) {
            int y = Integer.parseInt(month.group(1));
            int m = Integer.parseInt(month.group(2));
            filters.put("timeRange", monthRange(y, m, y, m));
        }

        // 状态 / 产品
        for (String st : new String[]{"已通过", "待核保", "核保中", "已拒保", "已撤单"}) {
            if (text.contains(st)) { filters.put("status", st); break; }
        }
        for (String p : new String[]{"寿险", "医疗险", "意外险"}) {
            if (text.contains(p)) { filters.put("productType", p); break; }
        }

        // 客户编号 / 目标范围
        Matcher c = CUST.matcher(text);
        if (c.find()) filters.put("customerId", c.group().toUpperCase());
        if (containsAny(text, "所有", "全部", "全体")) r.setTarget("all");
        else if (filters.containsKey("customerId") || filters.containsKey("date")) r.setTarget("single");

        // 统计指标
        if (intent.equals("AGGREGATE")) {
            List<String> metrics = new ArrayList<>();
            if (text.contains("通过率")) metrics.add("pass_rate");
            if (containsAny(text, "分布", "风险")) metrics.add("risk_level_distribution");
            if (metrics.isEmpty()) metrics.add("count");
            r.setMetrics(metrics);
        }

        r.setFilters(filters);
        return r;
    }

    private Map<String, String> monthRange(int y1, int m1, int y2, int m2) {
        YearMonth start = YearMonth.of(y1, m1);
        YearMonth end = YearMonth.of(y2, m2);
        Map<String, String> tr = new HashMap<>();
        tr.put("start", start.atDay(1).toString());
        tr.put("end", end.atEndOfMonth().toString());
        return tr;
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }
}
