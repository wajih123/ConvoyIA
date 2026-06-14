package com.goweyy.convoyia.pricer.convoy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConvoyPricingBreakdown {
    private BigDecimal transportCost;
    private BigDecimal segmentSurcharge;
    /** Insurance = BigDecimal.ZERO — TODO: fill after Hiscox contract signature */
    private BigDecimal vehicleInsuranceCost;
    private BigDecimal rcProPlatformCost;
    private BigDecimal subtotalBeforeBonus;
    private BigDecimal nightBonusAmount;
    private BigDecimal weekendBonusAmount;
    private BigDecimal urgencyBonusAmount;
    private BigDecimal totalHt;
    private BigDecimal vatAmount;
    private BigDecimal totalTtc;
    /** conveyorShare = totalTtc * CONVEYOR_SHARE_RATIO (0.75) */
    private BigDecimal conveyorPayout;
    /** platformShare = totalTtc * PLATFORM_SHARE_RATIO (0.25) */
    private BigDecimal platformFeeAmount;
    /** Stripe pre-auth = totalTtc * 1.20 */
    private BigDecimal stripePreAuthAmount;
    private boolean minimumFareApplied;
    private String currencyCode;
    private String currencySymbol;
    private String taxName;
}
