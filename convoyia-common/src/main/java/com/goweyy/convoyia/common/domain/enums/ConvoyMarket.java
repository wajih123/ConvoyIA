package com.goweyy.convoyia.common.domain.enums;

/**
 * Worldwide markets supported by ConvoyIA.
 * Each market defines its ISO country code, currency, timezone and locale.
 */
public enum ConvoyMarket {

    FRANCE("fr", "EUR", "Europe/Paris", "fr-FR"),
    UNITED_KINGDOM("gb", "GBP", "Europe/London", "en-GB"),
    GERMANY("de", "EUR", "Europe/Berlin", "de-DE"),
    SPAIN("es", "EUR", "Europe/Madrid", "es-ES"),
    ITALY("it", "EUR", "Europe/Rome", "it-IT"),
    NETHERLANDS("nl", "EUR", "Europe/Amsterdam", "nl-NL"),
    BELGIUM("be", "EUR", "Europe/Brussels", "fr-BE"),
    UAE("ae", "AED", "Asia/Dubai", "en-AE"),
    SAUDI_ARABIA("sa", "SAR", "Asia/Riyadh", "ar-SA"),
    AUSTRALIA("au", "AUD", "Australia/Sydney", "en-AU"),
    UNITED_STATES("us", "USD", "America/New_York", "en-US"),
    CANADA("ca", "CAD", "America/Toronto", "en-CA");

    private final String countryCode;
    private final String currencyCode;
    private final String timezone;
    private final String locale;

    ConvoyMarket(String countryCode, String currencyCode, String timezone, String locale) {
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
        this.timezone = timezone;
        this.locale = locale;
    }

    public String getCountryCode() { return countryCode; }
    public String getCurrencyCode() { return currencyCode; }
    public String getTimezone() { return timezone; }
    public String getLocale() { return locale; }
}
