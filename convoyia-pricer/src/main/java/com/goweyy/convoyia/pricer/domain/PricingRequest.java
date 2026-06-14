package com.goweyy.convoyia.pricer.domain;

import com.goweyy.convoyia.common.domain.enums.MissionUrgency;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PricingRequest {
    String missionId;
    String tenantId;
    VehicleSegment vehicleSegment;
    double vehicleDeclaredValue;
    double estimatedDistanceKm;
    LocalDateTime requestedAt;
    MissionUrgency urgency;
}
