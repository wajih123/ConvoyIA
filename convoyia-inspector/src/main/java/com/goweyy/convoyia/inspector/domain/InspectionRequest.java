package com.goweyy.convoyia.inspector.domain;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InspectionRequest {
    String missionId;
    String tenantId;
    InspectionPhase phase;
    List<String> photoUrls;
    Long odometerReading;
    Integer fuelLevelPercent;
    String conveyorId;
    VehicleSegment vehicleSegment;
}
