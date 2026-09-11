package com.aicompetition.service;

import com.aicompetition.config.AiCoderProperties;
import com.aicompetition.entity.ai.ChatMessage;
import com.aicompetition.entity.ai.ChatRequest;
import com.aicompetition.entity.ai.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiCoderProperties properties;
    private final WebClient.Builder webClientBuilder;

    public AiService(AiCoderProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<ChatResponse> chat(List<ChatMessage> messages, Double temperature, Integer maxTokens) {
        ChatRequest request = ChatRequest.builder()
                .model(properties.getModel())
                .messages(messages)
                .temperature(temperature != null ? temperature : properties.getDefaultTemperature())
                .maxTokens(maxTokens != null ? maxTokens : properties.getDefaultMaxTokens())
                .stream(false)
                .build();

        return buildClient()
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(e -> log.error("AI chat error: {}", e.getMessage()));
    }

    private WebClient buildClient() {
        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
