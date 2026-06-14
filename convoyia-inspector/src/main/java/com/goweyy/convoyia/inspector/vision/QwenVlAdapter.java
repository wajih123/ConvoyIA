package com.goweyy.convoyia.inspector.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.llm.FallbackLlmAdapter;
import com.goweyy.convoyia.inspector.domain.DamageReport;
import com.goweyy.convoyia.inspector.domain.InspectionPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QwenVlAdapter {

    private static final Duration VISION_TIMEOUT = Duration.ofSeconds(30);

    @Qualifier("ollamaWebClient")
    private final WebClient ollamaWebClient;
    private final FallbackLlmAdapter fallbackLlmAdapter;
    private final ObjectMapper objectMapper;

    public Mono<DamageReport> analyzeImage(String base64Image, InspectionPhase phase, String missionId) {
        Map<String, Object> body = Map.of(
                "model", LlmModel.QWEN_VL_7B.getOllamaTag(),
                "prompt", buildVisionPrompt(base64Image, phase),
                "stream", false,
                "format", "json"
        );

        return ollamaWebClient.post()
                .uri("/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(com.goweyy.convoyia.common.llm.OllamaResponse.class)
                .timeout(VISION_TIMEOUT)
                .map(response -> parseResponse(response.getResponse(), base64Image))
                .onErrorResume(e -> {
                    log.warn("Qwen-VL failed for missionId={}, falling back to Claude with vision: {}", missionId, e.getMessage());
                    LlmRequest fallbackReq = LlmRequest.builder()
                            .model(LlmModel.CLAUDE_SONNET)
                            .prompt(buildVisionPrompt(base64Image, phase))
                            .expectedFormat(ResponseFormat.JSON)
                            .maxTokens(1000)
                            .build();
                    return fallbackLlmAdapter.invoke(fallbackReq)
                            .map(resp -> parseResponse(resp, base64Image));
                });
    }

    private DamageReport parseResponse(String response, String base64Image) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> parsed = (Map<String, Object>) objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            List<String> damageAreas = (List<String>) parsed.getOrDefault("damageAreas", List.of());
            String severity = (String) parsed.getOrDefault("severity", "NONE");
            return DamageReport.builder()
                    .areas(damageAreas)
                    .severity(severity)
                    .newDamagesVsDeparture(List.of())
                    .requiresHiscoxAlert("MAJOR".equals(severity) || "CRITICAL".equals(severity))
                    .photoUrls(List.of())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Qwen-VL response: {}", e.getMessage());
            return DamageReport.builder()
                    .areas(List.of())
                    .severity("NONE")
                    .newDamagesVsDeparture(List.of())
                    .requiresHiscoxAlert(false)
                    .photoUrls(List.of())
                    .build();
        }
    }

    private String buildVisionPrompt(String base64Image, InspectionPhase phase) {
        return "Analyse l'état du véhicule en phase " + phase.name() + 
               ". Identifie les dommages visibles, l'état général, le kilométrage et le niveau de carburant si visible. " +
               "Réponds UNIQUEMENT en JSON avec: damageAreas, severity (NONE|MINOR|MAJOR|CRITICAL), odometer, fuelLevel, overallCondition.";
    }
}
