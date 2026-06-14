package com.goweyy.convoyia.pricer.domain;

import com.goweyy.convoyia.common.domain.enums.VehicleCoverageMode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Insurance configuration for a tenant's pricing formula.
 *
 * IMPORTANT — insurance responsibilities:
 * - Hiscox = RC Pro for the PLATFORM (Goweyy's professional liability). NOT vehicle coverage.
 * - vehicleCoverageTiers = separate vehicle coverage per mission (type TBD with broker).
 * - Conveyor's own RC Pro as independent: managed by conveyor, NOT in this config.
 *
 * ⚠️ rcProPlatformAnnualCost: PLACEHOLDER — replace with real Hiscox annual contract amount.
 * ⚠️ vehicleCoverageTiers.costPerMission: PLACEHOLDER — replace with real broker amounts.
 */
@Value
@Builder
public class InsuranceConfig {
    VehicleCoverageMode vehicleCoverageMode;
    /** Ordered ascending by maxVehicleValue. */
    List<InsuranceTier> vehicleCoverageTiers;
    /**
     * Hiscox annual RC Pro contract cost for the platform (Goweyy).
     * TODO: replace with real value after Hiscox contract signature.
     */
    BigDecimal rcProPlatformAnnualCost;
    /**
     * Estimated annual missions — divisor to get per-mission RC Pro cost.
     * TODO: adjust based on actual mission volume.
     */
    int rcProEstimatedAnnualMissions;
}
