package com.aicompetition.entity.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    private Boolean stream;

    public ChatRequest() {}

    private ChatRequest(Builder b) {
        this.model = b.model;
        this.messages = b.messages;
        this.temperature = b.temperature;
        this.topP = b.topP;
        this.maxTokens = b.maxTokens;
        this.stream = b.stream;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private Boolean stream;

        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder topP(Double topP) { this.topP = topP; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder stream(Boolean stream) { this.stream = stream; return this; }
        public ChatRequest build() { return new ChatRequest(this); }
    }

    public String getModel() { return model; }
    public List<ChatMessage> getMessages() { return messages; }
    public Double getTemperature() { return temperature; }
    public Double getTopP() { return topP; }
    public Integer getMaxTokens() { return maxTokens; }
    public Boolean getStream() { return stream; }
}
