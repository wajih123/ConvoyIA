package com.goweyy.convoyia.pricer.domain;

import com.goweyy.convoyia.common.domain.enums.TransportPricingMode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Complete pricing formula configuration for one tenant.
 * Stored in DB — Goweyy uses the default seed. White-label tenants can override.
 *
 * ⚠️ platformFeeRatio for Goweyy is LOCKED at 0.25 (25%). No exceptions.
 */
@Value
@Builder
public class PricingFormulaConfig {
    String tenantId;
    TransportPricingMode transportMode;
    /**
     * Rate per km — used for DISTANCE_BASED and HYBRID modes.
     * TODO: replace with real validated commercial rate before launch.
     */
    BigDecimal ratePerKm;
    /** Flat base fare — used for FLAT_RATE and HYBRID modes. */
    BigDecimal flatBaseFare;
    /** Enforced minimum fare TTC. Goweyy default: 30.00 EUR. LOCKED. */
    BigDecimal minimumFare;
    /**
     * Platform fee ratio applied on totalTtc.
     * Goweyy: 0.25 (25%) — conveyor gets 0.75 (75%). LOCKED for Goweyy.
     * White-label tenants may configure their own ratio.
     */
    BigDecimal platformFeeRatio;
    BigDecimal vatRate;
    BigDecimal stripePreAuthMultiplier;
    InsuranceConfig insuranceConfig;
    SegmentSurchargeConfig segmentSurcharges;
    ContextMultipliersConfig contextMultipliers;
    boolean active;
    Instant createdAt;
    Instant updatedAt;

    // Multi-currency / multi-tenant fields
    /** ISO 4217 currency code — from tenant config (e.g. "EUR", "GBP", "AED"). Defaults to "EUR". */
    String currencyCode;
    /** Currency symbol for display (e.g. "€", "£", "د.إ"). Defaults to "€". */
    String currencySymbol;
    /** Local tax name (e.g. "TVA", "VAT", "GST"). Defaults to "TVA". */
    String taxName;
}
