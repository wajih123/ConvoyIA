package com.goweyy.convoyia.pricer.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConvoyPricingRequest {
    private String missionId;
    private String tenantId;
    private ConvoyVehicleSegment vehicleSegment;
    private ConvoyUrgency urgency;
    private double vehicleDeclaredValue;
    private double estimatedDistanceKm;
    private LocalDateTime requestedAt;
}
