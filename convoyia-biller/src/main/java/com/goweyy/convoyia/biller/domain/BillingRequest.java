package com.goweyy.convoyia.biller.domain;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BillingRequest {
    String missionId;
    String tenantId;
    String conveyorId;
    String conveyorStripeAccountId;
    String clientId;
    String paymentIntentId;
    PricingResult pricingResult;
    boolean damageDetected;
    VehicleSegment vehicleSegment;

    // Tenant context for invoice generation (white-label + multi-currency)
    /** Display name of the tenant (e.g. "Goweyy"). Defaults to "Goweyy" if null. */
    String tenantName;
    /** ISO 4217 currency code (e.g. "EUR", "GBP", "AED"). */
    String currencyCode;
    /** Currency symbol for display (e.g. "€", "£", "د.إ"). */
    String currencySymbol;
    /** Local tax name (e.g. "TVA", "VAT", "GST"). */
    String taxName;
    /** Tax rate as a decimal (e.g. 0.20 for 20%). */
    BigDecimal taxRate;
    /** IANA timezone identifier (e.g. "Europe/Paris"). */
    String timezone;
    /** Platform fee ratio (e.g. 0.25 for 25%). */
    BigDecimal platformFeeRatio;
}
