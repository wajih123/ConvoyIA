package com.goweyy.convoyia.inspector.convoy.vision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyQwenVlAdapter {

    private final WebClient ollamaWebClient;

    @Value("${convoyia.llm.vision-timeout-seconds:30}")
    private int visionTimeoutSeconds;

    public String analyze(String prompt, List<String> imageUrls) {
        log.debug("ConvoyQwenVlAdapter analyzing {} images", imageUrls.size());

        Map<String, Object> body = Map.of(
                "model", "qwen:7b",
                "prompt", prompt,
                "images", imageUrls,
                "stream", false,
                "format", "json"
        );

        try {
            Map<?, ?> response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(visionTimeoutSeconds));
            return response != null && response.get("response") != null
                    ? response.get("response").toString()
                    : "";
        } catch (Exception e) {
            log.warn("ConvoyQwenVlAdapter error: {}", e.getMessage());
            return "";
        }
    }
}
