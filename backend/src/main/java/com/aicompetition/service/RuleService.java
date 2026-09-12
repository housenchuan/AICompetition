package com.aicompetition.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * 风险计分规则知识库：从 rules/risk-rules.json 加载，作为规则展示与规则引擎的单一事实来源。
 */
@Service
public class RuleService {

    private final ObjectMapper objectMapper;
    private JsonNode rules;

    public RuleService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() throws Exception {
        try (InputStream in = new ClassPathResource("rules/risk-rules.json").getInputStream()) {
            this.rules = objectMapper.readTree(in);
        }
    }

    /** 完整规则（供前端规则展示页）。 */
    public JsonNode getRules() {
        return rules;
    }

    /** 取某一维度规则数组。 */
    public JsonNode section(String name) {
        return rules.get(name);
    }
}
