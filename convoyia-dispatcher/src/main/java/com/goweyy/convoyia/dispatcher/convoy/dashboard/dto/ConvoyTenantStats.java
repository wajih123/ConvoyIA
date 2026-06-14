package com.goweyy.convoyia.dispatcher.convoy.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ConvoyTenantStats {
    private String tenantId;
    private long totalMissions;
    private long completedMissions;
    private long failedMissions;
    private long escalatedMissions;
    private BigDecimal totalRevenueTtc;
    private String currencyCode;
    private Map<String, Long> missionsByState;
}
