package com.goweyy.convoyia.tracker.convoy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.llm.ConvoyLlmGateway;
import com.goweyy.convoyia.common.llm.ConvoyLlmRequest;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyAnomalyAlert;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyTrackingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyTrackerAgent {

    private final ConvoyAnomalyDetectionService anomalyDetectionService;
    private final ConvoyLlmGateway llmGateway;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    public ConvoyTrackingEvent track(ConvoyGpsPosition position) {
        log.debug("ConvoyTrackerAgent tracking missionId={}", position.getMissionId());

        Optional<ConvoyAnomalyAlert> anomaly = anomalyDetectionService.detectAnomaly(position);
        anomaly.ifPresent(a -> log.warn("Anomaly detected missionId={} type={}", a.getMissionId(), a.getAnomalyType()));

        Optional<ConvoyAnomalyAlert> confirmed = anomaly.flatMap(this::confirmWithLlm);
        confirmed.ifPresent(alert -> kafkaEventPublisher.publishEvent(alert, ConvoyKafkaTopicsConfig.TOPIC_CONVOY_TRACKER_ANOMALY));

        return ConvoyTrackingEvent.builder()
                .missionId(position.getMissionId())
                .tenantId(position.getTenantId())
                .eventType(confirmed.isPresent() ? "ANOMALY" : "POSITION_UPDATE")
                .position(position)
                .occurredAt(Instant.now())
                .build();
    }

    private Optional<ConvoyAnomalyAlert> confirmWithLlm(ConvoyAnomalyAlert alert) {
        try {
            String response = llmGateway.invoke(ConvoyLlmRequest.builder()
                    .model(ConvoyLlmModel.LLAMA3_8B)
                    .prompt("""
                            Analyse this convoy anomaly and respond JSON only.
                            {"anomalous":true|false,"type":"...","severity":"INFO|WARNING|CRITICAL","recommendation":"..."}
                            missionId=%s anomalyType=%s description=%s speed=%s
                            """.formatted(alert.getMissionId(), alert.getAnomalyType(), alert.getDescription(), alert.getPosition().getSpeedKmh()))
                    .maxTokens(300)
                    .build());
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            if (!Boolean.TRUE.equals(parsed.get("anomalous"))) {
                return Optional.empty();
            }
            String type = parsed.get("type") != null ? String.valueOf(parsed.get("type")) : alert.getAnomalyType();
            String recommendation = parsed.get("recommendation") != null ? String.valueOf(parsed.get("recommendation")) : alert.getDescription();
            ConvoyAlertSeverity severity = parsed.get("severity") != null
                    ? ConvoyAlertSeverity.valueOf(String.valueOf(parsed.get("severity")).toUpperCase())
                    : alert.getSeverity();
            return Optional.of(ConvoyAnomalyAlert.builder()
                    .missionId(alert.getMissionId())
                    .tenantId(alert.getTenantId())
                    .anomalyType(type)
                    .severity(severity)
                    .description(recommendation)
                    .position(alert.getPosition())
                    .detectedAt(Instant.now())
                    .build());
        } catch (Exception exception) {
            log.warn("LLM anomaly confirmation failed for missionId={}: {}", alert.getMissionId(), exception.getMessage());
            return Optional.of(alert);
        }
    }
}
