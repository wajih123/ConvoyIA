package com.goweyy.convoyia.common.domain.enums;

import java.math.BigDecimal;

/**
 * Worldwide markets supported by ConvoyIA.
 * Each market defines its ISO country code, currency, timezone, locale, tax name and default tax rate.
 */
public enum ConvoyMarket {

    FRANCE("fr", "EUR", "Europe/Paris", "fr-FR", "TVA", new BigDecimal("0.20")),
    UNITED_KINGDOM("gb", "GBP", "Europe/London", "en-GB", "VAT", new BigDecimal("0.20")),
    GERMANY("de", "EUR", "Europe/Berlin", "de-DE", "MwSt", new BigDecimal("0.19")),
    SPAIN("es", "EUR", "Europe/Madrid", "es-ES", "IVA", new BigDecimal("0.21")),
    ITALY("it", "EUR", "Europe/Rome", "it-IT", "IVA", new BigDecimal("0.22")),
    NETHERLANDS("nl", "EUR", "Europe/Amsterdam", "nl-NL", "BTW", new BigDecimal("0.21")),
    BELGIUM("be", "EUR", "Europe/Brussels", "fr-BE", "TVA", new BigDecimal("0.21")),
    UAE("ae", "AED", "Asia/Dubai", "en-AE", "VAT", new BigDecimal("0.05")),
    SAUDI_ARABIA("sa", "SAR", "Asia/Riyadh", "ar-SA", "VAT", new BigDecimal("0.15")),
    AUSTRALIA("au", "AUD", "Australia/Sydney", "en-AU", "GST", new BigDecimal("0.10")),
    UNITED_STATES("us", "USD", "America/New_York", "en-US", "Tax", new BigDecimal("0.00")),
    CANADA("ca", "CAD", "America/Toronto", "en-CA", "GST", new BigDecimal("0.05"));

    private final String countryCode;
    private final String currencyCode;
    private final String timezone;
    private final String locale;
    private final String taxName;
    private final BigDecimal defaultTaxRate;

    ConvoyMarket(String countryCode, String currencyCode, String timezone, String locale,
                 String taxName, BigDecimal defaultTaxRate) {
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
        this.timezone = timezone;
        this.locale = locale;
        this.taxName = taxName;
        this.defaultTaxRate = defaultTaxRate;
    }

    public String getCountryCode() { return countryCode; }
    public String getCurrencyCode() { return currencyCode; }
    public String getTimezone() { return timezone; }
    public String getLocale() { return locale; }
    public String getTaxName() { return taxName; }
    public BigDecimal getDefaultTaxRate() { return defaultTaxRate; }
}
