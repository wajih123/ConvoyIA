package com.goweyy.convoyia.tracker.agent;

import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.tracker.domain.AnomalyAlert;
import com.goweyy.convoyia.tracker.domain.GpsPosition;
import com.goweyy.convoyia.tracker.domain.TrackingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerAgent {

    private final AnomalyDetectionService anomalyDetectionService;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final DatabaseClient databaseClient;

    // SSE sinks per mission
    private final ConcurrentHashMap<String, Sinks.Many<TrackingEvent>> missionSinks = new ConcurrentHashMap<>();

    public Mono<Void> processPosition(GpsPosition position) {
        return savePosition(position)
                .then(anomalyDetectionService.detectAnomalies(position.getMissionId(), position))
                .flatMap(anomalies -> {
                    emitPositionEvent(position);
                    if (!anomalies.isEmpty()) {
                        return handleAnomalies(position.getMissionId(), anomalies);
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> savePosition(GpsPosition position) {
        return databaseClient.sql("""
                        INSERT INTO tracking_positions
                            (mission_id, tenant_id, conveyor_id, latitude, longitude, speed_kmh, accuracy, recorded_at)
                        VALUES (:missionId, :tenantId, :conveyorId, :lat, :lon, :speed, :accuracy, :recordedAt)
                        """)
                .bind("missionId", UUID.fromString(position.getMissionId()))
                .bind("tenantId", position.getTenantId())
                .bind("conveyorId", position.getConveyorId() != null ? position.getConveyorId() : "")
                .bind("lat", position.getLatitude())
                .bind("lon", position.getLongitude())
                .bind("speed", position.getSpeedKmh())
                .bind("accuracy", position.getAccuracy())
                .bind("recordedAt", position.getTimestamp() != null ? position.getTimestamp() : Instant.now())
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<Void> handleAnomalies(String missionId, List<AnomalyAlert> anomalies) {
        for (AnomalyAlert alert : anomalies) {
            log.warn("Anomaly detected for missionId={} type={} severity={}: {}",
                    missionId, alert.getAnomalyType(), alert.getSeverity(), alert.getDescription());

            TrackingEvent anomalyEvent = TrackingEvent.builder()
                    .missionId(missionId)
                    .tenantId(alert.getTenantId())
                    .type("ANOMALY")
                    .payload(Map.of(
                            "anomalyType", alert.getAnomalyType(),
                            "severity", alert.getSeverity(),
                            "description", alert.getDescription()
                    ))
                    .timestamp(Instant.now())
                    .build();

            emitEvent(missionId, anomalyEvent);
        }

        return kafkaEventPublisher.publishEvent(
                Map.of("missionId", missionId, "anomalies", anomalies),
                KafkaTopicsConfig.TOPIC_MISSION_TRACKER_ANOMALY
        );
    }

    public reactor.core.publisher.Flux<TrackingEvent> streamEvents(String missionId) {
        Sinks.Many<TrackingEvent> sink = missionSinks.computeIfAbsent(
                missionId,
                id -> Sinks.many().multicast().onBackpressureBuffer()
        );
        return sink.asFlux();
    }

    private void emitPositionEvent(GpsPosition position) {
        TrackingEvent event = TrackingEvent.builder()
                .missionId(position.getMissionId())
                .tenantId(position.getTenantId())
                .type("POSITION_UPDATE")
                .payload(Map.of(
                        "latitude", position.getLatitude(),
                        "longitude", position.getLongitude(),
                        "speedKmh", position.getSpeedKmh()
                ))
                .timestamp(Instant.now())
                .build();
        emitEvent(position.getMissionId(), event);
    }

    private void emitEvent(String missionId, TrackingEvent event) {
        Sinks.Many<TrackingEvent> sink = missionSinks.get(missionId);
        if (sink != null) {
            sink.tryEmitNext(event);
        }
    }
}
