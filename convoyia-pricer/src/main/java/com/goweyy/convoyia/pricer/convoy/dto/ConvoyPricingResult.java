package com.goweyy.convoyia.pricer.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyPricingResult {
    private String missionId;
    private String tenantId;
    private ConvoyPricingStatus status;
    private ConvoyPricingBreakdown pricingBreakdown;
    private Instant pricedAt;
}
