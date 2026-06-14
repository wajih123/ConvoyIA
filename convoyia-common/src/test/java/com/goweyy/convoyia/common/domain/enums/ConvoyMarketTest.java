package com.goweyy.convoyia.common.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyMarketTest {

    @Test
    void france_currency_is_eur() {
        assertThat(ConvoyMarket.FRANCE.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void france_tax_name_is_tva() {
        assertThat(ConvoyMarket.FRANCE.getTaxName()).isEqualTo("TVA");
    }

    @Test
    void france_vat_rate_is_20_percent() {
        assertThat(ConvoyMarket.FRANCE.getDefaultTaxRate()).isEqualByComparingTo(new BigDecimal("0.20"));
    }

    @Test
    void uk_currency_is_gbp() {
        assertThat(ConvoyMarket.UNITED_KINGDOM.getCurrencyCode()).isEqualTo("GBP");
    }

    @Test
    void germany_currency_is_eur() {
        assertThat(ConvoyMarket.GERMANY.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void germany_tax_name_is_mwst() {
        assertThat(ConvoyMarket.GERMANY.getTaxName()).isEqualTo("MwSt");
    }

    @Test
    void uae_currency_is_aed() {
        assertThat(ConvoyMarket.UAE.getCurrencyCode()).isEqualTo("AED");
    }

    @Test
    void uae_vat_is_5_percent() {
        assertThat(ConvoyMarket.UAE.getDefaultTaxRate()).isEqualByComparingTo(new BigDecimal("0.05"));
    }

    @Test
    void us_has_zero_tax_rate() {
        assertThat(ConvoyMarket.UNITED_STATES.getDefaultTaxRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void all_markets_have_non_null_fields() {
        for (ConvoyMarket market : ConvoyMarket.values()) {
            assertThat(market.getCurrencyCode()).as("currencyCode for %s", market).isNotNull();
            assertThat(market.getCountryCode()).as("countryCode for %s", market).isNotNull();
            assertThat(market.getTimezone()).as("timezone for %s", market).isNotNull();
            assertThat(market.getLocale()).as("locale for %s", market).isNotNull();
            assertThat(market.getTaxName()).as("taxName for %s", market).isNotNull();
            assertThat(market.getDefaultTaxRate()).as("taxRate for %s", market).isNotNull();
        }
    }

    @Test
    void twelve_markets_exist() {
        assertThat(ConvoyMarket.values()).hasSize(12);
    }
}
