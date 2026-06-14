package com.goweyy.convoyia.common.llm;

import com.goweyy.convoyia.common.domain.records.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaAdapter {

    private final WebClient ollamaWebClient;

    public Mono<String> invoke(LlmRequest request) {
        Map<String, Object> body = Map.of(
                "model", request.getModel().getOllamaTag(),
                "prompt", request.getPrompt(),
                "stream", false,
                "format", "json"
        );

        return ollamaWebClient.post()
                .uri("/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .map(OllamaResponse::getResponse);
    }
}
