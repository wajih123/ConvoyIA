package com.goweyy.convoyia.pricer.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Context-based multiplier configuration (night, weekend, urgency).
 */
@Value
@Builder
public class ContextMultipliersConfig {
    BigDecimal nightBonusRatio;
    /** Nullable — optional weekend bonus. */
    BigDecimal weekendBonusRatio;
    BigDecimal expressMultiplier;
    BigDecimal urgentMultiplier;
    int nightHourStart;
    int nightHourEnd;
}
