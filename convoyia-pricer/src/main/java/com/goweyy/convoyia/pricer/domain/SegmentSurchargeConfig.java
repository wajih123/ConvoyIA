package com.goweyy.convoyia.pricer.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Segment surcharge configuration — amounts in EUR HT, per segment.
 * LUXE_PLATEAU always returns PENDING_MANUAL_QUOTE — no surcharge computed.
 */
@Value
@Builder
public class SegmentSurchargeConfig {
    BigDecimal standard;
    BigDecimal courant;
    BigDecimal premium;
    BigDecimal hautDeGamme;
}
