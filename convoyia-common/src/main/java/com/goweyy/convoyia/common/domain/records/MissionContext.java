package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.MissionState;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MissionContext {
    String missionId;
    MissionRequest originalRequest;
    MissionState currentState;
    VehicleSegment vehicleSegment;
    Double confidenceScore;
    String assignedConveyorId;
    List<String> agentTrace;
    Map<String, Object> enrichedData;
    Instant lastUpdated;
    String tenantId;
}
