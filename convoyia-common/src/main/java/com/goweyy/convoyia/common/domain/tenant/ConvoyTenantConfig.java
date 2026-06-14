package com.goweyy.convoyia.common.domain.tenant;

import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Master tenant configuration for ConvoyIA worldwide.
 * Every dimension that varies per country or client is configurable here.
 * Goweyy (France) is tenant #1 — all defaults reflect its settings.
 *
 * Stored in convoy_tenant_configs (see V16__convoyia_worldwide.sql).
 */
@Value
@Builder
public class ConvoyTenantConfig {

    // Identity
    String tenantId;          // "goweyy", "driiveme", "hiflow-uk"...
    String tenantName;        // "Goweyy", "Driiveme"
    String tenantLogoUrl;     // for white-label invoices/docs

    // Market
    ConvoyMarket market;

    String currencyCode;      // ISO 4217: "EUR", "GBP", "USD"
    String currencySymbol;    // "€", "£", "$"
    String timezone;          // "Europe/Paris"
    String locale;            // "fr-FR" — used in LLM prompts + documents
    String countryCode;       // "fr", "gb"

    // Tax
    String taxName;           // "TVA", "VAT", "GST", "Tax"
    BigDecimal taxRate;       // 0.20 France, 0.20 UK, 0.10 AU, 0.05 UAE...
    String taxNumber;         // tenant VAT registration number

    // Regulatory — driver background check
    String backgroundCheckDocName;     // "Casier B3" FR, "DBS Check" UK
    Integer backgroundCheckMaxAgeDays; // 90 days FR, 365 days UK

    // Driver license categories accepted (JSON: ["B","BE"])
    String acceptedLicenseCategories;

    // Insurance
    String insuranceProviderName;       // "Hiscox" for Goweyy
    BigDecimal insuranceCeilingAmount;  // 120000 EUR for Goweyy
    String insuranceCurrency;

    // Return trip partner
    String returnTripPartnerName;    // "Bolt Business", "Uber Business"
    String returnTripPartnerApiUrl;  // future integration endpoint

    // Pricing
    /**
     * Platform fee ratio applied on totalTtc.
     * Goweyy: 0.25 (25%) — LOCKED. White-label tenants configure their own ratio.
     */
    BigDecimal platformFeeRatio;
    BigDecimal minimumFareTtc;       // 30.00 EUR Goweyy

    // LLM language for prompts and generated documents
    String llmPromptLanguage;        // "french", "english", "german", "arabic"...

    // Stripe Connect
    String stripeAccountId;   // tenant's Stripe Connect account
    String stripeCurrency;    // lowercase: "eur", "gbp", "aed"

    boolean active;
    Instant createdAt;
    Instant updatedAt;
}
