package com.goweyy.convoyia.tracker.convoy.agent;

import com.goweyy.convoyia.tracker.convoy.dto.ConvoyAnomalyAlert;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyTrackingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyTrackerAgent {

    private final ConvoyAnomalyDetectionService anomalyDetectionService;

    public ConvoyTrackingEvent track(ConvoyGpsPosition position) {
        log.debug("ConvoyTrackerAgent tracking missionId={}", position.getMissionId());

        Optional<ConvoyAnomalyAlert> anomaly = anomalyDetectionService.detectAnomaly(position);
        anomaly.ifPresent(a ->
                log.warn("Anomaly detected missionId={} type={}", a.getMissionId(), a.getAnomalyType()));

        return ConvoyTrackingEvent.builder()
                .missionId(position.getMissionId())
                .tenantId(position.getTenantId())
                .eventType(anomaly.isPresent() ? "ANOMALY" : "POSITION_UPDATE")
                .position(position)
                .occurredAt(Instant.now())
                .build();
    }
}
