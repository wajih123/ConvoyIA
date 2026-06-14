package com.goweyy.convoyia.tracker.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AnomalyAlert {
    String missionId;
    String tenantId;
    String anomalyType; // LONG_STOP | ROUTE_DEVIATION | MISSION_TIMEOUT
    String severity;
    String description;
    GpsPosition position;
    Instant detectedAt;
}
