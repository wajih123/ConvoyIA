package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.MissionState;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class DispatchDecision {
    String missionId;
    String tenantId;
    MissionState finalState;
    String assignedConveyorId;
    int estimatedDurationMin;
    String routingNotes;
    List<String> agentTrace;
    Instant decidedAt;
}
