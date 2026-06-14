package com.goweyy.convoyia.common.domain.events;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MissionCompletedEvent {
    String missionId;
    String tenantId;
    Instant occurredAt;
}
