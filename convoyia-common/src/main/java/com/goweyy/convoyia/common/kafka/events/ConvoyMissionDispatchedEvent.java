package com.goweyy.convoyia.common.kafka.events;

import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyMissionDispatchedEvent {
    private String missionId;
    private String tenantId;
    private ConvoyVehicleSegment vehicleSegment;
    private ConvoyUrgency urgency;
    private String originAddress;
    private String destinationAddress;
    private Instant occurredAt;
}
