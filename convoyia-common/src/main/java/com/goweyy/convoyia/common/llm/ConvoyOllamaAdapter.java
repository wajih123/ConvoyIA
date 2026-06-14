package com.goweyy.convoyia.common.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyOllamaAdapter {

    private final WebClient ollamaWebClient;

    @Value("${convoyia.llm.timeout-seconds:8}")
    private int timeoutSeconds;

    public String invoke(ConvoyLlmRequest request) {
        Map<String, Object> body = Map.of(
                "model", request.getModel().getTag(),
                "prompt", request.getPrompt(),
                "stream", false,
                "format", "json"
        );

        return ollamaWebClient.post()
                .uri("/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .map(OllamaResponse::getResponse)
                .block(Duration.ofSeconds(timeoutSeconds));
    }
}
