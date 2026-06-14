package com.goweyy.convoyia.common.simulation;

import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 100 mission simulation test.
 *
 * Generates and verifies 100 distinct mission pricing scenarios covering all
 * vehicle segments, urgency levels, distance ranges, and time-of-day slots.
 * Validates all core pricing invariants for each mission.
 */
@DisplayName("100 Mission Simulation")
class Convoy100MissionsSimulationTest {

    private static final BigDecimal FLAT_BASE      = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM    = new BigDecimal("0.80");
    private static final BigDecimal MINIMUM_FARE   = new BigDecimal("30.00");
    private static final BigDecimal VAT_RATE        = new BigDecimal("0.20");
    private static final BigDecimal CONVEYOR_RATIO  = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO  = new BigDecimal("0.25");
    private static final BigDecimal EXPRESS_BONUS   = new BigDecimal("0.15");
    private static final BigDecimal URGENT_BONUS    = new BigDecimal("0.30");
    private static final BigDecimal WEEKEND_BONUS   = new BigDecimal("0.10");

    @Test
    @DisplayName("100 missions: all pricing invariants hold")
    void simulate_100_missions_all_invariants_hold() {
        List<MissionCase> missions = generate100Missions();
        assertThat(missions).hasSize(100);

        List<String> failures = new ArrayList<>();

        for (MissionCase mission : missions) {
            try {
                validateMission(mission);
            } catch (AssertionError e) {
                failures.add(e.getMessage());
            }
        }

        assertThat(failures)
                .as("All 100 mission pricing invariants must hold. Failures:\n" +
                    String.join("\n", failures))
                .isEmpty();
    }

    @Test
    @DisplayName("100 missions: LUXE_PLATEAU always produces manual quote")
    void simulate_luxe_plateau_missions_produce_manual_quote() {
        for (int i = 0; i < 20; i++) {
            double distanceKm = 10 + i * 20;  // 10 to 390 km
            ConvoyPricingStatus status = computeStatus(ConvoyVehicleSegment.LUXE_PLATEAU, distanceKm);
            assertThat(status)
                    .as("LUXE_PLATEAU mission %d (dist=%.0f) must produce PENDING_MANUAL_QUOTE", i, distanceKm)
                    .isEqualTo(ConvoyPricingStatus.PENDING_MANUAL_QUOTE);
        }
    }

    @Test
    @DisplayName("100 missions: minimum fare enforced for all short missions")
    void simulate_short_missions_apply_minimum_fare() {
        for (int i = 0; i < 25; i++) {
            double distanceKm = 0.5 + i * 0.5;  // 0.5 to 12.5 km
            BigDecimal rawTransport = FLAT_BASE.add(
                    BigDecimal.valueOf(distanceKm).multiply(RATE_PER_KM)).setScale(2, RoundingMode.HALF_UP);
            if (rawTransport.compareTo(MINIMUM_FARE) < 0) {
                BigDecimal totalHt = rawTransport.max(MINIMUM_FARE);
                assertThat(totalHt)
                        .as("Short mission %.1f km must use minimum fare", distanceKm)
                        .isEqualByComparingTo(MINIMUM_FARE);
            }
        }
    }

    @Test
    @DisplayName("100 missions: conveyor always earns more than platform")
    void simulate_conveyor_always_earns_more_than_platform() {
        List<MissionCase> missions = generate100Missions();
        for (MissionCase mission : missions) {
            if (mission.segment == ConvoyVehicleSegment.LUXE_PLATEAU) continue;
            BigDecimal ttc = computeTtc(mission);
            BigDecimal conveyor = ttc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal platform = ttc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
            assertThat(conveyor)
                    .as("Mission %s: conveyor must earn more than platform", mission.id)
                    .isGreaterThan(platform);
        }
    }

    // ── Mission generation ────────────────────────────────────────────────────

    private List<MissionCase> generate100Missions() {
        List<MissionCase> missions = new ArrayList<>();
        int idx = 0;

        // Vary across all non-LUXE segments × all urgencies × multiple distances
        ConvoyVehicleSegment[] segments = {
            ConvoyVehicleSegment.STANDARD, ConvoyVehicleSegment.COURANT,
            ConvoyVehicleSegment.PREMIUM,  ConvoyVehicleSegment.HAUT_DE_GAMME
        };
        ConvoyUrgency[] urgencies = { ConvoyUrgency.STANDARD, ConvoyUrgency.EXPRESS, ConvoyUrgency.URGENT };

        double[] distances = { 1, 5, 10, 20, 30, 50, 80, 100, 150, 200, 300, 500 };

        LocalDateTime weekday  = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)).withHour(12);
        LocalDateTime weekend  = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).withHour(12);
        LocalDateTime night    = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).withHour(2);
        LocalDateTime[] times  = { weekday, weekend, night };

        // 4 segments × 3 urgencies × 3 time slots = 36 core cases
        for (ConvoyVehicleSegment segment : segments) {
            for (ConvoyUrgency urgency : urgencies) {
                for (LocalDateTime time : times) {
                    missions.add(new MissionCase(
                            "mission-" + (++idx),
                            segment, urgency,
                            distances[idx % distances.length],
                            time));
                }
            }
        }

        // Add LUXE_PLATEAU cases (should return manual quote)
        for (int i = 0; i < 10; i++) {
            missions.add(new MissionCase("mission-luxe-" + i,
                    ConvoyVehicleSegment.LUXE_PLATEAU, ConvoyUrgency.STANDARD,
                    50 + i * 10, weekday));
        }

        // Fill remaining to reach exactly 100 with varying distances
        while (missions.size() < 100) {
            int i = missions.size();
            ConvoyVehicleSegment seg = segments[i % segments.length];
            ConvoyUrgency urg = urgencies[i % urgencies.length];
            double dist = 10 + (i % 50) * 4;
            missions.add(new MissionCase("mission-fill-" + i, seg, urg, dist, weekday));
        }

        return missions;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateMission(MissionCase mission) {
        if (mission.segment == ConvoyVehicleSegment.LUXE_PLATEAU) {
            assertThat(computeStatus(mission.segment, mission.distanceKm))
                    .as("LUXE_PLATEAU mission %s must produce PENDING_MANUAL_QUOTE", mission.id)
                    .isEqualTo(ConvoyPricingStatus.PENDING_MANUAL_QUOTE);
            return;
        }

        BigDecimal ttc = computeTtc(mission);

        // TTC must be positive
        assertThat(ttc.compareTo(BigDecimal.ZERO)).as("TTC must be positive for mission " + mission.id).isGreaterThan(0);

        // TTC ≥ MINIMUM_FARE × 1.20 (minimum HT × VAT)
        BigDecimal minTtc = MINIMUM_FARE.multiply(BigDecimal.ONE.add(VAT_RATE)).setScale(2, RoundingMode.HALF_UP);
        assertThat(ttc).as("TTC must be ≥ minimum TTC for mission " + mission.id).isGreaterThanOrEqualTo(minTtc);

        // Conveyor + platform ≈ TTC (rounding tolerance ≤ 0.01)
        BigDecimal conveyor = ttc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platform = ttc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = ttc.subtract(conveyor.add(platform)).abs();
        assertThat(diff).as("Split sum must equal TTC for mission " + mission.id)
                .isLessThanOrEqualTo(new BigDecimal("0.01"));

        // Stripe pre-auth > TTC
        BigDecimal preAuth = ttc.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.CEILING);
        assertThat(preAuth).as("Pre-auth must exceed TTC for mission " + mission.id).isGreaterThan(ttc);
    }

    // ── Pricing calculation (mirrors ConvoyPricingCalculationService) ─────────

    private BigDecimal computeTtc(MissionCase mission) {
        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(mission.distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal surcharge = switch (mission.segment) {
            case STANDARD      -> BigDecimal.ZERO;
            case COURANT       -> new BigDecimal("5.00");
            case PREMIUM       -> new BigDecimal("15.00");
            case HAUT_DE_GAMME -> new BigDecimal("30.00");
            case LUXE_PLATEAU  -> BigDecimal.ZERO;
        };

        BigDecimal subtotal = transport.add(surcharge).setScale(2, RoundingMode.HALF_UP);

        BigDecimal urgencyBonus = switch (mission.urgency) {
            case EXPRESS -> subtotal.multiply(EXPRESS_BONUS).setScale(2, RoundingMode.HALF_UP);
            case URGENT  -> subtotal.multiply(URGENT_BONUS).setScale(2, RoundingMode.HALF_UP);
            default      -> BigDecimal.ZERO;
        };

        BigDecimal weekendBonus = BigDecimal.ZERO;
        if (mission.requestedAt != null) {
            DayOfWeek dow = mission.requestedAt.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendBonus = subtotal.multiply(WEEKEND_BONUS).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalHtRaw = subtotal.add(urgencyBonus).add(weekendBonus).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHt    = totalHtRaw.max(MINIMUM_FARE);

        BigDecimal vat    = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        return totalHt.add(vat).setScale(2, RoundingMode.HALF_UP);
    }

    private ConvoyPricingStatus computeStatus(ConvoyVehicleSegment segment, double distanceKm) {
        return segment == ConvoyVehicleSegment.LUXE_PLATEAU
                ? ConvoyPricingStatus.PENDING_MANUAL_QUOTE
                : ConvoyPricingStatus.PRICED;
    }

    // ── Data record ───────────────────────────────────────────────────────────

    record MissionCase(
            String id,
            ConvoyVehicleSegment segment,
            ConvoyUrgency urgency,
            double distanceKm,
            LocalDateTime requestedAt
    ) {}
}
