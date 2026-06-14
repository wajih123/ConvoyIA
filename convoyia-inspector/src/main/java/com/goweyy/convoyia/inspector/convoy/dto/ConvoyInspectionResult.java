package com.goweyy.convoyia.inspector.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyInspectionPhase;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyInspectionResult {
    private String missionId;
    private String tenantId;
    private ConvoyInspectionPhase phase;
    private boolean passed;
    private ConvoyDamageReport damageReport;
    private String notes;
    private Instant inspectedAt;
}
