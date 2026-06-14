package com.goweyy.convoyia.common.domain.events;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InspectionCompletedEvent {
    String missionId;
    String tenantId;
    boolean damageDetected;
    Instant occurredAt;
}
