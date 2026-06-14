package com.goweyy.convoyia.tracker.convoy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyTrackingEvent {
    private String missionId;
    private String tenantId;
    private String eventType;
    private ConvoyGpsPosition position;
    private Instant occurredAt;
}
