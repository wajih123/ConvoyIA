package com.goweyy.convoyia.common.domain.events;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PricingCompletedEvent {
    String missionId;
    String tenantId;
    BigDecimal totalTtc;
    BigDecimal conveyorShare;
    BigDecimal platformShare;
    Instant occurredAt;
}
