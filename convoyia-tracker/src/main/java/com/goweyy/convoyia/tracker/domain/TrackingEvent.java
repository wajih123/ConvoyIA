package com.goweyy.convoyia.tracker.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class TrackingEvent {
    String missionId;
    String tenantId;
    String type; // POSITION_UPDATE | ANOMALY | ETA_UPDATE | MISSION_COMPLETED
    Map<String, Object> payload;
    Instant timestamp;
}
