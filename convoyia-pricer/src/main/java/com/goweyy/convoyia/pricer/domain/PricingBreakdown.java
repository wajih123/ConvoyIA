package com.goweyy.convoyia.pricer.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Full line-by-line pricing breakdown for transparency and audit.
 */
@Value
@Builder
public class PricingBreakdown {
    BigDecimal transportCost;
    BigDecimal segmentSurcharge;
    /** 0 if vehicleCoverageMode is EXTERNAL or INCLUDED. */
    BigDecimal vehicleInsuranceCost;
    /** Hiscox RC Pro per-mission share (platform professional liability). */
    BigDecimal rcProPlatformCost;
    BigDecimal subtotalBeforeBonus;
    BigDecimal nightBonusAmount;
    BigDecimal weekendBonusAmount;
    BigDecimal urgencyBonusAmount;
    BigDecimal totalHt;
    BigDecimal vatAmount;
    BigDecimal totalTtc;
    /** Platform's fee (Goweyy default: 25% of totalTtc). */
    BigDecimal platformFeeAmount;
    /** What the conveyor receives (Goweyy default: 75% of totalTtc). */
    BigDecimal conveyorPayout;
    BigDecimal stripePreAuthAmount;
    /** True if minimum fare was enforced. */
    boolean minimumFareApplied;
    /** Human-readable audit trail of applied formula parameters. */
    String appliedFormulaSummary;
}
