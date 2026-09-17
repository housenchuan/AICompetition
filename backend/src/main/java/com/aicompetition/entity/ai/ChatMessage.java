package com.aicompetition.entity.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    private String role;
    private String content;
    /** 推理模型（如 DeepSeek）的思考过程字段，仅作兼容映射，业务取值仍用 content。 */
    @JsonProperty("reasoning_content")
    private String reasoningContent;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) { return new ChatMessage("system", content); }
    public static ChatMessage user(String content) { return new ChatMessage("user", content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReasoningContent() { return reasoningContent; }
    public void setReasoningContent(String reasoningContent) { this.reasoningContent = reasoningContent; }
}
