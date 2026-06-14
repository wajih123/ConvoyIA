package com.goweyy.convoyia.dispatcher.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ConvoyDispatchDecision {
    private String missionId;
    private String tenantId;
    private ConvoyMissionState finalState;
    private ConvoyVehicleSegment vehicleSegment;
    private Double confidenceScore;
    private Integer estimatedDurationMin;
    private String routingNotes;
    private List<String> agentTrace;
    private Instant decidedAt;
}
