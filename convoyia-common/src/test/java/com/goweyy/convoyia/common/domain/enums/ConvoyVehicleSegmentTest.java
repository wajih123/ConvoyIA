package com.goweyy.convoyia.common.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyVehicleSegmentTest {

    // ── fromValue — boundary mapping ──────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "0,        STANDARD",
        "5000,     STANDARD",
        "19999,    STANDARD",
        "20000,    COURANT",
        "30000,    COURANT",
        "39999,    COURANT",
        "40000,    PREMIUM",
        "55000,    PREMIUM",
        "69999,    PREMIUM",
        "70000,    HAUT_DE_GAMME",
        "100000,   HAUT_DE_GAMME",
        "119999,   HAUT_DE_GAMME",
        "120000,   LUXE_PLATEAU",
        "200000,   LUXE_PLATEAU",
        "500000,   LUXE_PLATEAU"
    })
    void from_value_maps_correctly(double vehicleValue, ConvoyVehicleSegment expected) {
        assertThat(ConvoyVehicleSegment.fromValue(vehicleValue)).isEqualTo(expected);
    }

    // ── getMinValue / getMaxValue ────────────────────────────────────────────

    @Test
    void standard_min_is_0_max_is_20000() {
        assertThat(ConvoyVehicleSegment.STANDARD.getMinValue()).isEqualTo(0);
        assertThat(ConvoyVehicleSegment.STANDARD.getMaxValue()).isEqualTo(20_000);
    }

    @Test
    void courant_min_is_20000_max_is_40000() {
        assertThat(ConvoyVehicleSegment.COURANT.getMinValue()).isEqualTo(20_000);
        assertThat(ConvoyVehicleSegment.COURANT.getMaxValue()).isEqualTo(40_000);
    }

    @Test
    void premium_min_is_40000_max_is_70000() {
        assertThat(ConvoyVehicleSegment.PREMIUM.getMinValue()).isEqualTo(40_000);
        assertThat(ConvoyVehicleSegment.PREMIUM.getMaxValue()).isEqualTo(70_000);
    }

    @Test
    void haut_de_gamme_min_is_70000_max_is_120000() {
        assertThat(ConvoyVehicleSegment.HAUT_DE_GAMME.getMinValue()).isEqualTo(70_000);
        assertThat(ConvoyVehicleSegment.HAUT_DE_GAMME.getMaxValue()).isEqualTo(120_000);
    }

    @Test
    void luxe_plateau_min_is_120000_max_is_max_int() {
        assertThat(ConvoyVehicleSegment.LUXE_PLATEAU.getMinValue()).isEqualTo(120_000);
        assertThat(ConvoyVehicleSegment.LUXE_PLATEAU.getMaxValue()).isEqualTo(Integer.MAX_VALUE);
    }

    // ── All 5 segments exist ──────────────────────────────────────────────────

    @Test
    void five_segments_exist() {
        assertThat(ConvoyVehicleSegment.values()).hasSize(5);
    }
}
