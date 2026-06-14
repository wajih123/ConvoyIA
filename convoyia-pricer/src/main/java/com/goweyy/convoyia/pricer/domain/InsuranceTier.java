package com.goweyy.convoyia.pricer.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * One tier in the vehicle insurance formula.
 * Tiers are ordered ascending by maxVehicleValue.
 * The last tier has maxVehicleValue = Double.MAX_VALUE → signals manual quote.
 *
 * ⚠️ costPerMission values are PLACEHOLDERS — replace with real insurance
 *    contract amounts validated with your insurance broker before go-live.
 */
@Value
@Builder
public class InsuranceTier {
    /** Upper bound of this tier (exclusive). */
    double maxVehicleValue;
    /**
     * Cost added to mission price for this tier.
     * Set to 0.00 until real contract amounts are confirmed.
     */
    BigDecimal costPerMission;
}
