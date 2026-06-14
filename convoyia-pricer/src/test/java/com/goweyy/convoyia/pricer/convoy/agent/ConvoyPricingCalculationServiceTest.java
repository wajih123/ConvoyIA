package com.goweyy.convoyia.pricer.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingBreakdown;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingRequest;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyPricingCalculationServiceTest {

    private static final BigDecimal MINIMUM_FARE = new BigDecimal("30.00");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");
    private static final BigDecimal CONVEYOR_RATIO = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO = new BigDecimal("0.25");

    private ConvoyPricingCalculationService service;

    @BeforeEach
    void setUp() {
        service = new ConvoyPricingCalculationService();
    }

    // ── LUXE_PLATEAU → manual quote ──────────────────────────────────────────

    @Test
    void luxe_plateau_returns_pending_manual_quote() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.LUXE_PLATEAU, ConvoyUrgency.STANDARD, 100, null);
        ConvoyPricingResult result = service.calculate(req);
        assertThat(result.getStatus()).isEqualTo(ConvoyPricingStatus.PENDING_MANUAL_QUOTE);
        assertThat(result.getPricingBreakdown()).isNull();
    }

    // ── All non-LUXE segments produce PRICED status ──────────────────────────

    @ParameterizedTest
    @EnumSource(value = ConvoyVehicleSegment.class, names = {"STANDARD", "COURANT", "PREMIUM", "HAUT_DE_GAMME"})
    void non_luxe_segments_produce_priced_status(ConvoyVehicleSegment segment) {
        ConvoyPricingRequest req = buildRequest(segment, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        ConvoyPricingResult result = service.calculate(req);
        assertThat(result.getStatus()).isEqualTo(ConvoyPricingStatus.PRICED);
        assertThat(result.getPricingBreakdown()).isNotNull();
    }

    // ── Minimum fare enforcement ─────────────────────────────────────────────

    @Test
    void very_short_distance_applies_minimum_fare() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 1, weekdayNoon());
        ConvoyPricingResult result = service.calculate(req);
        ConvoyPricingBreakdown b = result.getPricingBreakdown();
        assertThat(b.isMinimumFareApplied()).isTrue();
        assertThat(b.getTotalHt()).isEqualByComparingTo(MINIMUM_FARE);
    }

    @Test
    void long_distance_does_not_apply_minimum_fare() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 200, weekdayNoon());
        ConvoyPricingResult result = service.calculate(req);
        assertThat(result.getPricingBreakdown().isMinimumFareApplied()).isFalse();
    }

    // ── VAT calculation ──────────────────────────────────────────────────────

    @Test
    void vat_is_20_percent_of_total_ht() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        ConvoyPricingBreakdown b = service.calculate(req).getPricingBreakdown();
        BigDecimal expectedVat = b.getTotalHt().multiply(VAT_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(b.getVatAmount()).isEqualByComparingTo(expectedVat);
    }

    @Test
    void total_ttc_equals_ht_plus_vat() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.PREMIUM, ConvoyUrgency.STANDARD, 80, weekdayNoon());
        ConvoyPricingBreakdown b = service.calculate(req).getPricingBreakdown();
        assertThat(b.getTotalTtc()).isEqualByComparingTo(b.getTotalHt().add(b.getVatAmount()));
    }

    // ── 75/25 split ──────────────────────────────────────────────────────────

    @Test
    void conveyor_gets_75_percent_of_total_ttc() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 100, weekdayNoon());
        ConvoyPricingBreakdown b = service.calculate(req).getPricingBreakdown();
        BigDecimal expectedConveyor = b.getTotalTtc().multiply(CONVEYOR_RATIO).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(b.getConveyorPayout()).isEqualByComparingTo(expectedConveyor);
    }

    @Test
    void platform_gets_25_percent_of_total_ttc() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 100, weekdayNoon());
        ConvoyPricingBreakdown b = service.calculate(req).getPricingBreakdown();
        BigDecimal expectedPlatform = b.getTotalTtc().multiply(PLATFORM_RATIO).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(b.getPlatformFeeAmount()).isEqualByComparingTo(expectedPlatform);
    }

    // ── Urgency bonuses ──────────────────────────────────────────────────────

    @Test
    void express_urgency_adds_15_percent_bonus() {
        LocalDateTime dt = weekdayNoon();
        ConvoyPricingRequest standard = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, dt);
        ConvoyPricingRequest express  = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.EXPRESS,  50, dt);
        BigDecimal htStandard = service.calculate(standard).getPricingBreakdown().getTotalHt();
        BigDecimal htExpress  = service.calculate(express).getPricingBreakdown().getTotalHt();
        assertThat(htExpress).isGreaterThanOrEqualTo(htStandard);
    }

    @Test
    void urgent_urgency_adds_30_percent_bonus() {
        LocalDateTime dt = weekdayNoon();
        ConvoyPricingRequest standard = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 100, dt);
        ConvoyPricingRequest urgent   = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.URGENT,   100, dt);
        BigDecimal htStandard = service.calculate(standard).getPricingBreakdown().getTotalHt();
        BigDecimal htUrgent   = service.calculate(urgent).getPricingBreakdown().getTotalHt();
        assertThat(htUrgent).isGreaterThan(htStandard);
    }

    @Test
    void urgent_is_more_expensive_than_express() {
        LocalDateTime dt = weekdayNoon();
        ConvoyPricingRequest express = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.EXPRESS, 100, dt);
        ConvoyPricingRequest urgent  = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.URGENT,  100, dt);
        BigDecimal htExpress = service.calculate(express).getPricingBreakdown().getTotalHt();
        BigDecimal htUrgent  = service.calculate(urgent).getPricingBreakdown().getTotalHt();
        assertThat(htUrgent).isGreaterThan(htExpress);
    }

    // ── Weekend bonus ────────────────────────────────────────────────────────

    @Test
    void weekend_adds_10_percent_bonus() {
        LocalDateTime saturday = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                .withHour(12).withMinute(0);
        LocalDateTime monday   = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                .withHour(12).withMinute(0);
        ConvoyPricingRequest weekdayReq  = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 100, monday);
        ConvoyPricingRequest weekendReq  = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 100, saturday);
        BigDecimal htWeekday = service.calculate(weekdayReq).getPricingBreakdown().getTotalHt();
        BigDecimal htWeekend = service.calculate(weekendReq).getPricingBreakdown().getTotalHt();
        assertThat(htWeekend).isGreaterThan(htWeekday);
    }

    // ── Segment surcharges ───────────────────────────────────────────────────

    @Test
    void standard_has_zero_segment_surcharge() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        assertThat(service.calculate(req).getPricingBreakdown().getSegmentSurcharge())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void premium_has_higher_surcharge_than_courant() {
        ConvoyPricingRequest courantReq = buildRequest(ConvoyVehicleSegment.COURANT,  ConvoyUrgency.STANDARD, 50, weekdayNoon());
        ConvoyPricingRequest premiumReq = buildRequest(ConvoyVehicleSegment.PREMIUM,  ConvoyUrgency.STANDARD, 50, weekdayNoon());
        BigDecimal courantSurcharge = service.calculate(courantReq).getPricingBreakdown().getSegmentSurcharge();
        BigDecimal premiumSurcharge = service.calculate(premiumReq).getPricingBreakdown().getSegmentSurcharge();
        assertThat(premiumSurcharge).isGreaterThan(courantSurcharge);
    }

    @Test
    void haut_de_gamme_has_highest_non_luxe_surcharge() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.HAUT_DE_GAMME, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        BigDecimal surcharge = service.calculate(req).getPricingBreakdown().getSegmentSurcharge();
        assertThat(surcharge).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    // ── Currency / metadata ──────────────────────────────────────────────────

    @Test
    void currency_code_is_eur() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        assertThat(service.calculate(req).getPricingBreakdown().getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void tax_name_is_tva() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        assertThat(service.calculate(req).getPricingBreakdown().getTaxName()).isEqualTo("TVA");
    }

    // ── Null requestedAt ─────────────────────────────────────────────────────

    @Test
    void null_requested_at_does_not_throw() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, null);
        assertThat(service.calculate(req).getStatus()).isEqualTo(ConvoyPricingStatus.PRICED);
    }

    // ── Mission and tenant IDs are propagated ────────────────────────────────

    @Test
    void mission_and_tenant_ids_propagated() {
        ConvoyPricingRequest req = buildRequest(ConvoyVehicleSegment.STANDARD, ConvoyUrgency.STANDARD, 50, weekdayNoon());
        ConvoyPricingResult result = service.calculate(req);
        assertThat(result.getMissionId()).isEqualTo("mission-test");
        assertThat(result.getTenantId()).isEqualTo("tenant-goweyy");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ConvoyPricingRequest buildRequest(ConvoyVehicleSegment segment, ConvoyUrgency urgency,
                                              double distanceKm, LocalDateTime at) {
        return ConvoyPricingRequest.builder()
                .missionId("mission-test")
                .tenantId("tenant-goweyy")
                .vehicleSegment(segment)
                .urgency(urgency)
                .vehicleDeclaredValue(30_000)
                .estimatedDistanceKm(distanceKm)
                .requestedAt(at)
                .build();
    }

    private LocalDateTime weekdayNoon() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
                .withHour(12).withMinute(0).withSecond(0);
    }
}
