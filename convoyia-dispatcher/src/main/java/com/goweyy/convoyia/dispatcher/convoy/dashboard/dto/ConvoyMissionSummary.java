package com.goweyy.convoyia.dispatcher.convoy.dashboard.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConvoyMissionSummary {
    private String missionId;
    private String tenantId;
    private ConvoyMissionState state;
    private String originAddress;
    private String destinationAddress;
    private BigDecimal totalTtc;
    private String currencyCode;
    private Instant createdAt;
}
