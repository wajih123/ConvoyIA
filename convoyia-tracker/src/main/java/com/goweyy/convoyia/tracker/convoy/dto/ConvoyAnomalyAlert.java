package com.goweyy.convoyia.tracker.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyAnomalyAlert {
    private String missionId;
    private String tenantId;
    private String anomalyType;
    private ConvoyAlertSeverity severity;
    private String description;
    private ConvoyGpsPosition position;
    private Instant detectedAt;
}
