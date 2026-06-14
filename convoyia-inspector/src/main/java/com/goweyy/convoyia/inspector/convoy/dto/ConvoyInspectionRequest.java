package com.goweyy.convoyia.inspector.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyInspectionPhase;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConvoyInspectionRequest {
    private String missionId;
    private String tenantId;
    private ConvoyInspectionPhase phase;
    private List<String> photoUrls;
    private String vehiclePlate;
}
