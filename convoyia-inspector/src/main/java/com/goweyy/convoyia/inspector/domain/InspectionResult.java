package com.goweyy.convoyia.inspector.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InspectionResult {
    String missionId;
    String tenantId;
    InspectionPhase phase;
    DamageReport damageReport;
    boolean damageDetected;
    Long odometerAtInspection;
    Integer fuelLevelAtInspection;
    Instant inspectedAt;
    String inspectedBy;
}
