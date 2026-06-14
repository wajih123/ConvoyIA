package com.goweyy.convoyia.pricer.domain;

import com.goweyy.convoyia.common.domain.enums.PricingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PricingResult {
    String missionId;
    String tenantId;
    PricingStatus status;
    /** Full breakdown — null when status is PENDING_MANUAL_QUOTE. */
    PricingBreakdown pricingBreakdown;
    Instant pricedAt;
}
