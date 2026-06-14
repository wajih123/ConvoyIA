package com.goweyy.convoyia.tracker.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.tracker.domain.AnomalyAlert;
import com.goweyy.convoyia.tracker.domain.GpsPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final int LONG_STOP_MINUTES = 15;
    private static final double ROUTE_DEVIATION_KM = 5.0;

    private final LlmGateway llmGateway;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public Mono<List<AnomalyAlert>> detectAnomalies(String missionId, GpsPosition latest) {
        List<AnomalyAlert> alerts = new ArrayList<>();

        // Rule-based checks first (before LLM to save tokens)
        return getRecentPositions(missionId, 10)
                .flatMap(positions -> {
                    // LONG_STOP check
                    if (!positions.isEmpty()) {
                        GpsPosition previous = positions.get(positions.size() - 1);
                        if (latest.getSpeedKmh() < 1.0 && previous.getSpeedKmh() < 1.0) {
                            long minutesStopped = ChronoUnit.MINUTES.between(previous.getTimestamp(), latest.getTimestamp());
                            if (minutesStopped >= LONG_STOP_MINUTES) {
                                alerts.add(AnomalyAlert.builder()
                                        .missionId(missionId)
                                        .tenantId(latest.getTenantId())
                                        .anomalyType("LONG_STOP")
                                        .severity("WARNING")
                                        .description("Véhicule arrêté depuis " + minutesStopped + " minutes")
                                        .position(latest)
                                        .detectedAt(Instant.now())
                                        .build());
                            }
                        }
                    }

                    // If rule-based checks found nothing, use LLM for complex analysis
                    if (alerts.isEmpty() && positions.size() >= 3) {
                        return runLlmAnomalyCheck(missionId, latest, positions)
                                .map(llmAlerts -> {
                                    alerts.addAll(llmAlerts);
                                    return alerts;
                                });
                    }
                    return Mono.just(alerts);
                });
    }

    private Mono<List<AnomalyAlert>> runLlmAnomalyCheck(String missionId, GpsPosition latest, List<GpsPosition> history) {
        String prompt = buildAnomalyPrompt(missionId, latest, history);
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.LLAMA3_8B)
                .prompt(prompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
                        boolean anomalous = Boolean.TRUE.equals(parsed.get("anomalous"));
                        if (anomalous) {
                            return List.of(AnomalyAlert.builder()
                                    .missionId(missionId)
                                    .tenantId(latest.getTenantId())
                                    .anomalyType("LLM_DETECTED")
                                    .severity((String) parsed.getOrDefault("severity", "WARNING"))
                                    .description((String) parsed.getOrDefault("reason", "Anomalie détectée par IA"))
                                    .position(latest)
                                    .detectedAt(Instant.now())
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse LLM anomaly response for missionId={}: {}", missionId, e.getMessage());
                    }
                    return List.<AnomalyAlert>of();
                })
                .onErrorReturn(List.of());
    }

    private String buildAnomalyPrompt(String missionId, GpsPosition latest, List<GpsPosition> history) {
        return """
                Tu es un agent de surveillance de convoyage automobile.
                Mission: %s
                
                Analyse les dernières positions GPS et détermine si la situation est anormale.
                
                Position actuelle: lat=%f, lon=%f, vitesse=%f km/h, à %s
                Historique (dernières positions): %d positions disponibles
                
                Anomalies à détecter:
                - LONG_STOP: arrêt > 15 minutes
                - ROUTE_DEVIATION: déviation > 5km de la route prévue
                - MISSION_TIMEOUT: durée > 150%% de la durée estimée
                
                Réponds UNIQUEMENT en JSON valide:
                {
                  "anomalous": false,
                  "anomalyType": "LONG_STOP|ROUTE_DEVIATION|MISSION_TIMEOUT|null",
                  "severity": "INFO|WARNING|CRITICAL",
                  "reason": "Explication"
                }
                """.formatted(missionId, latest.getLatitude(), latest.getLongitude(),
                latest.getSpeedKmh(), latest.getTimestamp(), history.size());
    }

    @SuppressWarnings("unchecked")
    private Mono<List<GpsPosition>> getRecentPositions(String missionId, int limit) {
        return databaseClient.sql("""
                        SELECT * FROM tracking_positions
                        WHERE mission_id = :missionId
                        ORDER BY recorded_at DESC
                        LIMIT :limit
                        """)
                .bind("missionId", java.util.UUID.fromString(missionId))
                .bind("limit", limit)
                .fetch()
                .all()
                .map(row -> GpsPosition.builder()
                        .missionId(missionId)
                        .tenantId((String) row.get("tenant_id"))
                        .conveyorId((String) row.get("conveyor_id"))
                        .latitude(((Number) row.get("latitude")).doubleValue())
                        .longitude(((Number) row.get("longitude")).doubleValue())
                        .speedKmh(row.get("speed_kmh") != null ? ((Number) row.get("speed_kmh")).doubleValue() : 0.0)
                        .timestamp((Instant) row.get("recorded_at"))
                        .build())
                .collectList();
    }
}
