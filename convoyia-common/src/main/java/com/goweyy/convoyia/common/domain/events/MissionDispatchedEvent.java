package com.goweyy.convoyia.common.domain.events;

import com.goweyy.convoyia.common.domain.enums.MissionUrgency;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MissionDispatchedEvent {
    String missionId;
    String tenantId;
    VehicleSegment vehicleSegment;
    MissionUrgency urgency;
    Instant occurredAt;
}
