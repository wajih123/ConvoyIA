package com.goweyy.convoyia.tracker.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyAnomalyAlert;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class ConvoyAnomalyDetectionService {

    private static final double MAX_SPEED_KMH = 130.0;

    public Optional<ConvoyAnomalyAlert> detectAnomaly(ConvoyGpsPosition position) {
        if (position.getSpeedKmh() > MAX_SPEED_KMH) {
            log.warn("Speed anomaly detected missionId={} speed={}", position.getMissionId(), position.getSpeedKmh());
            return Optional.of(ConvoyAnomalyAlert.builder()
                    .missionId(position.getMissionId())
                    .tenantId(position.getTenantId())
                    .anomalyType("SPEED_EXCEEDED")
                    .severity(ConvoyAlertSeverity.WARNING)
                    .description(String.format("Speed %.1f km/h exceeds limit of %.1f km/h",
                            position.getSpeedKmh(), MAX_SPEED_KMH))
                    .position(position)
                    .detectedAt(Instant.now())
                    .build());
        }
        return Optional.empty();
    }
}
