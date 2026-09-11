package com.aicompetition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.aicoder")
public class AiCoderProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Double defaultTemperature;
    private Integer defaultMaxTokens;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getDefaultTemperature() { return defaultTemperature; }
    public void setDefaultTemperature(Double defaultTemperature) { this.defaultTemperature = defaultTemperature; }
    public Integer getDefaultMaxTokens() { return defaultMaxTokens; }
    public void setDefaultMaxTokens(Integer defaultMaxTokens) { this.defaultMaxTokens = defaultMaxTokens; }
}
