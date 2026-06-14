package com.goweyy.convoyia.pricer.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PricingResult {
    String missionId;
    String tenantId;
    String status;
    BigDecimal baseFare;
    BigDecimal segmentSurcharge;
    BigDecimal nightBonus;
    BigDecimal totalHt;
    BigDecimal totalTtc;
    BigDecimal stripePreAuthAmount;
    BigDecimal conveyorShare;
    BigDecimal platformShare;
    BigDecimal estimatedReturnCost;
    @Builder.Default
    String currency = "EUR";
    Instant pricedAt;
}
