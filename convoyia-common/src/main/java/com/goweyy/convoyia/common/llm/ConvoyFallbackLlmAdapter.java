package com.goweyy.convoyia.common.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyFallbackLlmAdapter {

    private final WebClient.Builder webClientBuilder;

    @Value("${anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${anthropic.base-url:https://api.anthropic.com}")
    private String anthropicBaseUrl;

    public String invoke(ConvoyLlmRequest request) {
        log.warn("ConvoyFallback LLM activated for model={}", request.getModel());

        Map<String, Object> body = Map.of(
                "model", "claude-sonnet-4-6",
                "max_tokens", request.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "user", "content", request.getPrompt())
                )
        );

        return webClientBuilder.build()
                .post()
                .uri(anthropicBaseUrl + "/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    var content = (List<Map<String, Object>>) response.get("content");
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get(0).get("text");
                    }
                    return "";
                })
                .block(Duration.ofSeconds(30));
    }
}
